package com.nestcart.modules.visibility_compute.service;

import com.nestcart.modules.visibility_compute.dto.LineOfSightResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.BiFunction;

@Service
@Slf4j
public class LineOfSightService {

    public boolean isLineOfSightBresenham(int x0, int y0, double h0,
                                          int x1, int y1,
                                          double earthRadius, double gridResolution,
                                          BiFunction<Integer, Integer, Double> elevationGetter) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;

        while (true) {
            if (x == x1 && y == y1) break;

            double distRatio = dx + dy == 0 ? 0 :
                    (Math.abs(x - x0) + Math.abs(y - y0)) / (double) (dx + dy);

            double lineOfSightHeight = h0 - distRatio * distRatio * 0;
            double curvatureDrop = getEarthCurvatureDrop(distRatio * Math.max(dx, dy) * gridResolution, earthRadius);
            double terrainHeight = elevationGetter.apply(x, y) - curvatureDrop;

            if (terrainHeight > lineOfSightHeight && !(x == x0 && y == y0)) {
                return false;
            }

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }

        return true;
    }

    public LineOfSightResult analyzeLineOfSightBresenham(int x0, int y0, double h0,
                                                         int x1, int y1,
                                                         double earthRadius, double gridResolution,
                                                         BiFunction<Integer, Integer, Double> elevationGetter) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;
        double totalDist = Math.sqrt(dx * dx + dy * dy) * gridResolution;

        while (true) {
            if (x == x1 && y == y1) break;

            double distRatio = dx + dy == 0 ? 0 :
                    (Math.abs(x - x0) + Math.abs(y - y0)) / (double) (dx + dy);

            double lineOfSightHeight = h0 - distRatio * distRatio * 0;
            double distFromObs = distRatio * Math.max(dx, dy) * gridResolution;
            double curvatureDrop = getEarthCurvatureDrop(distFromObs, earthRadius);
            double terrainHeight = elevationGetter.apply(x, y) - curvatureDrop;

            if (terrainHeight > lineOfSightHeight && !(x == x0 && y == y0)) {
                return LineOfSightResult.builder()
                        .visible(false)
                        .blockerX(x)
                        .blockerY(y)
                        .blockerElevation(terrainHeight + curvatureDrop)
                        .distance(distFromObs)
                        .build();
            }

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }

        return LineOfSightResult.builder()
                .visible(true)
                .distance(totalDist)
                .build();
    }

    public boolean isLineOfSightQuadtree(QuadtreeNode root, int obsX, int obsY, double obsHeight,
                                         int targetX, int targetY,
                                         double earthRadius, double gridResolution) {
        if (root == null) return true;

        double dx = targetX - obsX;
        double dy = targetY - obsY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        double distMeters = dist * gridResolution;

        return isLOSClearRecursive(root, obsX, obsY, obsHeight,
                targetX, targetY, distMeters, gridResolution, earthRadius);
    }

    public LineOfSightResult analyzeLineOfSightQuadtree(QuadtreeNode root, int obsX, int obsY, double obsHeight,
                                                        int targetX, int targetY,
                                                        double earthRadius, double gridResolution) {
        boolean visible = isLineOfSightQuadtree(root, obsX, obsY, obsHeight,
                targetX, targetY, earthRadius, gridResolution);

        double dx = targetX - obsX;
        double dy = targetY - obsY;
        double dist = Math.sqrt(dx * dx + dy * dy) * gridResolution;

        return LineOfSightResult.builder()
                .visible(visible)
                .distance(dist)
                .build();
    }

    private boolean isLOSClearRecursive(QuadtreeNode node, int obsX, int obsY, double obsHeight,
                                        int targetX, int targetY, double totalDist,
                                        double resolution, double earthRadius) {
        if (node == null) return true;

        if (!lineIntersectsNode(node, obsX, obsY, targetX, targetY)) {
            return true;
        }

        double minLOSHeight = getMinLOSHeightToNode(node, obsX, obsY, obsHeight,
                targetX, targetY, totalDist, resolution, earthRadius);
        double maxLOSHeight = getMaxLOSHeightAtNode(node, obsX, obsY, obsHeight,
                targetX, targetY, totalDist, resolution);

        if (node.getMinElevation() >= maxLOSHeight) {
            return false;
        }

        if (node.getMaxElevation() <= minLOSHeight) {
            return true;
        }

        if (node.isLeaf()) {
            return checkNodeDetail(node, obsX, obsY, obsHeight,
                    targetX, targetY, totalDist, resolution, earthRadius);
        }

        for (QuadtreeNode child : node.getChildren()) {
            if (child != null) {
                if (!isLOSClearRecursive(child, obsX, obsY, obsHeight,
                        targetX, targetY, totalDist, resolution, earthRadius)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean lineIntersectsNode(QuadtreeNode node, int x0, int y0, int x1, int y1) {
        return Math.max(x0, x1) >= node.getMinX() && Math.min(x0, x1) <= node.getMaxX()
                && Math.max(y0, y1) >= node.getMinY() && Math.min(y0, y1) <= node.getMaxY();
    }

    private double getMinLOSHeightToNode(QuadtreeNode node, int obsX, int obsY, double obsHeight,
                                          int targetX, int targetY, double totalDist,
                                          double resolution, double earthRadius) {
        double[] distances = getDistancesToNodeCorners(node, obsX, obsY, targetX, targetY, resolution);

        double minHeight = obsHeight;
        for (double d : distances) {
            double ratio = d / totalDist;
            double curvatureDrop = d * d / (2 * earthRadius);
            double lineHeight = obsHeight * (1 - ratio) + 0 * ratio;
            minHeight = Math.min(minHeight, lineHeight - curvatureDrop);
        }

        return minHeight;
    }

    private double getMaxLOSHeightAtNode(QuadtreeNode node, int obsX, int obsY, double obsHeight,
                                          int targetX, int targetY, double totalDist,
                                          double resolution) {
        double[] distances = getDistancesToNodeCorners(node, obsX, obsY, targetX, targetY, resolution);

        double maxHeight = 0;
        for (double d : distances) {
            double ratio = d / totalDist;
            double lineHeight = obsHeight * (1 - ratio) + 0 * ratio;
            maxHeight = Math.max(maxHeight, lineHeight);
        }

        return maxHeight;
    }

    private double[] getDistancesToNodeCorners(QuadtreeNode node, int obsX, int obsY,
                                                int targetX, int targetY, double resolution) {
        double lineLength = Math.sqrt(Math.pow(targetX - obsX, 2) + Math.pow(targetY - obsY, 2));
        if (lineLength == 0) return new double[]{0};

        double t0 = projectPointToLine(node.getMinX(), node.getMinY(), obsX, obsY, targetX, targetY);
        double t1 = projectPointToLine(node.getMaxX(), node.getMinY(), obsX, obsY, targetX, targetY);
        double t2 = projectPointToLine(node.getMinX(), node.getMaxY(), obsX, obsY, targetX, targetY);
        double t3 = projectPointToLine(node.getMaxX(), node.getMaxY(), obsX, obsY, targetX, targetY);

        double tMin = Math.min(Math.min(t0, t1), Math.min(t2, t3));
        double tMax = Math.max(Math.max(t0, t1), Math.max(t2, t3));

        tMin = Math.max(0, Math.min(1, tMin));
        tMax = Math.max(0, Math.min(1, tMax));

        return new double[]{tMin * lineLength * resolution, tMax * lineLength * resolution};
    }

    private double projectPointToLine(int px, int py, int x0, int y0, int x1, int y1) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double lenSq = dx * dx + dy * dy;
        if (lenSq == 0) return 0;

        double t = ((px - x0) * dx + (py - y0) * dy) / lenSq;
        return Math.max(0, Math.min(1, t));
    }

    private boolean checkNodeDetail(QuadtreeNode node, int obsX, int obsY, double obsHeight,
                                    int targetX, int targetY, double totalDist,
                                    double resolution, double earthRadius) {
        int dx = Math.abs(targetX - obsX);
        int dy = Math.abs(targetY - obsY);
        int sx = obsX < targetX ? 1 : -1;
        int sy = obsY < targetY ? 1 : -1;
        int err = dx - dy;

        int x = obsX;
        int y = obsY;

        while (true) {
            if (x == targetX && y == targetY) break;

            if (x >= node.getMinX() && x <= node.getMaxX()
                    && y >= node.getMinY() && y <= node.getMaxY()) {
                double distFromObs = Math.sqrt(Math.pow(x - obsX, 2) + Math.pow(y - obsY, 2)) * resolution;
                double ratio = totalDist > 0 ? distFromObs / totalDist : 0;
                double curvatureDrop = distFromObs * distFromObs / (2 * earthRadius);

                double lineHeight = obsHeight * (1 - ratio);
                double terrainHeight = node.getMaxElevation() - curvatureDrop;

                if (terrainHeight > lineHeight) {
                    return false;
                }
            }

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }

        return true;
    }

    private double getEarthCurvatureDrop(double distance, double earthRadius) {
        return distance * distance / (2 * earthRadius);
    }

    @Data
    @AllArgsConstructor
    public static class QuadtreeNode {
        private int minX;
        private int minY;
        private int maxX;
        private int maxY;
        private double minElevation;
        private double maxElevation;
        private boolean isLeaf;
        private QuadtreeNode[] children;

        public QuadtreeNode(int minX, int minY, int maxX, int maxY,
                            double minElev, double maxElev, boolean isLeaf) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.minElevation = minElev;
            this.maxElevation = maxElev;
            this.isLeaf = isLeaf;
        }
    }
}

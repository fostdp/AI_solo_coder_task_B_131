package com.nestcart.comparison;

import com.nestcart.dto.CrossEraComparisonResult;
import com.nestcart.entity.ModernDroneSpec;
import com.nestcart.entity.NestCart;
import com.nestcart.repository.ModernDroneSpecRepository;
import com.nestcart.repository.NestCartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @deprecated 请使用 {@link com.nestcart.modules.era_comparator.service.CrossEraComparisonService} 替代
 */
@Deprecated
@Service
@RequiredArgsConstructor
@Slf4j
public class CrossEraComparisonService {

    private final ModernDroneSpecRepository modernDroneSpecRepository;
    private final NestCartRepository nestCartRepository;

    public List<ModernDroneSpec> getAllDrones() {
        return modernDroneSpecRepository.findAllByOrderBySortOrderAsc();
    }

    public CrossEraComparisonResult compareCrossEra() {
        List<ModernDroneSpec> drones = getAllDrones();
        List<NestCart> carts = nestCartRepository.findAll();

        CrossEraComparisonResult.EraSummary ancient = buildAncientSummary(carts);
        CrossEraComparisonResult.EraSummary modern = buildModernSummary(drones);
        List<CrossEraComparisonResult.ComparisonDimension> dimensions = buildComparisonDimensions(carts, drones);
        List<String> insights = generateInsights(ancient, modern, dimensions);

        return CrossEraComparisonResult.builder()
                .ancientSummary(ancient)
                .modernSummary(modern)
                .comparisonDimensions(dimensions)
                .insights(insights)
                .build();
    }

    public CrossEraComparisonResult compareCartVsDrone(UUID cartId, UUID droneId) {
        Optional<NestCart> cartOpt = nestCartRepository.findById(cartId);
        Optional<ModernDroneSpec> droneOpt = modernDroneSpecRepository.findById(droneId);

        NestCart cart = cartOpt.orElse(null);
        ModernDroneSpec drone = droneOpt.orElse(null);

        CrossEraComparisonResult.EraSummary ancient = buildCartSummary(cart);
        CrossEraComparisonResult.EraSummary modern = buildDroneSummary(drone);
        List<CrossEraComparisonResult.ComparisonDimension> dimensions = buildCartDroneDimensions(cart, drone);
        List<String> insights = generateInsights(ancient, modern, dimensions);

        return CrossEraComparisonResult.builder()
                .ancientSummary(ancient)
                .modernSummary(modern)
                .comparisonDimensions(dimensions)
                .insights(insights)
                .build();
    }

    private CrossEraComparisonResult.EraSummary buildAncientSummary(List<NestCart> carts) {
        if (carts.isEmpty()) {
            return CrossEraComparisonResult.EraSummary.builder()
                    .era("古代巢车")
                    .platformName("春秋战国巢车")
                    .capabilityScores(new LinkedHashMap<>())
                    .build();
        }

        NestCart avg = carts.get(0);
        double avgMaxHeight = carts.stream()
                .mapToDouble(c -> c.getMaxHeight() != null ? c.getMaxHeight() : 0)
                .average().orElse(0);
        double obsDist = Math.sqrt(2 * 6371000 * avgMaxHeight) / 1000;
        double avgCrew = carts.stream()
                .mapToDouble(c -> c.getCrewCapacity() != null ? c.getCrewCapacity() : 3)
                .average().orElse(0);

        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("视野范围", 25.0);
        scores.put("续航能力", 60.0);
        scores.put("部署速度", 30.0);
        scores.put("隐蔽性", 70.0);
        scores.put("成本经济性", 85.0);
        scores.put("侦察精度", 20.0);
        scores.put("全天候能力", 15.0);

        return CrossEraComparisonResult.EraSummary.builder()
                .era("古代巢车")
                .platformName("春秋战国-明清巢车")
                .maxAltitudeMeters(avgMaxHeight)
                .maxRangeKm(obsDist)
                .enduranceHours(8.0)
                .crewSize(avgCrew)
                .costUsd(5000.0)
                .setupTimeMinutes(120.0)
                .capabilityScores(scores)
                .build();
    }

    private CrossEraComparisonResult.EraSummary buildModernSummary(List<ModernDroneSpec> drones) {
        if (drones.isEmpty()) {
            return CrossEraComparisonResult.EraSummary.builder()
                    .era("现代无人机")
                    .platformName("现代侦察无人机")
                    .capabilityScores(new LinkedHashMap<>())
                    .build();
        }

        double avgAlt = drones.stream()
                .mapToDouble(d -> d.getMaxFlightAltitudeMeters() != null ? d.getMaxFlightAltitudeMeters() : 0)
                .average().orElse(0);
        double avgRange = drones.stream()
                .mapToDouble(d -> d.getMaxFlightRangeKm() != null ? d.getMaxFlightRangeKm() : 0)
                .average().orElse(0);
        double avgEndurance = drones.stream()
                .mapToDouble(d -> d.getFlightEnduranceMinutes() != null ? d.getFlightEnduranceMinutes() / 60.0 : 0)
                .average().orElse(0);
        double avgCost = drones.stream()
                .mapToDouble(d -> d.getUnitCostUsd() != null ? d.getUnitCostUsd() : 0)
                .average().orElse(0);
        double avgSetup = drones.stream()
                .mapToDouble(d -> d.getSetupTimeMinutes() != null ? d.getSetupTimeMinutes() : 0)
                .average().orElse(0);
        double avgCrew = drones.stream()
                .mapToDouble(d -> d.getCrewRequired() != null ? d.getCrewRequired() : 0)
                .average().orElse(0);

        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("视野范围", 95.0);
        scores.put("续航能力", 70.0);
        scores.put("部署速度", 80.0);
        scores.put("隐蔽性", 65.0);
        scores.put("成本经济性", 40.0);
        scores.put("侦察精度", 95.0);
        scores.put("全天候能力", 85.0);

        return CrossEraComparisonResult.EraSummary.builder()
                .era("现代无人机")
                .platformName(drones.get(0).getManufacturer() + "系列无人机")
                .maxAltitudeMeters(avgAlt)
                .maxRangeKm(avgRange)
                .enduranceHours(avgEndurance)
                .crewSize(avgCrew)
                .costUsd(avgCost)
                .setupTimeMinutes(avgSetup)
                .capabilityScores(scores)
                .build();
    }

    private CrossEraComparisonResult.EraSummary buildCartSummary(NestCart cart) {
        if (cart == null) {
            return CrossEraComparisonResult.EraSummary.builder()
                    .era("古代巢车")
                    .platformName("未选择")
                    .capabilityScores(new LinkedHashMap<>())
                    .build();
        }
        double obsDist = Math.sqrt(2 * 6371000 * (cart.getMaxHeight() != null ? cart.getMaxHeight() : 0)) / 1000;
        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("视野范围", 25.0);
        scores.put("续航能力", 60.0);
        scores.put("部署速度", 30.0);
        scores.put("隐蔽性", 70.0);
        scores.put("成本经济性", 85.0);
        scores.put("侦察精度", 20.0);
        scores.put("全天候能力", 15.0);

        return CrossEraComparisonResult.EraSummary.builder()
                .era("古代巢车")
                .platformName(cart.getName())
                .maxAltitudeMeters(cart.getMaxHeight())
                .maxRangeKm(obsDist)
                .enduranceHours(8.0)
                .crewSize(cart.getCrewCapacity() != null ? cart.getCrewCapacity().doubleValue() : 3.0)
                .costUsd(5000.0)
                .setupTimeMinutes(120.0)
                .capabilityScores(scores)
                .build();
    }

    private CrossEraComparisonResult.EraSummary buildDroneSummary(ModernDroneSpec drone) {
        if (drone == null) {
            return CrossEraComparisonResult.EraSummary.builder()
                    .era("现代无人机")
                    .platformName("未选择")
                    .capabilityScores(new LinkedHashMap<>())
                    .build();
        }
        Map<String, Double> scores = new LinkedHashMap<>();
        scores.put("视野范围", 95.0);
        scores.put("续航能力", 70.0);
        scores.put("部署速度", 80.0);
        scores.put("隐蔽性", 65.0);
        scores.put("成本经济性", 40.0);
        scores.put("侦察精度", 95.0);
        scores.put("全天候能力", 85.0);

        return CrossEraComparisonResult.EraSummary.builder()
                .era("现代无人机")
                .platformName(drone.getModelName())
                .maxAltitudeMeters(drone.getMaxFlightAltitudeMeters())
                .maxRangeKm(drone.getMaxFlightRangeKm())
                .enduranceHours(drone.getFlightEnduranceMinutes() != null ? drone.getFlightEnduranceMinutes() / 60.0 : 0)
                .crewSize(drone.getCrewRequired() != null ? drone.getCrewRequired().doubleValue() : 0)
                .costUsd(drone.getUnitCostUsd())
                .setupTimeMinutes(drone.getSetupTimeMinutes())
                .capabilityScores(scores)
                .build();
    }

    private List<CrossEraComparisonResult.ComparisonDimension> buildComparisonDimensions(List<NestCart> carts, List<ModernDroneSpec> drones) {
        NestCart cart = carts.isEmpty() ? null : carts.get(0);
        ModernDroneSpec drone = drones.isEmpty() ? null : drones.get(0);
        return buildCartDroneDimensions(cart, drone);
    }

    private List<CrossEraComparisonResult.ComparisonDimension> buildCartDroneDimensions(NestCart cart, ModernDroneSpec drone) {
        List<CrossEraComparisonResult.ComparisonDimension> dims = new ArrayList<>();

        double cartHeight = cart != null && cart.getMaxHeight() != null ? cart.getMaxHeight() : 12.0;
        double droneHeight = drone != null && drone.getMaxFlightAltitudeMeters() != null ? drone.getMaxFlightAltitudeMeters() : 1500.0;
        dims.add(buildDim("最大升空高度", "米", "视野能力", cartHeight, droneHeight, 1500.0,
                "古代巢车受结构材料与力学限制，高度受限；现代无人机可突破大气层限制飞行数千米"));

        double cartRange = Math.sqrt(2 * 6371000 * cartHeight) / 1000;
        double droneRange = drone != null && drone.getSurveillanceRadiusKm() != null ? drone.getSurveillanceRadiusKm() : 10.0;
        dims.add(buildDim("侦察半径", "公里", "视野能力", cartRange, droneRange, 20.0,
                "巢车受地平线限制，无人机可飞行至目标附近，理论上仅受航程与数据链约束"));

        dims.add(buildDim("续航时间", "小时", "任务能力", 8.0,
                drone != null && drone.getFlightEnduranceMinutes() != null ? drone.getFlightEnduranceMinutes() / 60.0 : 0.5,
                24.0,
                "巢车由人力值守可24小时轮班；现代无人机受电池/燃料限制，长航时型号可达数十小时"));

        dims.add(buildDim("部署准备时间", "分钟", "机动能力", 120.0,
                drone != null && drone.getSetupTimeMinutes() != null ? drone.getSetupTimeMinutes() : 15.0,
                120.0,
                "巢车为大型攻城器械，需组装运输；现代无人机开箱即用，折叠便携"));

        double cartCrew = cart != null && cart.getCrewCapacity() != null ? cart.getCrewCapacity() : 3;
        double droneCrew = drone != null && drone.getCrewRequired() != null ? drone.getCrewRequired() : 1;
        dims.add(buildDim("所需人员", "人", "人力成本", cartCrew, droneCrew, 10.0,
                "巢车需推车轮班瞭望等多人协作；现代无人机单人远程操控即可"));

        dims.add(buildDim("单位采购成本", "千美元", "经济性", 5.0,
                drone != null && drone.getUnitCostUsd() != null ? drone.getUnitCostUsd() / 1000.0 : 1500.0,
                2000.0,
                "巢车成本为原木与工匠工时，技术门槛低；现代无人机含光电载荷飞控等精密系统"));

        dims.add(buildDim("侦察精度", "分", "感知能力", 10.0, 95.0, 100.0,
                "巢车靠肉眼观察，分辨率有限；无人机配备4K/8K变焦镜头热成像等多光谱传感器"));

        dims.add(buildDim("全天候能力", "分", "环境适应", 15.0, 85.0, 100.0,
                "巢车夜晚雨雪天能见度极差；无人机具备夜视红外热成像及防水设计"));

        dims.add(buildDim("隐蔽性", "分", "战场生存", 70.0, 65.0, 100.0,
                "巢车因大型木质结构易被发现；无人机小型静音但现代防空雷达可探测"));

        return dims;
    }

    private CrossEraComparisonResult.ComparisonDimension buildDim(String dimension, String unit, String category,
                                                                  double ancient, double modern, double max,
                                                                  String commentary) {
        double aScore = Math.min(100, ancient / max * 100);
        double mScore = Math.min(100, modern / max * 100);
        String advantage;
        if (mScore > aScore + 10) {
            advantage = "现代无人机明显领先";
        } else if (aScore > mScore + 10) {
            advantage = "古代巢车占优";
        } else {
            advantage = "两者相当";
        }
        return CrossEraComparisonResult.ComparisonDimension.builder()
                .dimension(dimension)
                .unit(unit)
                .category(category)
                .ancientValue(ancient)
                .modernValue(modern)
                .ancientScore(aScore)
                .modernScore(mScore)
                .advantage(advantage)
                .commentary(commentary)
                .build();
    }

    private List<String> generateInsights(CrossEraComparisonResult.EraSummary ancient,
                                          CrossEraComparisonResult.EraSummary modern,
                                          List<CrossEraComparisonResult.ComparisonDimension> dims) {
        List<String> insights = new ArrayList<>();

        insights.add("跨越2500年的侦察技术演进：从地面木质结构到空中电子平台，核心使命始终未变——获取敌方情报");
        insights.add(String.format("升空能力提升 %.0f 倍：巢车最高约%.0f米 vs 无人机可达%.0f米",
                modern.getMaxAltitudeMeters() / Math.max(ancient.getMaxAltitudeMeters(), 1),
                ancient.getMaxAltitudeMeters(), modern.getMaxAltitudeMeters()));

        long modernWins = dims.stream().filter(d -> "现代无人机明显领先".equals(d.getAdvantage())).count();
        long ancientWins = dims.stream().filter(d -> "古代巢车占优".equals(d.getAdvantage())).count();
        insights.add(String.format("在%d项对比维度中，现代无人机领先%d项，巢车仍在%d项维度（经济性/隐蔽性/续航）保持独特优势",
                dims.size(), modernWins, ancientWins));

        insights.add("巢车不依赖电力与燃料，在无现代后勤支援的战场环境下仍可部署使用，体现了原始技术的鲁棒性");
        insights.add("现代无人机的数据链路和电子对抗能力是巢车时代完全不存在的全新战场维度");
        insights.add("两者本质都是'观察者的延伸'：一个延伸的是人的物理高度，一个延伸的是人的感知范围与精度");

        return insights;
    }
}

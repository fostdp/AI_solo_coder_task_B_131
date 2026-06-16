package com.nestcart.modules.evolution_analyzer;

import com.nestcart.modules.evolution_analyzer.dto.DynastyEvolutionResult;
import com.nestcart.modules.evolution_analyzer.entity.DynastyCart;
import com.nestcart.modules.evolution_analyzer.repository.DynastyCartRepository;
import com.nestcart.modules.evolution_analyzer.service.DynastyEvolutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("朝代演变分析测试 - evolution_analyzer 模块")
class DynastyEvolutionServiceTest {

    @Mock
    private DynastyCartRepository dynastyCartRepository;

    @InjectMocks
    private DynastyEvolutionService dynastyEvolutionService;

    private List<DynastyCart> testCarts;

    @BeforeEach
    void setUp() {
        testCarts = new ArrayList<>();
        testCarts.add(buildDynastyCart("西周", "公元前1046-前771年", "公元前1046年", 8.0, 5.0, 80.0, 2, 35.0, 1, "原木捆绑+绳索"));
        testCarts.add(buildDynastyCart("春秋", "公元前770-前476年", "公元前770年", 12.0, 8.0, 150.0, 3, 55.0, 2, "榫卯木构"));
        testCarts.add(buildDynastyCart("战国", "公元前475-前221年", "公元前475年", 15.0, 10.0, 200.0, 4, 72.0, 3, "硬木+铜箍加固"));
        testCarts.add(buildDynastyCart("宋代", "960-1279年", "公元960年", 22.0, 14.0, 350.0, 7, 94.0, 6, "复合木构+钢铁复合"));
    }

    private DynastyCart buildDynastyCart(String name, String period, String eraYear, double maxHeight,
                                          double boomLength, double basketWeight,
                                          int crewSize, double score, int sortOrder,
                                          String innovation) {
        return DynastyCart.builder()
                .id(UUID.randomUUID())
                .dynastyName(name)
                .period(period)
                .eraYear(eraYear)
                .maxHeight(maxHeight)
                .boomLength(boomLength)
                .basketWeight(basketWeight)
                .crewSize(crewSize)
                .evolutionScore(score)
                .sortOrder(sortOrder)
                .innovationFeatures(innovation)
                .build();
    }

    @Nested
    @DisplayName("正常场景：4个朝代的完整演化分析")
    class NormalEvolutionTests {

        @Test
        @DisplayName("演变分析应返回完整的演化点列表，包含4个朝代")
        void analyzeEvolution_shouldReturnFourEvolutionPoints() {
            when(dynastyCartRepository.findAllByOrderBySortOrderAsc()).thenReturn(testCarts);

            DynastyEvolutionResult result = dynastyEvolutionService.analyzeEvolution();

            assertNotNull(result);
            assertNotNull(result.getEvolutionPoints());
            assertEquals(4, result.getEvolutionPoints().size());
            assertEquals("西周", result.getEvolutionPoints().get(0).getDynastyName());
            assertEquals("宋代", result.getEvolutionPoints().get(3).getDynastyName());
        }

        @Test
        @DisplayName("观察距离应随高度递增，验证结构优化带来的视野改善")
        void analyzeEvolution_observationDistanceShouldIncreaseWithHeight() {
            when(dynastyCartRepository.findAllByOrderBySortOrderAsc()).thenReturn(testCarts);

            DynastyEvolutionResult result = dynastyEvolutionService.analyzeEvolution();
            List<DynastyEvolutionResult.EvolutionPoint> points = result.getEvolutionPoints();

            for (int i = 1; i < points.size(); i++) {
                assertTrue(points.get(i).getObservationDistance() >= points.get(i - 1).getObservationDistance(),
                        String.format("%s观察距离(%.2fm)应≥%s(%.2fm)",
                                points.get(i).getDynastyName(), points.get(i).getObservationDistance(),
                                points.get(i - 1).getDynastyName(), points.get(i - 1).getObservationDistance()));
            }
        }

        @Test
        @DisplayName("技术评分应随朝代递增，验证结构持续优化")
        void analyzeEvolution_evolutionScoreShouldIncrease() {
            when(dynastyCartRepository.findAllByOrderBySortOrderAsc()).thenReturn(testCarts);

            DynastyEvolutionResult result = dynastyEvolutionService.analyzeEvolution();
            List<DynastyEvolutionResult.EvolutionPoint> points = result.getEvolutionPoints();

            for (int i = 1; i < points.size(); i++) {
                assertTrue(points.get(i).getEvolutionScore() >= points.get(i - 1).getEvolutionScore(),
                        String.format("%s评分(%.1f)应≥%s(%.1f)",
                                points.get(i).getDynastyName(), points.get(i).getEvolutionScore(),
                                points.get(i - 1).getDynastyName(), points.get(i - 1).getEvolutionScore()));
            }
        }

        @Test
        @DisplayName("悬臂长度递增趋势应反映在趋势分析中")
        void analyzeEvolution_shouldContainBoomLengthGrowthTrend() {
            when(dynastyCartRepository.findAllByOrderBySortOrderAsc()).thenReturn(testCarts);

            DynastyEvolutionResult result = dynastyEvolutionService.analyzeEvolution();

            assertTrue(result.getEvolutionTrend().stream()
                    .anyMatch(t -> t.contains("悬臂长度增长")));
        }

        @Test
        @DisplayName("性能摘要应包含正确的平均值和朝代数量")
        void analyzeEvolution_performanceSummaryShouldBeCorrect() {
            when(dynastyCartRepository.findAllByOrderBySortOrderAsc()).thenReturn(testCarts);

            DynastyEvolutionResult result = dynastyEvolutionService.analyzeEvolution();
            Map<String, Double> summary = result.getPerformanceSummary();

            double expectedAvgHeight = testCarts.stream()
                    .mapToDouble(c -> c.getMaxHeight() != null ? c.getMaxHeight() : 0)
                    .average().orElse(0);
            assertEquals(expectedAvgHeight, summary.get("avgHeight"), 0.01);
            assertEquals(4.0, summary.get("dynastyCount"));
            assertNotNull(summary.get("avgBoomLength"));
            assertNotNull(summary.get("avgEvolutionScore"));
            assertNotNull(summary.get("avgCrewSize"));
        }

        @Test
        @DisplayName("对比表应包含5行指标，每行包含所有朝代数据")
        void analyzeEvolution_comparisonTableShouldBeComplete() {
            when(dynastyCartRepository.findAllByOrderBySortOrderAsc()).thenReturn(testCarts);

            DynastyEvolutionResult result = dynastyEvolutionService.analyzeEvolution();

            assertEquals(5, result.getComparisonTable().size());
            for (DynastyEvolutionResult.DynastyComparisonRow row : result.getComparisonTable()) {
                assertEquals(4, row.getValuesByDynasty().size());
            }
        }

        @Test
        @DisplayName("观察距离计算应使用地平线公式 d = sqrt(2Rh)")
        void analyzeEvolution_observationDistanceFormulaValidation() {
            when(dynastyCartRepository.findAllByOrderBySortOrderAsc()).thenReturn(testCarts);

            DynastyEvolutionResult result = dynastyEvolutionService.analyzeEvolution();

            double earthRadius = 6371000.0;
            double expectedDist = Math.sqrt(2 * earthRadius * 22.0);
            double actualDist = result.getEvolutionPoints().get(3).getObservationDistance();
            assertEquals(expectedDist, actualDist, 0.01);
        }
    }

    @Nested
    @DisplayName("边界场景测试")
    class BoundaryTests {

        @Test
        @DisplayName("仅1个朝代时趋势分析应返回空列表")
        void analyzeEvolution_singleCart_shouldReturnEmptyTrends() {
            List<DynastyCart> single = Collections.singletonList(testCarts.get(0));
            when(dynastyCartRepository.findAllByOrderBySortOrderAsc()).thenReturn(single);

            DynastyEvolutionResult result = dynastyEvolutionService.analyzeEvolution();

            assertTrue(result.getEvolutionTrend().isEmpty());
            assertEquals(1, result.getEvolutionPoints().size());
        }

        @Test
        @DisplayName("空数据时应返回空结果但不应抛异常")
        void analyzeEvolution_emptyData_shouldNotThrow() {
            when(dynastyCartRepository.findAllByOrderBySortOrderAsc()).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> dynastyEvolutionService.analyzeEvolution());

            DynastyEvolutionResult result = dynastyEvolutionService.analyzeEvolution();
            assertTrue(result.getEvolutionPoints().isEmpty());
            assertTrue(result.getEvolutionTrend().isEmpty());
            assertEquals(0.0, result.getPerformanceSummary().get("avgHeight"));
            assertEquals(0.0, result.getPerformanceSummary().get("dynastyCount"));
        }

        @Test
        @DisplayName("最大高度为0时观察距离应为0")
        void analyzeEvolution_zeroHeight_observationDistanceShouldBeZero() {
            DynastyCart zeroHeightCart = buildDynastyCart("测试", "测试期", "测试年", 0, 5.0, 80.0, 2, 10.0, 1, "无");
            when(dynastyCartRepository.findAllByOrderBySortOrderAsc())
                    .thenReturn(Arrays.asList(zeroHeightCart, testCarts.get(3)));

            DynastyEvolutionResult result = dynastyEvolutionService.analyzeEvolution();

            assertEquals(0.0, result.getEvolutionPoints().get(0).getObservationDistance());
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionTests {

        @Test
        @DisplayName("Repository抛出异常时应向上传播")
        void analyzeEvolution_repositoryThrows_shouldPropagate() {
            when(dynastyCartRepository.findAllByOrderBySortOrderAsc())
                    .thenThrow(new RuntimeException("数据库连接失败"));

            assertThrows(RuntimeException.class, () -> dynastyEvolutionService.analyzeEvolution());
        }

        @Test
        @DisplayName("maxHeight为null时应默认为0，观察距离为0")
        void analyzeEvolution_nullMaxHeight_shouldDefaultToZero() {
            DynastyCart nullCart = DynastyCart.builder()
                    .id(UUID.randomUUID())
                    .dynastyName("空测试")
                    .period("无")
                    .sortOrder(1)
                    .maxHeight(null)
                    .boomLength(null)
                    .basketWeight(null)
                    .crewSize(null)
                    .evolutionScore(null)
                    .build();
            DynastyCart normalCart = testCarts.get(3);
            when(dynastyCartRepository.findAllByOrderBySortOrderAsc())
                    .thenReturn(Arrays.asList(nullCart, normalCart));

            assertDoesNotThrow(() -> dynastyEvolutionService.analyzeEvolution());
            DynastyEvolutionResult result = dynastyEvolutionService.analyzeEvolution();
            assertEquals(0.0, result.getEvolutionPoints().get(0).getObservationDistance());
        }
    }

    @Nested
    @DisplayName("结构优化专项验证")
    class StructureOptimizationTests {

        @Test
        @DisplayName("西周到宋代，悬臂长度增长应大于等于100%")
        void structureOptimization_boomLengthGrowthShouldExceed100Percent() {
            when(dynastyCartRepository.findAllByOrderBySortOrderAsc()).thenReturn(testCarts);

            DynastyEvolutionResult result = dynastyEvolutionService.analyzeEvolution();

            DynastyEvolutionResult.EvolutionPoint first = result.getEvolutionPoints().get(0);
            DynastyEvolutionResult.EvolutionPoint last = result.getEvolutionPoints().get(3);
            double growth = (last.getBoomLength() - first.getBoomLength()) / first.getBoomLength() * 100;
            assertTrue(growth >= 100, String.format("悬臂长度增长 %.0f%% 应≥100%%", growth));
        }

        @Test
        @DisplayName("结构优化使承重能力提升应大于等于3倍")
        void structureOptimization_basketWeightShouldIncreaseAtLeast3x() {
            when(dynastyCartRepository.findAllByOrderBySortOrderAsc()).thenReturn(testCarts);

            DynastyEvolutionResult result = dynastyEvolutionService.analyzeEvolution();

            double firstWeight = result.getEvolutionPoints().get(0).getBasketWeight();
            double lastWeight = result.getEvolutionPoints().get(3).getBasketWeight();
            assertTrue(lastWeight / firstWeight >= 3,
                    String.format("承重比 %.1f 应≥3.0", lastWeight / firstWeight));
        }
    }

    @Test
    @DisplayName("getAllDynastyCarts 方法应返回 Repository 查询结果")
    void getAllDynastyCarts_shouldReturnRepositoryResult() {
        when(dynastyCartRepository.findAllByOrderBySortOrderAsc()).thenReturn(testCarts);

        List<DynastyCart> result = dynastyEvolutionService.getAllDynastyCarts();

        assertEquals(4, result.size());
        verify(dynastyCartRepository, times(1)).findAllByOrderBySortOrderAsc();
    }
}

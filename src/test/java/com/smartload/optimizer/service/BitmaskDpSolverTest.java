package com.smartload.optimizer.service;

import com.smartload.optimizer.model.request.OrderDto;
import com.smartload.optimizer.model.request.TruckDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BitmaskDpSolverTest {

    private BitmaskDpSolver solver;
    private TruckDto truck;

    @BeforeEach
    void setUp() {
        solver = new BitmaskDpSolver();
        truck = new TruckDto("truck-1", 44000L, 3000L);
    }

    private OrderDto order(String id, long payout, long weight, long volume,
                            String origin, String dest, boolean hazmat) {
        return new OrderDto(id, payout, weight, volume,
                origin, dest,
                LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 10),
                hazmat);
    }

    @Test
    void singleOrderFits_isSelected() {
        var orders = List.of(order("o1", 100000L, 10000L, 500L, "LA", "Dallas", false));
        var result = solver.solve(orders, truck);
        assertThat(result.bestMask()).isEqualTo(0b1);
        assertThat(result.bestPayout()).isEqualTo(100000L);
    }

    @Test
    void singleOrderExceedsWeight_emptySelection() {
        var orders = List.of(order("o1", 100000L, 50000L, 500L, "LA", "Dallas", false));
        var result = solver.solve(orders, truck);
        assertThat(result.bestMask()).isEqualTo(0);
        assertThat(result.bestPayout()).isEqualTo(0L);
    }

    @Test
    void singleOrderExceedsVolume_emptySelection() {
        var orders = List.of(order("o1", 100000L, 1000L, 5000L, "LA", "Dallas", false));
        var result = solver.solve(orders, truck);
        assertThat(result.bestMask()).isEqualTo(0);
        assertThat(result.bestPayout()).isEqualTo(0L);
    }

    @Test
    void twoOrdersBothFit_bothSelected() {
        var o1 = order("o1", 100000L, 10000L, 500L, "LA", "Dallas", false);
        var o2 = order("o2", 80000L,  8000L,  400L, "LA", "Dallas", false);
        var result = solver.solve(List.of(o1, o2), truck);
        assertThat(result.bestMask()).isEqualTo(0b11);
        assertThat(result.bestPayout()).isEqualTo(180000L);
    }

    @Test
    void twoOrders_onlyHigherPayoutFits() {
        // o1 alone fits, o2 alone fits, together exceed weight
        var o1 = order("o1", 200000L, 30000L, 1000L, "LA", "Dallas", false);
        var o2 = order("o2",  50000L, 20000L, 1000L, "LA", "Dallas", false);
        var result = solver.solve(List.of(o1, o2), truck);
        // Combined weight = 50000 > 44000, so pick o1 alone (higher payout)
        assertThat(result.bestMask()).isEqualTo(0b01);
        assertThat(result.bestPayout()).isEqualTo(200000L);
    }

    @Test
    void routeIncompatible_cannotCombine_bestSingleSelected() {
        var o1 = order("o1", 100000L, 5000L, 200L, "LA",      "Dallas", false);
        var o2 = order("o2", 200000L, 5000L, 200L, "Chicago", "Dallas", false);
        var result = solver.solve(List.of(o1, o2), truck);
        // Cannot combine (different origins), pick highest-payout single = o2
        assertThat(result.bestMask()).isEqualTo(0b10);
        assertThat(result.bestPayout()).isEqualTo(200000L);
    }

    @Test
    void hazmatsNotMixedWithNonHazmat() {
        var hazmatOrder    = order("o1", 300000L, 5000L, 200L, "LA", "Dallas", true);
        var nonHazmatOrder = order("o2", 100000L, 5000L, 200L, "LA", "Dallas", false);
        var result = solver.solve(List.of(hazmatOrder, nonHazmatOrder), truck);
        // Cannot combine; pick hazmat alone (higher payout)
        assertThat(result.bestMask()).isEqualTo(0b01);
        assertThat(result.bestPayout()).isEqualTo(300000L);
    }

    @Test
    void allHazmatOrders_allSelected() {
        var o1 = order("o1", 100000L, 10000L, 500L, "LA", "Dallas", true);
        var o2 = order("o2",  80000L,  8000L, 400L, "LA", "Dallas", true);
        var result = solver.solve(List.of(o1, o2), truck);
        assertThat(result.bestMask()).isEqualTo(0b11);
        assertThat(result.bestPayout()).isEqualTo(180000L);
    }

    @Test
    void nothingFits_emptySelection() {
        var o1 = order("o1", 100000L, 50000L, 500L,  "LA", "Dallas", false);
        var o2 = order("o2",  80000L,  8000L, 9000L, "LA", "Dallas", false);
        var result = solver.solve(List.of(o1, o2), truck);
        assertThat(result.bestMask()).isEqualTo(0);
        assertThat(result.bestPayout()).isEqualTo(0L);
        assertThat(result.bestWeight()).isEqualTo(0L);
        assertThat(result.bestVolume()).isEqualTo(0L);
    }

    @Test
    void picksHigherPayoutSubset() {
        // o1+o2: weight=30000, volume=2100, payout=430000 — fits
        // o3 alone: weight=40000, volume=2000, payout=420000 — fits
        // o1+o3 or o2+o3: exceed weight (58000 / 52000) — don't fit
        // Best valid subset is o1+o2 (430000 > 420000)
        var o1 = order("o1", 250000L, 18000L, 1200L, "LA", "Dallas", false);
        var o2 = order("o2", 180000L, 12000L,  900L, "LA", "Dallas", false);
        var o3 = order("o3", 420000L, 40000L, 2000L, "LA", "Dallas", false);
        var result = solver.solve(List.of(o1, o2, o3), truck);
        assertThat(result.bestPayout()).isEqualTo(430000L);
    }

    @Test
    void n22Orders_completesUnder5Seconds() {
        List<OrderDto> orders = new ArrayList<>();
        for (int i = 0; i < 22; i++) {
            orders.add(order("o" + i, 10000L + i, 1000L, 100L, "LA", "Dallas", false));
        }
        long start = System.currentTimeMillis();
        solver.solve(orders, truck);
        long elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed).isLessThan(5000L);
    }

    @Test
    void bestWeightAndVolumeMatchSelectedOrders() {
        var o1 = order("o1", 100000L, 10000L, 500L, "LA", "Dallas", false);
        var o2 = order("o2",  80000L,  8000L, 400L, "LA", "Dallas", false);
        var result = solver.solve(List.of(o1, o2), truck);
        assertThat(result.bestWeight()).isEqualTo(18000L);
        assertThat(result.bestVolume()).isEqualTo(900L);
    }
}

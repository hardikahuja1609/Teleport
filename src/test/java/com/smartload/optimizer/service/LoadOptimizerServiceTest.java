package com.smartload.optimizer.service;

import com.smartload.optimizer.model.request.OptimizeRequest;
import com.smartload.optimizer.model.request.OrderDto;
import com.smartload.optimizer.model.request.TruckDto;
import com.smartload.optimizer.model.response.OptimizeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoadOptimizerServiceTest {

    private LoadOptimizerService service;

    @BeforeEach
    void setUp() {
        service = new LoadOptimizerService(new BitmaskDpSolver());
    }

    private OrderDto order(String id, long payout, long weight, long volume) {
        return new OrderDto(id, payout, weight, volume,
                "Los Angeles, CA", "Dallas, TX",
                LocalDate.of(2025, 12, 5), LocalDate.of(2025, 12, 9),
                false);
    }

    @Test
    void responseContainsCorrectOrderIds() {
        var truck = new TruckDto("truck-1", 44000L, 3000L);
        var o1 = order("ord-001", 250000L, 18000L, 1200L);
        var o2 = order("ord-002", 180000L, 12000L,  900L);
        var request = new OptimizeRequest(truck, List.of(o1, o2));

        OptimizeResponse response = service.optimize(request);

        assertThat(response.truckId()).isEqualTo("truck-1");
        assertThat(response.selectedOrderIds()).containsExactlyInAnyOrder("ord-001", "ord-002");
        assertThat(response.totalPayoutCents()).isEqualTo(430000L);
        assertThat(response.totalWeightLbs()).isEqualTo(30000L);
        assertThat(response.totalVolumeCuft()).isEqualTo(2100L);
    }

    @Test
    void utilizationCalculatedCorrectly() {
        var truck = new TruckDto("truck-1", 44000L, 3000L);
        var o1 = order("ord-001", 250000L, 18000L, 1200L);
        var o2 = order("ord-002", 180000L, 12000L,  900L);
        var request = new OptimizeRequest(truck, List.of(o1, o2));

        OptimizeResponse response = service.optimize(request);

        // 30000/44000 = 68.1818... → rounded to 68.18
        assertThat(response.utilizationWeightPercent()).isEqualTo(68.18);
        // 2100/3000 = 70.0
        assertThat(response.utilizationVolumePercent()).isEqualTo(70.0);
    }

    @Test
    void emptySelectionWhenNothingFits() {
        var truck = new TruckDto("truck-1", 1000L, 100L);
        var o1 = order("ord-001", 250000L, 18000L, 1200L);
        var request = new OptimizeRequest(truck, List.of(o1));

        OptimizeResponse response = service.optimize(request);

        assertThat(response.selectedOrderIds()).isEmpty();
        assertThat(response.totalPayoutCents()).isEqualTo(0L);
        assertThat(response.totalWeightLbs()).isEqualTo(0L);
        assertThat(response.totalVolumeCuft()).isEqualTo(0L);
        assertThat(response.utilizationWeightPercent()).isEqualTo(0.0);
        assertThat(response.utilizationVolumePercent()).isEqualTo(0.0);
    }

    @Test
    void totalPayoutMatchesSumOfSelectedOrders() {
        var truck = new TruckDto("truck-1", 44000L, 3000L);
        var o1 = order("ord-001", 250000L, 18000L, 1200L);
        var o2 = order("ord-002", 180000L, 12000L,  900L);
        var request = new OptimizeRequest(truck, List.of(o1, o2));

        OptimizeResponse response = service.optimize(request);

        long expectedPayout = response.selectedOrderIds().stream()
                .mapToLong(id -> id.equals("ord-001") ? 250000L : 180000L)
                .sum();
        assertThat(response.totalPayoutCents()).isEqualTo(expectedPayout);
    }
}

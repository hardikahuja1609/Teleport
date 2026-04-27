package com.smartload.optimizer.service;

import com.smartload.optimizer.model.request.OptimizeRequest;
import com.smartload.optimizer.model.request.OrderDto;
import com.smartload.optimizer.model.response.OptimizeResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LoadOptimizerService {

    private final BitmaskDpSolver solver;

    public LoadOptimizerService(BitmaskDpSolver solver) {
        this.solver = solver;
    }

    public OptimizeResponse optimize(OptimizeRequest request) {
        List<OrderDto> orders = request.orders();
        BitmaskDpSolver.SolverResult result = solver.solve(orders, request.truck());

        List<String> selectedIds = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            if ((result.bestMask() & (1 << i)) != 0) {
                selectedIds.add(orders.get(i).id());
            }
        }

        long maxWeight = request.truck().maxWeightLbs();
        long maxVolume = request.truck().maxVolumeCuft();

        double weightPct = maxWeight == 0 ? 0.0
                : Math.round(result.bestWeight() * 10000.0 / maxWeight) / 100.0;
        double volumePct = maxVolume == 0 ? 0.0
                : Math.round(result.bestVolume() * 10000.0 / maxVolume) / 100.0;

        return new OptimizeResponse(
                request.truck().id(),
                selectedIds,
                result.bestPayout(),
                result.bestWeight(),
                result.bestVolume(),
                weightPct,
                volumePct
        );
    }
}

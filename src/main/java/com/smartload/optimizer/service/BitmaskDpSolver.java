package com.smartload.optimizer.service;

import com.smartload.optimizer.model.request.OrderDto;
import com.smartload.optimizer.model.request.TruckDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BitmaskDpSolver {

    public record SolverResult(int bestMask, long bestPayout, long bestWeight, long bestVolume) {}

    public SolverResult solve(List<OrderDto> orders, TruckDto truck) {
        int n = orders.size();
        int totalMasks = 1 << n;

        // Precompute arrays for cache-friendly access
        long[] payout = new long[n];
        long[] weight = new long[n];
        long[] volume = new long[n];
        String[] origin = new String[n];
        String[] dest = new String[n];
        boolean[] hazmat = new boolean[n];

        for (int i = 0; i < n; i++) {
            OrderDto o = orders.get(i);
            payout[i] = o.payoutCents();
            weight[i] = o.weightLbs();
            volume[i] = o.volumeCuft();
            origin[i] = o.origin().strip().toLowerCase();
            dest[i]   = o.destination().strip().toLowerCase();
            hazmat[i] = o.isHazmat();
        }

        int bestMask = 0;
        long bestPayout = 0L;

        for (int mask = 1; mask < totalMasks; mask++) {
            long totalWeight = 0L;
            long totalVolume = 0L;
            long totalPayout = 0L;
            String refOrigin = null;
            String refDest = null;
            boolean hasHazmat = false;
            boolean hasNonHazmat = false;
            boolean routeOk = true;

            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) == 0) continue;

                totalWeight += weight[i];
                totalVolume += volume[i];
                totalPayout += payout[i];

                if (hazmat[i]) hasHazmat = true;
                else hasNonHazmat = true;

                if (refOrigin == null) {
                    refOrigin = origin[i];
                    refDest   = dest[i];
                } else if (!origin[i].equals(refOrigin) || !dest[i].equals(refDest)) {
                    routeOk = false;
                }
            }

            if (totalWeight > truck.maxWeightLbs()) continue;
            if (totalVolume > truck.maxVolumeCuft()) continue;
            if (!routeOk) continue;
            if (hasHazmat && hasNonHazmat) continue;

            if (totalPayout > bestPayout) {
                bestPayout = totalPayout;
                bestMask   = mask;
            }
        }

        // Reconstruct totals for best mask
        long bestWeight = 0L;
        long bestVolume = 0L;
        for (int i = 0; i < n; i++) {
            if ((bestMask & (1 << i)) != 0) {
                bestWeight += weight[i];
                bestVolume += volume[i];
            }
        }

        return new SolverResult(bestMask, bestPayout, bestWeight, bestVolume);
    }
}

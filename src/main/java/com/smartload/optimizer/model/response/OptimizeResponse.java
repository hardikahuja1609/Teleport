package com.smartload.optimizer.model.response;

import java.util.List;

public record OptimizeResponse(
        String truckId,
        List<String> selectedOrderIds,
        long totalPayoutCents,
        long totalWeightLbs,
        long totalVolumeCuft,
        double utilizationWeightPercent,
        double utilizationVolumePercent
) {}

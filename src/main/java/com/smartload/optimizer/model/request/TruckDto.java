package com.smartload.optimizer.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TruckDto(
        @NotBlank(message = "Truck id must not be blank") String id,
        @Positive(message = "max_weight_lbs must be positive") long maxWeightLbs,
        @Positive(message = "max_volume_cuft must be positive") long maxVolumeCuft
) {}

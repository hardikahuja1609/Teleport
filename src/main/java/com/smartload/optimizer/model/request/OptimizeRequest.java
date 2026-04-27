package com.smartload.optimizer.model.request;

import com.smartload.optimizer.validation.ValidOrderSet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@ValidOrderSet
public record OptimizeRequest(
        @NotNull(message = "truck must not be null") @Valid TruckDto truck,
        @NotNull(message = "orders must not be null") @Size(min = 1, message = "orders must contain at least one order") List<@Valid OrderDto> orders
) {}

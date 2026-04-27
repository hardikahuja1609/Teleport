package com.smartload.optimizer.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record OrderDto(
        @NotBlank(message = "Order id must not be blank") String id,
        @Positive(message = "payout_cents must be positive") long payoutCents,
        @Positive(message = "weight_lbs must be positive") long weightLbs,
        @Positive(message = "volume_cuft must be positive") long volumeCuft,
        @NotBlank(message = "origin must not be blank") String origin,
        @NotBlank(message = "destination must not be blank") String destination,
        @NotNull(message = "pickup_date must not be null") LocalDate pickupDate,
        @NotNull(message = "delivery_date must not be null") LocalDate deliveryDate,
        boolean isHazmat
) {}

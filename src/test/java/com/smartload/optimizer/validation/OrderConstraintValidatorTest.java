package com.smartload.optimizer.validation;

import com.smartload.optimizer.model.request.OptimizeRequest;
import com.smartload.optimizer.model.request.OrderDto;
import com.smartload.optimizer.model.request.TruckDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OrderConstraintValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private TruckDto validTruck() {
        return new TruckDto("truck-1", 44000L, 3000L);
    }

    private OrderDto order(LocalDate pickup, LocalDate delivery) {
        return new OrderDto("o1", 100000L, 1000L, 100L,
                "A", "B", pickup, delivery, false);
    }

    @Test
    void validOrder_pickupBeforeDelivery_noViolation() {
        var request = new OptimizeRequest(validTruck(), List.of(
                order(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 5))));
        Set<ConstraintViolation<OptimizeRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void samePickupAndDeliveryDate_noViolation() {
        var request = new OptimizeRequest(validTruck(), List.of(
                order(LocalDate.of(2025, 12, 5), LocalDate.of(2025, 12, 5))));
        Set<ConstraintViolation<OptimizeRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void pickupAfterDelivery_hasViolation() {
        var request = new OptimizeRequest(validTruck(), List.of(
                order(LocalDate.of(2025, 12, 10), LocalDate.of(2025, 12, 1))));
        Set<ConstraintViolation<OptimizeRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).contains("pickup_date");
    }
}

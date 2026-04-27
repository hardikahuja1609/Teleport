package com.smartload.optimizer.validation;

import com.smartload.optimizer.model.request.OptimizeRequest;
import com.smartload.optimizer.model.request.OrderDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OrderConstraintValidator implements ConstraintValidator<ValidOrderSet, OptimizeRequest> {

    @Override
    public boolean isValid(OptimizeRequest request, ConstraintValidatorContext context) {
        if (request == null || request.orders() == null) {
            return true; // null checks handled by @NotNull
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        for (int i = 0; i < request.orders().size(); i++) {
            OrderDto order = request.orders().get(i);
            if (order == null || order.pickupDate() == null || order.deliveryDate() == null) {
                continue; // null date checks handled by @NotNull on the field
            }
            if (order.pickupDate().isAfter(order.deliveryDate())) {
                context.buildConstraintViolationWithTemplate(
                        "orders[" + i + "]: pickup_date '" + order.pickupDate()
                        + "' must not be after delivery_date '" + order.deliveryDate() + "'"
                ).addConstraintViolation();
                valid = false;
            }
        }

        return valid;
    }
}

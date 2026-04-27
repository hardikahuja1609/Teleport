package com.smartload.optimizer.controller;

import com.smartload.optimizer.exception.PayloadTooLargeException;
import com.smartload.optimizer.model.request.OptimizeRequest;
import com.smartload.optimizer.model.response.OptimizeResponse;
import com.smartload.optimizer.service.LoadOptimizerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/load-optimizer")
public class LoadOptimizerController {

    private static final int MAX_ORDERS = 22;

    private final LoadOptimizerService service;

    public LoadOptimizerController(LoadOptimizerService service) {
        this.service = service;
    }

    @PostMapping("/optimize")
    public ResponseEntity<OptimizeResponse> optimize(@RequestBody @Valid OptimizeRequest request) {
        if (request.orders().size() > MAX_ORDERS) {
            throw new PayloadTooLargeException(
                    "Maximum " + MAX_ORDERS + " orders allowed; received " + request.orders().size());
        }
        return ResponseEntity.ok(service.optimize(request));
    }
}

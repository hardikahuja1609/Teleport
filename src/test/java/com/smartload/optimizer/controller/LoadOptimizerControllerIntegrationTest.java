package com.smartload.optimizer.controller;

import com.smartload.optimizer.model.response.OptimizeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoadOptimizerControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String OPTIMIZE_URL = "/api/v1/load-optimizer/optimize";

    private HttpEntity<String> jsonBody(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(json, headers);
    }

    @Test
    void happyPath_returnsOptimalSelection() {
        String body = """
                {
                  "truck": {"id": "truck-123", "max_weight_lbs": 44000, "max_volume_cuft": 3000},
                  "orders": [
                    {"id": "ord-001", "payout_cents": 250000, "weight_lbs": 18000, "volume_cuft": 1200,
                     "origin": "Los Angeles, CA", "destination": "Dallas, TX",
                     "pickup_date": "2025-12-05", "delivery_date": "2025-12-09", "is_hazmat": false},
                    {"id": "ord-002", "payout_cents": 180000, "weight_lbs": 12000, "volume_cuft": 900,
                     "origin": "Los Angeles, CA", "destination": "Dallas, TX",
                     "pickup_date": "2025-12-04", "delivery_date": "2025-12-10", "is_hazmat": false}
                  ]
                }""";

        ResponseEntity<OptimizeResponse> response =
                restTemplate.postForEntity(OPTIMIZE_URL, jsonBody(body), OptimizeResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        OptimizeResponse result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.truckId()).isEqualTo("truck-123");
        assertThat(result.selectedOrderIds()).containsExactlyInAnyOrder("ord-001", "ord-002");
        assertThat(result.totalPayoutCents()).isEqualTo(430000L);
        assertThat(result.totalWeightLbs()).isEqualTo(30000L);
        assertThat(result.totalVolumeCuft()).isEqualTo(2100L);
        assertThat(result.utilizationWeightPercent()).isEqualTo(68.18);
        assertThat(result.utilizationVolumePercent()).isEqualTo(70.0);
    }

    @Test
    void missingTruck_returns400() {
        String body = """
                {"orders": [
                  {"id": "o1", "payout_cents": 100, "weight_lbs": 100, "volume_cuft": 10,
                   "origin": "A", "destination": "B",
                   "pickup_date": "2025-12-01", "delivery_date": "2025-12-05", "is_hazmat": false}
                ]}""";

        ResponseEntity<String> response =
                restTemplate.postForEntity(OPTIMIZE_URL, jsonBody(body), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void missingOrders_returns400() {
        String body = """
                {"truck": {"id": "t1", "max_weight_lbs": 44000, "max_volume_cuft": 3000}}""";

        ResponseEntity<String> response =
                restTemplate.postForEntity(OPTIMIZE_URL, jsonBody(body), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void emptyOrderList_returns400() {
        String body = """
                {"truck": {"id": "t1", "max_weight_lbs": 44000, "max_volume_cuft": 3000},
                 "orders": []}""";

        ResponseEntity<String> response =
                restTemplate.postForEntity(OPTIMIZE_URL, jsonBody(body), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void blankTruckId_returns400() {
        String body = """
                {"truck": {"id": "", "max_weight_lbs": 44000, "max_volume_cuft": 3000},
                 "orders": [
                   {"id": "o1", "payout_cents": 100, "weight_lbs": 100, "volume_cuft": 10,
                    "origin": "A", "destination": "B",
                    "pickup_date": "2025-12-01", "delivery_date": "2025-12-05", "is_hazmat": false}
                 ]}""";

        ResponseEntity<String> response =
                restTemplate.postForEntity(OPTIMIZE_URL, jsonBody(body), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void negativeTruckWeight_returns400() {
        String body = """
                {"truck": {"id": "t1", "max_weight_lbs": -1, "max_volume_cuft": 3000},
                 "orders": [
                   {"id": "o1", "payout_cents": 100, "weight_lbs": 100, "volume_cuft": 10,
                    "origin": "A", "destination": "B",
                    "pickup_date": "2025-12-01", "delivery_date": "2025-12-05", "is_hazmat": false}
                 ]}""";

        ResponseEntity<String> response =
                restTemplate.postForEntity(OPTIMIZE_URL, jsonBody(body), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void invalidDateFormat_returns400() {
        String body = """
                {"truck": {"id": "t1", "max_weight_lbs": 44000, "max_volume_cuft": 3000},
                 "orders": [
                   {"id": "o1", "payout_cents": 100, "weight_lbs": 100, "volume_cuft": 10,
                    "origin": "A", "destination": "B",
                    "pickup_date": "not-a-date", "delivery_date": "2025-12-05", "is_hazmat": false}
                 ]}""";

        ResponseEntity<String> response =
                restTemplate.postForEntity(OPTIMIZE_URL, jsonBody(body), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void pickupAfterDelivery_returns400() {
        String body = """
                {"truck": {"id": "t1", "max_weight_lbs": 44000, "max_volume_cuft": 3000},
                 "orders": [
                   {"id": "o1", "payout_cents": 100, "weight_lbs": 100, "volume_cuft": 10,
                    "origin": "A", "destination": "B",
                    "pickup_date": "2025-12-10", "delivery_date": "2025-12-01", "is_hazmat": false}
                 ]}""";

        ResponseEntity<String> response =
                restTemplate.postForEntity(OPTIMIZE_URL, jsonBody(body), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void twentyThreeOrders_returns413() {
        String ordersJson = IntStream.rangeClosed(1, 23)
                .mapToObj(i -> """
                        {"id": "o%d", "payout_cents": 10000, "weight_lbs": 100, "volume_cuft": 10,
                         "origin": "A", "destination": "B",
                         "pickup_date": "2025-12-01", "delivery_date": "2025-12-05", "is_hazmat": false}
                        """.formatted(i))
                .collect(Collectors.joining(","));

        String body = """
                {"truck": {"id": "t1", "max_weight_lbs": 1000000, "max_volume_cuft": 1000000},
                 "orders": [%s]}""".formatted(ordersJson);

        ResponseEntity<String> response =
                restTemplate.postForEntity(OPTIMIZE_URL, jsonBody(body), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void twentyTwoOrders_returns200() {
        String ordersJson = IntStream.rangeClosed(1, 22)
                .mapToObj(i -> """
                        {"id": "o%d", "payout_cents": 10000, "weight_lbs": 100, "volume_cuft": 10,
                         "origin": "A", "destination": "B",
                         "pickup_date": "2025-12-01", "delivery_date": "2025-12-05", "is_hazmat": false}
                        """.formatted(i))
                .collect(Collectors.joining(","));

        String body = """
                {"truck": {"id": "t1", "max_weight_lbs": 1000000, "max_volume_cuft": 1000000},
                 "orders": [%s]}""".formatted(ordersJson);

        ResponseEntity<OptimizeResponse> response =
                restTemplate.postForEntity(OPTIMIZE_URL, jsonBody(body), OptimizeResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().selectedOrderIds()).hasSize(22);
    }

    @Test
    void allOrdersExceedCapacity_returns200EmptyList() {
        String body = """
                {"truck": {"id": "t1", "max_weight_lbs": 100, "max_volume_cuft": 10},
                 "orders": [
                   {"id": "o1", "payout_cents": 100000, "weight_lbs": 50000, "volume_cuft": 5000,
                    "origin": "A", "destination": "B",
                    "pickup_date": "2025-12-01", "delivery_date": "2025-12-05", "is_hazmat": false}
                 ]}""";

        ResponseEntity<OptimizeResponse> response =
                restTemplate.postForEntity(OPTIMIZE_URL, jsonBody(body), OptimizeResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().selectedOrderIds()).isEmpty();
        assertThat(response.getBody().totalPayoutCents()).isEqualTo(0L);
    }

    @Test
    void hazmatNotMixedWithNonHazmat() {
        String body = """
                {"truck": {"id": "t1", "max_weight_lbs": 44000, "max_volume_cuft": 3000},
                 "orders": [
                   {"id": "hazmat-1", "payout_cents": 300000, "weight_lbs": 5000, "volume_cuft": 200,
                    "origin": "A", "destination": "B",
                    "pickup_date": "2025-12-01", "delivery_date": "2025-12-05", "is_hazmat": true},
                   {"id": "normal-1", "payout_cents": 100000, "weight_lbs": 5000, "volume_cuft": 200,
                    "origin": "A", "destination": "B",
                    "pickup_date": "2025-12-01", "delivery_date": "2025-12-05", "is_hazmat": false}
                 ]}""";

        ResponseEntity<OptimizeResponse> response =
                restTemplate.postForEntity(OPTIMIZE_URL, jsonBody(body), OptimizeResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        OptimizeResponse result = response.getBody();
        assertThat(result).isNotNull();
        // Must not contain both hazmat and non-hazmat
        boolean hasHazmat = result.selectedOrderIds().contains("hazmat-1");
        boolean hasNormal = result.selectedOrderIds().contains("normal-1");
        assertThat(hasHazmat && hasNormal).isFalse();
        // Picks hazmat (higher payout)
        assertThat(result.selectedOrderIds()).containsExactly("hazmat-1");
    }

    @Test
    void malformedJson_returns400() {
        ResponseEntity<String> response =
                restTemplate.postForEntity(OPTIMIZE_URL, jsonBody("{broken json"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void healthEndpoint_returns200() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/healthz", HttpMethod.GET, null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }
}

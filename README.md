# SmartLoad Optimization API

A stateless REST service that selects the revenue-maximizing combination of truck orders using bitmask dynamic programming, respecting weight, volume, hazmat, and route constraints.

## How to run

```bash
git clone <your-repo>
cd smartload-optimization-api
docker compose up --build
# Service available at http://localhost:8080
```

## Health check

```bash
curl http://localhost:8080/healthz
# {"status":"UP"}
```

## API Reference

### POST /api/v1/load-optimizer/optimize

**Request:**
```json
{
  "truck": {
    "id": "truck-123",
    "max_weight_lbs": 44000,
    "max_volume_cuft": 3000
  },
  "orders": [
    {
      "id": "ord-001",
      "payout_cents": 250000,
      "weight_lbs": 18000,
      "volume_cuft": 1200,
      "origin": "Los Angeles, CA",
      "destination": "Dallas, TX",
      "pickup_date": "2025-12-05",
      "delivery_date": "2025-12-09",
      "is_hazmat": false
    },
    {
      "id": "ord-002",
      "payout_cents": 180000,
      "weight_lbs": 12000,
      "volume_cuft": 900,
      "origin": "Los Angeles, CA",
      "destination": "Dallas, TX",
      "pickup_date": "2025-12-04",
      "delivery_date": "2025-12-10",
      "is_hazmat": false
    }
  ]
}
```

**Response:**
```json
{
  "truck_id": "truck-123",
  "selected_order_ids": ["ord-001", "ord-002"],
  "total_payout_cents": 430000,
  "total_weight_lbs": 30000,
  "total_volume_cuft": 2100,
  "utilization_weight_percent": 68.18,
  "utilization_volume_percent": 70.0
}
```

**Example curl:**
```bash
curl -X POST http://localhost:8080/api/v1/load-optimizer/optimize \
  -H "Content-Type: application/json" \
  -d @sample-request.json
```

## HTTP Status Codes

| Status | Meaning |
|--------|---------|
| 200 | Success — optimal selection returned (may be empty if no orders fit) |
| 400 | Bad Request — invalid or missing fields, bad date format, pickup > delivery |
| 413 | Payload Too Large — more than 22 orders submitted |
| 500 | Internal Server Error |

## Constraints enforced

1. **Weight** — sum of selected orders ≤ truck `max_weight_lbs`
2. **Volume** — sum of selected orders ≤ truck `max_volume_cuft`
3. **Route** — all selected orders must share the same `origin` → `destination`
4. **Hazmat isolation** — hazmat orders cannot be mixed with non-hazmat orders
5. **Time windows** — per-order: `pickup_date` must not be after `delivery_date`

## Algorithm

Uses **bitmask DP** over all 2ⁿ subsets (n ≤ 22, so ≤ 4,194,303 states). Each subset is evaluated in O(n) time to check all constraints. Worst-case complexity: O(2ⁿ × n). Performance target: < 800 ms for n=22.

Money is handled exclusively as 64-bit integer cents — no floating-point arithmetic for monetary or capacity values.

## Running tests

```bash
./mvnw test
```

## Architecture

```
controller/
  LoadOptimizerController   POST /api/v1/load-optimizer/optimize
  HealthController           GET /healthz

service/
  LoadOptimizerService      orchestrates request → solver → response
  BitmaskDpSolver           pure bitmask DP algorithm (no Spring deps)

model/
  request/  TruckDto, OrderDto, OptimizeRequest
  response/ OptimizeResponse

validation/
  ValidOrderSet             constraint annotation
  OrderConstraintValidator  per-order pickup ≤ delivery check

exception/
  GlobalExceptionHandler    maps exceptions to HTTP status codes
  PayloadTooLargeException  thrown when orders.size() > 22
```

## Design decisions

- **Stateless** — no database, no shared mutable state; each request is self-contained
- **Long cents only** — all monetary values are `long`; utilization percentages are the only `double` values, derived from integer math
- **413 vs 400** — the 22-order limit is checked in the controller (not Bean Validation) to return HTTP 413 rather than 400
- **Route/hazmat as algorithm constraints** — a mixed-route or mixed-hazmat input is not malformed; the solver finds the best compatible subset
- **Layered Docker image** — multi-stage build with layered jar extraction for fast CI rebuilds

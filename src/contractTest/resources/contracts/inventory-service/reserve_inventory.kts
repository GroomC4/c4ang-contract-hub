import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "재고 예약 API - 정상 케이스 (Kotlin DSL)"

    request {
        method = POST
        url = url("/api/v1/inventory/reservations")

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "orderId": "ORD-12345",
                "productId": "PROD-001",
                "quantity": 2
            }
        """.trimIndent())
    }

    response {
        status = CREATED

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "reservationId": "${value(client(regex("[A-Z]+-[0-9]+")), server("RSV-12345"))}",
                "orderId": "${fromRequest().body("$.orderId")}",
                "productId": "${fromRequest().body("$.productId")}",
                "quantity": ${fromRequest().body("$.quantity")},
                "warehouseId": "WH-001",
                "status": "RESERVED",
                "expiresAt": "${value(client(regex(isoDateTime())), server("2025-01-10T10:05:00Z"))}"
            }
        """.trimIndent())
    }
}

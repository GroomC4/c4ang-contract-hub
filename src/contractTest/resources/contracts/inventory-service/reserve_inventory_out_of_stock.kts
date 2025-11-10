import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "재고 예약 API - 재고 부족 (Kotlin DSL)"

    request {
        method = POST
        url = url("/api/v1/inventory/reservations")

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "orderId": "ORD-12346",
                "productId": "PROD-999",
                "quantity": 100
            }
        """.trimIndent())
    }

    response {
        status = CONFLICT

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "error": "OUT_OF_STOCK",
                "message": "재고가 부족합니다",
                "productId": "${fromRequest().body("$.productId")}",
                "requestedQuantity": ${fromRequest().body("$.quantity")},
                "availableQuantity": 0
            }
        """.trimIndent())
    }
}

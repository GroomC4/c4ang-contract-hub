import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "주문 생성 API - 정상 케이스 (Kotlin DSL)"

    request {
        method = POST
        url = url("/api/v1/orders")

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "productId": "PROD-001",
                "quantity": 2,
                "customerId": "CUST-12345"
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
                "orderId": "${value(client(regex("[A-Z]+-[0-9]+")), server("ORD-12345"))}",
                "productId": "${fromRequest().body("$.productId")}",
                "quantity": ${fromRequest().body("$.quantity")},
                "customerId": "${fromRequest().body("$.customerId")}",
                "status": "CREATED",
                "createdAt": "${value(client(regex(isoDateTime())), server("2025-01-10T10:00:00Z"))}"
            }
        """.trimIndent())
    }
}

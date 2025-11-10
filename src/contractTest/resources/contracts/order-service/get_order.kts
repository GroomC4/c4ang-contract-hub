import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "주문 조회 API - 정상 케이스 (Kotlin DSL)"

    request {
        method = GET
        url = url("/api/v1/orders/ORD-12345")
    }

    response {
        status = OK

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "orderId": "ORD-12345",
                "productId": "PROD-001",
                "quantity": 2,
                "customerId": "CUST-12345",
                "status": "COMPLETED",
                "createdAt": "${value(regex(isoDateTime()))}",
                "updatedAt": "${value(regex(isoDateTime()))}"
            }
        """.trimIndent())
    }
}

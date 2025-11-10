import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "결제 처리 API - 정상 케이스 (Kotlin DSL)"

    request {
        method = POST
        url = url("/api/v1/payments")

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "orderId": "ORD-12345",
                "amount": 50000,
                "paymentMethod": "CREDIT_CARD"
            }
        """.trimIndent())
    }

    response {
        status = OK

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "paymentId": "${value(client(regex("[A-Z]+-[0-9]+")), server("PAY-12345"))}",
                "orderId": "${fromRequest().body("$.orderId")}",
                "amount": ${fromRequest().body("$.amount")},
                "paymentMethod": "${fromRequest().body("$.paymentMethod")}",
                "status": "COMPLETED",
                "processedAt": "${value(client(regex(isoDateTime())), server("2025-01-10T10:00:00Z"))}"
            }
        """.trimIndent())
    }
}

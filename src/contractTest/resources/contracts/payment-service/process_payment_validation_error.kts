import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "결제 처리 API - 유효성 검증 실패 (Kotlin DSL)"

    request {
        method = POST
        url = url("/api/v1/payments")

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "orderId": "",
                "amount": -1000,
                "paymentMethod": ""
            }
        """.trimIndent())
    }

    response {
        status = BAD_REQUEST

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "error": "VALIDATION_ERROR",
                "message": "입력 값이 유효하지 않습니다",
                "details": [
                    {
                        "field": "orderId",
                        "message": "주문 ID는 필수입니다"
                    },
                    {
                        "field": "amount",
                        "message": "금액은 0보다 커야 합니다"
                    },
                    {
                        "field": "paymentMethod",
                        "message": "결제 수단은 필수입니다"
                    }
                ]
            }
        """.trimIndent())
    }
}

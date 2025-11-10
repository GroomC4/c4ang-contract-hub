import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

contract {
    description = "주문 조회 API - 존재하지 않는 주문 (Kotlin DSL)"

    request {
        method = GET
        url = url("/api/v1/orders/INVALID-ID")
    }

    response {
        status = NOT_FOUND

        headers {
            contentType = "application/json"
        }

        body = body("""
            {
                "error": "NOT_FOUND",
                "message": "주문을 찾을 수 없습니다",
                "orderId": "INVALID-ID"
            }
        """.trimIndent())
    }
}

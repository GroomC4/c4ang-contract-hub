import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

/**
 * Kafka Messaging Contract for SAGA StockConfirmationFailed Event
 *
 * 시나리오: Payment Saga 실패 (재고 확정 실패)
 * - Order Service가 재고 확정 실패 시 SAGA 실패 이벤트 발행
 * - Payment Service가 이벤트를 소비하여 보상 트랜잭션 실행 (결제 취소)
 *
 * Avro 스키마: src/main/avro/saga/StockConfirmationFailed.avsc
 * Topic: saga.stock-confirmation.failed
 * Producer: Order Service
 * Consumer: Payment Service
 * Saga: Payment Saga 보상 트랜잭션
 */
contract {
    description = "SAGA 재고 확정 실패 이벤트 - Payment Service가 보상 트랜잭션 실행"

    input {
        triggeredBy("failStockConfirmation()")
    }

    outputMessage {
        sentTo("saga.stock-confirmation.failed")

        body("""
            {
                "eventId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("130e8400-e29b-41d4-a716-446655440000"))}",
                "eventTimestamp": ${value(client(anyNumber()), server(1705060905000))},
                "orderId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("660e8400-e29b-41d4-a716-446655440000"))}",
                "paymentId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("ee0e8400-e29b-41d4-a716-446655440000"))}",
                "failureReason": "${value(client(anyNonBlankString()), server("재고 부족"))}",
                "failedAt": ${value(client(anyNumber()), server(1705060905000))}
            }
        """.trimIndent())

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")  // orderId
        }
    }
}

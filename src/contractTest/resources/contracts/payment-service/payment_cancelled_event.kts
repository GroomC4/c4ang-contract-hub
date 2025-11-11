import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

/**
 * Kafka Messaging Contract for PaymentCancelled Event
 *
 * 시나리오: Payment-Order Saga (재고 부족 실패 보상)
 * - Payment Service가 재고 확정 실패 시 결제 취소 및 이벤트 발행
 * - Order Service가 이벤트를 소비하여 주문 상태 업데이트
 *
 * Avro 스키마: src/main/avro/payment/PaymentCancelled.avsc
 * Topic: payment.cancelled
 * Producer: Payment Service
 * Consumer: Order Service
 * Saga: Payment Saga 보상 트랜잭션
 */
contract {
    description = "결제 취소 이벤트 - Order Service가 주문 상태 업데이트"

    input {
        triggeredBy("cancelPayment()")
    }

    outputMessage {
        sentTo("payment.cancelled")

        body("""
            {
                "eventId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("110e8400-e29b-41d4-a716-446655440000"))}",
                "eventTimestamp": ${value(client(anyNumber()), server(1705060950000))},
                "paymentId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("ee0e8400-e29b-41d4-a716-446655440000"))}",
                "orderId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("660e8400-e29b-41d4-a716-446655440000"))}",
                "userId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("770e8400-e29b-41d4-a716-446655440000"))}",
                "cancellationReason": "${value(client(regex("STOCK_UNAVAILABLE|ADMIN_CANCEL|USER_CANCEL|SYSTEM_ERROR")), server("STOCK_UNAVAILABLE"))}",
                "cancelledAt": ${value(client(anyNumber()), server(1705060950000))}
            }
        """.trimIndent())

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")  // orderId
        }
    }
}

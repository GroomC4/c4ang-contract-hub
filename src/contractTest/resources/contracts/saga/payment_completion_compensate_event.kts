import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

/**
 * Kafka Messaging Contract for SAGA PaymentCompletionCompensate Event
 *
 * 시나리오: 결제 완료 보상 트랜잭션 (재고 확정 실패 시)
 * - Payment Service가 재고 확정 실패 감지 시 보상 이벤트 발행
 * - Order Service가 이벤트를 소비하여 주문 취소 처리
 *
 * Avro 스키마: src/main/avro/saga/PaymentCompletionCompensate.avsc
 * Topic: saga.payment-completion.compensate
 * Producer: Payment Service
 * Consumer: Order Service
 * Saga: 보상 트랜잭션
 */
contract {
    description = "SAGA 결제 완료 보상 이벤트 - Order Service가 주문 취소"

    input {
        triggeredBy("compensatePaymentCompletion()")
    }

    outputMessage {
        sentTo("saga.payment-completion.compensate")

        body("""
            {
                "eventId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("150e8400-e29b-41d4-a716-446655440000"))}",
                "eventTimestamp": ${value(client(anyNumber()), server(1705060955000))},
                "paymentId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("ee0e8400-e29b-41d4-a716-446655440000"))}",
                "orderId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("660e8400-e29b-41d4-a716-446655440000"))}",
                "refundAmount": ${value(client(anyDouble()), server(20000.00))},
                "compensationReason": "${value(client(anyNonBlankString()), server("재고 확정 실패"))}",
                "compensatedAt": ${value(client(anyNumber()), server(1705060955000))}
            }
        """.trimIndent())

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")  // orderId
        }
    }
}

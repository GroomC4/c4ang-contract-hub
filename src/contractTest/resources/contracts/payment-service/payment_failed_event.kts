import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

/**
 * Kafka Messaging Contract for PaymentFailed Event
 *
 * 시나리오: Payment Saga (결제 실패)
 * - Payment Service가 PG사 결제 승인 거부 시 PaymentFailed 이벤트 발행
 * - Order Service가 이벤트를 소비하여 보상 트랜잭션 실행 (주문 취소 + 재고 복원)
 *
 * Avro 스키마: src/main/avro/payment/PaymentFailed.avsc
 * Topic: payment.failed
 * Producer: Payment Service
 * Consumer: Order Service
 * Saga: Payment Saga 실패 보상
 */
contract {
    description = "결제 실패 이벤트 - Order Service가 보상 트랜잭션 실행"

    input {
        triggeredBy("failPayment()")
    }

    outputMessage {
        sentTo("payment.failed")

        body("""
            {
                "eventId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("ff0e8400-e29b-41d4-a716-446655440000"))}",
                "eventTimestamp": ${value(client(anyNumber()), server(1705060920000))},
                "paymentId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("ee0e8400-e29b-41d4-a716-446655440000"))}",
                "orderId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("660e8400-e29b-41d4-a716-446655440000"))}",
                "userId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("770e8400-e29b-41d4-a716-446655440000"))}",
                "failureReason": "${value(client(anyNonBlankString()), server("잔액 부족"))}",
                "failedAt": ${value(client(anyNumber()), server(1705060920000))}
            }
        """.trimIndent())

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")  // orderId
        }
    }
}

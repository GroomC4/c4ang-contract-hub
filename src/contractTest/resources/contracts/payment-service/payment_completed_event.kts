import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

/**
 * Kafka Messaging Contract for PaymentCompleted Event
 *
 * 시나리오: Payment-Order Saga (결제 완료)
 * - Payment Service가 결제 완료 후 PaymentCompleted 이벤트 발행
 * - Order Service가 이벤트를 소비하여 재고 확정 처리
 *
 * Avro 스키마: src/main/avro/payment/PaymentCompleted.avsc
 * Topic: payment.completed
 * Producer: Payment Service
 * Consumer: Order Service, Notification Service
 * Saga: Payment Saga 시작
 */
contract {
    description = "결제 완료 이벤트 - Order Service가 재고 확정 처리"

    input {
        triggeredBy("completePayment()")
    }

    outputMessage {
        sentTo("payment.completed")

        body("""
            {
                "eventId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("dd0e8400-e29b-41d4-a716-446655440000"))}",
                "eventTimestamp": ${value(client(anyNumber()), server(1705060900000))},
                "paymentId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("ee0e8400-e29b-41d4-a716-446655440000"))}",
                "orderId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("660e8400-e29b-41d4-a716-446655440000"))}",
                "userId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("770e8400-e29b-41d4-a716-446655440000"))}",
                "totalAmount": ${value(client(anyDouble()), server(20000.00))},
                "paymentMethod": "${value(client(regex("CARD|BANK_TRANSFER|KAKAO_PAY|NAVER_PAY|TOSS")), server("CARD"))}",
                "pgApprovalNumber": "${value(client(regex("[A-Z0-9-]+")), server("APPROVAL-12345"))}",
                "completedAt": ${value(client(anyNumber()), server(1705060900000))}
            }
        """.trimIndent())

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")  // orderId (순서 보장)
        }
    }
}

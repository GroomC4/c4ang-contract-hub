import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

/**
 * Kafka Messaging Contract for OrderExpirationNotification Event
 *
 * 시나리오: 만료된 주문 자동 취소 (Scheduled Job)
 * - Order Service의 Scheduled Job이 만료된 주문 감지 시 알림 이벤트 발행
 * - Notification Service가 이벤트를 소비하여 고객에게 Push 알림/이메일 발송
 *
 * Avro 스키마: src/main/avro/order/OrderExpirationNotification.avsc
 * Topic: order.expiration.notification
 * Producer: Order Service (Scheduled Job)
 * Consumer: Notification Service
 */
contract {
    description = "주문 만료 알림 이벤트 - Notification Service가 고객 알림 발송"

    input {
        triggeredBy("notifyOrderExpiration()")
    }

    outputMessage {
        sentTo("order.expiration.notification")

        body("""
            {
                "eventId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("170e8400-e29b-41d4-a716-446655440000"))}",
                "eventTimestamp": ${value(client(anyNumber()), server(1705062600000))},
                "orderId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("660e8400-e29b-41d4-a716-446655440000"))}",
                "userId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("770e8400-e29b-41d4-a716-446655440000"))}",
                "expirationReason": "${value(client(anyNonBlankString()), server("결제 시간 초과 (30분)"))}",
                "expiredAt": ${value(client(anyNumber()), server(1705062600000))}
            }
        """.trimIndent())

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")  // orderId
        }
    }
}

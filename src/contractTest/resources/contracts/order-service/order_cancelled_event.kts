import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

/**
 * Kafka Messaging Contract for OrderCancelled Event
 *
 * 시나리오: 주문 취소 (재고 복원)
 * - Order Service가 주문 취소 시 OrderCancelled 이벤트 발행
 * - Product Service가 이벤트를 소비하여 재고 복원
 *
 * 발생 케이스:
 * 1. 결제 실패 후 보상 트랜잭션
 * 2. 사용자 주문 취소
 * 3. 만료된 주문 자동 취소 (Scheduled Job)
 *
 * Avro 스키마: src/main/avro/order/OrderCancelled.avsc
 * Topic: order.cancelled
 * Producer: Order Service
 * Consumer: Product Service
 */
contract {
    description = "주문 취소 이벤트 - Product Service가 재고 복원 처리"

    input {
        triggeredBy("cancelOrder()")
    }

    outputMessage {
        sentTo("order.cancelled")

        body("""
            {
                "eventId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("cc0e8400-e29b-41d4-a716-446655440000"))}",
                "eventTimestamp": ${value(client(anyNumber()), server(1705060850000))},
                "orderId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("660e8400-e29b-41d4-a716-446655440000"))}",
                "userId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("770e8400-e29b-41d4-a716-446655440000"))}",
                "items": [
                    {
                        "productId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("990e8400-e29b-41d4-a716-446655440000"))}",
                        "quantity": ${value(client(anyPositiveInt()), server(2))}
                    }
                ],
                "cancellationReason": "${value(client(regex("PAYMENT_TIMEOUT|USER_REQUESTED|STOCK_UNAVAILABLE|SYSTEM_ERROR")), server("PAYMENT_TIMEOUT"))}",
                "cancelledAt": ${value(client(anyNumber()), server(1705060850000))}
            }
        """.trimIndent())

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")  // orderId
        }
    }
}

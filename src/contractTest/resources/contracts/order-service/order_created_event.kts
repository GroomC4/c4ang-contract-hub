import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

/**
 * Kafka Messaging Contract for OrderCreated Event
 *
 * 시나리오: Order 생성 (완전 비동기 Saga) - Step 1
 * - 고객이 주문을 생성하면 OrderCreated 이벤트 발행
 * - Product Service가 이벤트를 소비하여 재고 예약 처리
 *
 * Avro 스키마: src/main/avro/order/OrderCreated.avsc
 * Topic: order.created
 * Producer: Order Service
 * Consumer: Product Service
 * Saga: Order Creation Saga 시작
 */
contract {
    description = "주문 생성 이벤트 - Product Service가 재고 예약 처리"

    input {
        triggeredBy("createOrder()")
    }

    outputMessage {
        sentTo("order.created")

        body("""
            {
                "eventId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("550e8400-e29b-41d4-a716-446655440000"))}",
                "eventTimestamp": ${value(client(anyNumber()), server(1705060800000))},
                "orderId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("660e8400-e29b-41d4-a716-446655440000"))}",
                "userId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("770e8400-e29b-41d4-a716-446655440000"))}",
                "storeId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("880e8400-e29b-41d4-a716-446655440000"))}",
                "items": [
                    {
                        "productId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("990e8400-e29b-41d4-a716-446655440000"))}",
                        "quantity": ${value(client(anyPositiveInt()), server(2))},
                        "unitPrice": ${value(client(anyDouble()), server(10000.00))}
                    }
                ],
                "totalAmount": ${value(client(anyDouble()), server(20000.00))},
                "createdAt": ${value(client(anyNumber()), server(1705060800000))}
            }
        """.trimIndent())

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")  // orderId
        }
    }
}

import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

/**
 * Kafka Messaging Contract for StockReserved Event
 *
 * 시나리오: Order 생성 (완전 비동기 Saga) - Step 2 (성공)
 * - Product Service가 재고 예약 성공 후 StockReserved 이벤트 발행
 * - Order Service가 이벤트를 소비하여 주문 확정 처리
 *
 * Avro 스키마: src/main/avro/product/StockReserved.avsc
 * Topic: stock.reserved
 * Producer: Product Service
 * Consumer: Order Service
 * Saga: Order Creation Saga Step 2
 */
contract {
    description = "재고 예약 성공 이벤트 - Order Service가 주문 확정 처리"

    input {
        triggeredBy("reserveStock()")
    }

    outputMessage {
        sentTo("stock.reserved")

        body("""
            {
                "eventId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("aa0e8400-e29b-41d4-a716-446655440000"))}",
                "eventTimestamp": ${value(client(anyNumber()), server(1705060801000))},
                "orderId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("660e8400-e29b-41d4-a716-446655440000"))}",
                "reservedItems": [
                    {
                        "productId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("990e8400-e29b-41d4-a716-446655440000"))}",
                        "quantity": ${value(client(anyPositiveInt()), server(2))},
                        "reservedStock": ${value(client(anyPositiveInt()), server(48))}
                    }
                ],
                "reservedAt": ${value(client(anyNumber()), server(1705060801000))}
            }
        """.trimIndent())

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")  // orderId
        }
    }
}

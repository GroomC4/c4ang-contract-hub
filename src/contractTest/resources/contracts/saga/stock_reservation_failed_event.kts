import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

/**
 * Kafka Messaging Contract for SAGA StockReservationFailed Event
 *
 * 시나리오: Order Creation Saga 실패 (재고 부족)
 * - Product Service가 재고 예약 실패 시 SAGA 실패 이벤트 발행
 * - Order Service가 이벤트를 소비하여 보상 트랜잭션 실행 (주문 취소)
 *
 * Avro 스키마: src/main/avro/saga/StockReservationFailed.avsc
 * Topic: saga.stock-reservation.failed
 * Producer: Product Service
 * Consumer: Order Service
 * Saga: Order Creation Saga 보상 트랜잭션
 */
contract {
    description = "SAGA 재고 예약 실패 이벤트 - Order Service가 보상 트랜잭션 실행"

    input {
        triggeredBy("failStockReservation()")
    }

    outputMessage {
        sentTo("saga.stock-reservation.failed")

        body("""
            {
                "eventId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("120e8400-e29b-41d4-a716-446655440000"))}",
                "eventTimestamp": ${value(client(anyNumber()), server(1705060810000))},
                "orderId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("660e8400-e29b-41d4-a716-446655440000"))}",
                "failedItems": [
                    {
                        "productId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("990e8400-e29b-41d4-a716-446655440000"))}",
                        "requestedQuantity": ${value(client(anyPositiveInt()), server(10))},
                        "availableStock": ${value(client(anyPositiveInt()), server(5))}
                    }
                ],
                "failureReason": "${value(client(anyNonBlankString()), server("재고 부족"))}",
                "failedAt": ${value(client(anyNumber()), server(1705060810000))}
            }
        """.trimIndent())

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")  // orderId
        }
    }
}

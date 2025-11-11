import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

/**
 * Kafka Messaging Contract for StockConfirmed Event
 *
 * 시나리오: Payment Saga (재고 확정 성공)
 * - Order Service가 결제 완료 후 재고 확정 성공 시 이벤트 발행
 * - Payment Service가 이벤트를 소비하여 Saga 완료 처리
 *
 * Avro 스키마: src/main/avro/order/StockConfirmed.avsc
 * Topic: order.stock.confirmed
 * Producer: Order Service
 * Consumer: Payment Service
 * Saga: Payment Saga 완료
 */
contract {
    description = "재고 확정 성공 이벤트 - Payment Saga 완료"

    input {
        triggeredBy("confirmStock()")
    }

    outputMessage {
        sentTo("order.stock.confirmed")

        body("""
            {
                "eventId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("100e8400-e29b-41d4-a716-446655440000"))}",
                "eventTimestamp": ${value(client(anyNumber()), server(1705060903000))},
                "orderId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("660e8400-e29b-41d4-a716-446655440000"))}",
                "paymentId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("ee0e8400-e29b-41d4-a716-446655440000"))}",
                "confirmedItems": [
                    {
                        "productId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("990e8400-e29b-41d4-a716-446655440000"))}",
                        "quantity": ${value(client(anyPositiveInt()), server(2))}
                    }
                ],
                "confirmedAt": ${value(client(anyNumber()), server(1705060903000))}
            }
        """.trimIndent())

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")  // orderId
        }
    }
}

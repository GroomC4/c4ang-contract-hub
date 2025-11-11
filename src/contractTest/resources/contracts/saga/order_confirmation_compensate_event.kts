import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

/**
 * Kafka Messaging Contract for SAGA OrderConfirmationCompensate Event
 *
 * 시나리오: 주문 확정 보상 트랜잭션 (결제 실패 시)
 * - Order Service가 결제 실패 감지 시 보상 이벤트 발행
 * - Product Service가 이벤트를 소비하여 재고 복원
 *
 * Avro 스키마: src/main/avro/saga/OrderConfirmationCompensate.avsc
 * Topic: saga.order-confirmation.compensate
 * Producer: Order Service
 * Consumer: Product Service
 * Saga: 보상 트랜잭션
 */
contract {
    description = "SAGA 주문 확정 보상 이벤트 - Product Service가 재고 복원"

    input {
        triggeredBy("compensateOrderConfirmation()")
    }

    outputMessage {
        sentTo("saga.order-confirmation.compensate")

        body("""
            {
                "eventId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("140e8400-e29b-41d4-a716-446655440000"))}",
                "eventTimestamp": ${value(client(anyNumber()), server(1705060925000))},
                "orderId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("660e8400-e29b-41d4-a716-446655440000"))}",
                "items": [
                    {
                        "productId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("990e8400-e29b-41d4-a716-446655440000"))}",
                        "quantity": ${value(client(anyPositiveInt()), server(2))}
                    }
                ],
                "compensationReason": "${value(client(anyNonBlankString()), server("결제 실패"))}",
                "compensatedAt": ${value(client(anyNumber()), server(1705060925000))}
            }
        """.trimIndent())

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")  // orderId
        }
    }
}

import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

/**
 * Kafka Messaging Contract for OrderCreatedEvent
 *
 * 이 Contract는 Kafka를 통해 발행되는 OrderCreatedEvent의 메시지 구조를 정의합니다.
 * Avro 스키마: src/main/avro/events/OrderCreatedEvent.avsc
 * Topic: c4ang.order.created
 */
contract {
    description = "주문 생성 이벤트 - Kafka 메시지 Contract"

    // Kafka 메시지 발행
    input {
        triggeredBy("triggerOrderCreatedEvent()")
    }

    // 발행되는 메시지
    outputMessage {
        sentTo("c4ang.order.created")

        // Avro 직렬화된 메시지 본문
        body("""
            {
                "metadata": {
                    "eventId": "${value(client(regex("[A-Z]+-[0-9]+")), server("EVT-12345"))}",
                    "eventType": "OrderCreated",
                    "timestamp": ${value(client(anyNumber()), server(1704880800000))},
                    "correlationId": "ORD-12345",
                    "version": "1.0",
                    "source": "order-service"
                },
                "orderId": "ORD-12345",
                "customerId": "CUST-001",
                "productId": "PROD-001",
                "quantity": 2,
                "totalAmount": "50000.00",
                "orderStatus": "PENDING_PAYMENT"
            }
        """.trimIndent())

        // Kafka 메시지 헤더
        headers {
            messagingContentType(applicationJson())
        }
    }
}

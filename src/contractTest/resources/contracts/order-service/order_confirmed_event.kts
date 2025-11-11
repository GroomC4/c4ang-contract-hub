import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

/**
 * Kafka Messaging Contract for OrderConfirmed Event
 *
 * 시나리오: Order 생성 (완전 비동기 Saga) - Step 3
 * - Order Service가 재고 예약 성공 후 주문 확정 이벤트 발행
 * - Payment Service가 이벤트를 소비하여 결제 대기 생성
 *
 * Avro 스키마: src/main/avro/order/OrderConfirmed.avsc
 * Topic: order.confirmed
 * Producer: Order Service
 * Consumer: Payment Service
 * Saga: Order Creation Saga Step 3
 */
contract {
    description = "주문 확정 이벤트 - Payment Service가 결제 대기 생성"

    input {
        triggeredBy("confirmOrder()")
    }

    outputMessage {
        sentTo("order.confirmed")

        body("""
            {
                "eventId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("bb0e8400-e29b-41d4-a716-446655440000"))}",
                "eventTimestamp": ${value(client(anyNumber()), server(1705060802000))},
                "orderId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("660e8400-e29b-41d4-a716-446655440000"))}",
                "userId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("770e8400-e29b-41d4-a716-446655440000"))}",
                "totalAmount": ${value(client(anyDouble()), server(20000.00))},
                "confirmedAt": ${value(client(anyNumber()), server(1705060802000))}
            }
        """.trimIndent())

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")  // orderId
        }
    }
}

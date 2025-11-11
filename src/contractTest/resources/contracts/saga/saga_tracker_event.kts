import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

/**
 * Kafka Messaging Contract for SAGA Tracker Event
 *
 * 시나리오: Saga 흐름 추적 및 감사
 * - 모든 Saga 단계에서 추적 이벤트 발행
 * - Saga Tracker Service가 이벤트를 소비하여 흐름 추적 및 감사 로그 저장
 *
 * Avro 스키마: src/main/avro/saga/SagaTracker.avsc
 * Topic: saga.tracker
 * Producer: Order Service, Product Service, Payment Service
 * Consumer: Saga Tracker Service
 */
contract {
    description = "SAGA 추적 이벤트 - Saga 흐름 추적 및 감사"

    input {
        triggeredBy("trackSagaProgress()")
    }

    outputMessage {
        sentTo("saga.tracker")

        body("""
            {
                "eventId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("160e8400-e29b-41d4-a716-446655440000"))}",
                "eventTimestamp": ${value(client(anyNumber()), server(1705060800000))},
                "sagaId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("660e8400-e29b-41d4-a716-446655440000"))}",
                "sagaType": "${value(client(regex("ORDER_CREATION|PAYMENT_COMPLETION")), server("ORDER_CREATION"))}",
                "step": "${value(client(anyNonBlankString()), server("STOCK_RESERVATION"))}",
                "status": "${value(client(regex("STARTED|IN_PROGRESS|COMPLETED|FAILED|COMPENSATED")), server("IN_PROGRESS"))}",
                "orderId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("660e8400-e29b-41d4-a716-446655440000"))}",
                "metadata": ${value(client(optional(anyNonBlankString())), server(null))},
                "recordedAt": ${value(client(anyNumber()), server(1705060800000))}
            }
        """.trimIndent())

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")  // sagaId
        }
    }
}

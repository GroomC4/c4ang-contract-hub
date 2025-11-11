package contracts.saga

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Saga 흐름 추적 이벤트를 발행한다"

    label "saga.tracker"

    input {
        triggeredBy("trackSagaProgress()")
    }

    outputMessage {
        sentTo "saga.tracker"

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", $(producer(anyUuid())))  // sagaId
        }

        body([
            eventId: $(producer(anyUuid()), consumer("d10e8400-e29b-41d4-a716-446655440000")),
            eventTimestamp: $(producer(anyPositiveInt()), consumer(1705060800000L)),
            sagaId: $(producer(anyUuid()), consumer("660e8400-e29b-41d4-a716-446655440000")),
            sagaType: $(producer(regex("ORDER_CREATION|PAYMENT_COMPLETION")), consumer("ORDER_CREATION")),
            step: $(producer(anyNonEmptyString()), consumer("STOCK_RESERVATION")),
            status: $(producer(regex("STARTED|IN_PROGRESS|COMPLETED|FAILED|COMPENSATED")), consumer("IN_PROGRESS")),
            orderId: $(producer(anyUuid()), consumer("660e8400-e29b-41d4-a716-446655440000")),
            metadata: $(producer(optional(anyNonEmptyString())), consumer(null)),
            recordedAt: $(producer(anyPositiveInt()), consumer(1705060800000L))
        ])
    }
}

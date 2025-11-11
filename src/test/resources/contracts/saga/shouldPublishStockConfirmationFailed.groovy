package contracts.saga

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Order Service가 재고 확정 실패 이벤트를 발행한다 (SAGA)"

    label "saga.stock-confirmation.failed"

    input {
        triggeredBy("failStockConfirmation()")
    }

    outputMessage {
        sentTo "saga.stock-confirmation.failed"

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", $(producer(anyUuid())))  // orderId
        }

        body([
            eventId: $(producer(anyUuid()), consumer("b10e8400-e29b-41d4-a716-446655440000")),
            eventTimestamp: $(producer(anyPositiveInt()), consumer(1705060800000L)),
            orderId: $(producer(anyUuid()), consumer("880e8400-e29b-41d4-a716-446655440000")),
            paymentId: $(producer(anyUuid()), consumer("cc0e8400-e29b-41d4-a716-446655440000")),
            failureReason: $(producer(anyNonEmptyString()), consumer("재고 부족")),
            failedAt: $(producer(anyPositiveInt()), consumer(1705060800000L))
        ])
    }
}

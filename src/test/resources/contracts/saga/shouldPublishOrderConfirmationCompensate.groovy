package contracts.saga

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Order Service가 주문 확정 보상 이벤트를 발행한다 (결제 실패 시)"

    label "saga.order-confirmation.compensate"

    input {
        triggeredBy("compensateOrderConfirmation()")
    }

    outputMessage {
        sentTo "saga.order-confirmation.compensate"

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", $(producer(anyUuid())))  // orderId
        }

        body([
            eventId: $(producer(anyUuid()), consumer("c10e8400-e29b-41d4-a716-446655440000")),
            eventTimestamp: $(producer(anyPositiveInt()), consumer(1705060800000L)),
            orderId: $(producer(anyUuid()), consumer("660e8400-e29b-41d4-a716-446655440000")),
            items: [
                [
                    productId: $(producer(anyUuid()), consumer("990e8400-e29b-41d4-a716-446655440000")),
                    quantity: $(producer(anyPositiveInt()), consumer(2))
                ]
            ],
            compensationReason: $(producer(anyNonEmptyString()), consumer("결제 실패")),
            compensatedAt: $(producer(anyPositiveInt()), consumer(1705060800000L))
        ])
    }
}

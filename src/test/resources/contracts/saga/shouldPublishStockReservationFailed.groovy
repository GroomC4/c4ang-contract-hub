package contracts.saga

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Product Service가 재고 예약 실패 이벤트를 발행한다 (SAGA)"

    label "saga.stock-reservation.failed"

    input {
        triggeredBy("failStockReservation()")
    }

    outputMessage {
        sentTo "saga.stock-reservation.failed"

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", $(producer(anyUuid())))  // orderId
        }

        body([
            eventId: $(producer(anyUuid()), consumer("a10e8400-e29b-41d4-a716-446655440000")),
            eventTimestamp: $(producer(anyPositiveInt()), consumer(1705060800000L)),
            orderId: $(producer(anyUuid()), consumer("660e8400-e29b-41d4-a716-446655440000")),
            failedItems: [
                [
                    productId: $(producer(anyUuid()), consumer("990e8400-e29b-41d4-a716-446655440000")),
                    requestedQuantity: $(producer(anyPositiveInt()), consumer(10)),
                    availableStock: $(producer(anyPositiveInt()), consumer(5))
                ]
            ],
            failureReason: $(producer(anyNonEmptyString()), consumer("재고 부족")),
            failedAt: $(producer(anyPositiveInt()), consumer(1705060800000L))
        ])
    }
}

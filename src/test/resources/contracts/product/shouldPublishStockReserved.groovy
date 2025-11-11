package contracts.product

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Product Service가 StockReserved 이벤트를 발행한다"

    label "stock.reserved"

    input {
        // Producer 트리거: 재고 예약 메서드 호출
        triggeredBy("reserveStock()")
    }

    outputMessage {
        sentTo "stock.reserved"

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", $(producer(anyUuid())))
        }

        body([
            eventId: $(producer(anyUuid()), consumer("aa0e8400-e29b-41d4-a716-446655440000")),
            eventTimestamp: $(producer(anyPositiveInt()), consumer(1705060800000L)),
            orderId: $(producer(anyUuid()), consumer("660e8400-e29b-41d4-a716-446655440000")),
            reservedItems: [
                [
                    productId: $(producer(anyUuid()), consumer("990e8400-e29b-41d4-a716-446655440000")),
                    quantity: $(producer(anyPositiveInt()), consumer(2)),
                    reservedStock: $(producer(anyPositiveInt()), consumer(48))
                ]
            ],
            reservedAt: $(producer(anyPositiveInt()), consumer(1705060800000L))
        ])
    }
}

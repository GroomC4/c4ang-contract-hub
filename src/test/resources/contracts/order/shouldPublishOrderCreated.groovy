package contracts.order

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Order Service가 OrderCreated 이벤트를 발행한다"

    label "order.created"

    input {
        // Producer 트리거: Order 생성 API 호출
        triggeredBy("createOrder()")
    }

    outputMessage {
        sentTo "order.created"

        // Partition Key
        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", $(producer(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))))
        }

        // Avro 스키마 기반 Body
        body([
            eventId: $(producer(anyUuid()), consumer("550e8400-e29b-41d4-a716-446655440000")),
            eventTimestamp: $(producer(anyPositiveInt()), consumer(1705060800000L)),
            orderId: $(producer(anyUuid()), consumer("660e8400-e29b-41d4-a716-446655440000")),
            userId: $(producer(anyUuid()), consumer("770e8400-e29b-41d4-a716-446655440000")),
            storeId: $(producer(anyUuid()), consumer("880e8400-e29b-41d4-a716-446655440000")),
            items: [
                [
                    productId: $(producer(anyUuid()), consumer("990e8400-e29b-41d4-a716-446655440000")),
                    quantity: $(producer(anyPositiveInt()), consumer(2)),
                    unitPrice: $(producer(anyDouble()), consumer(10000.00))
                ]
            ],
            totalAmount: $(producer(anyDouble()), consumer(20000.00)),
            createdAt: $(producer(anyPositiveInt()), consumer(1705060800000L))
        ])
    }
}

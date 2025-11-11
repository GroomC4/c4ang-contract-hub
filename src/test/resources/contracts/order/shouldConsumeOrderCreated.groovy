package contracts.order

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Product Service가 OrderCreated 이벤트를 소비하고 재고를 예약한다"

    label "product.service.order.created"

    input {
        // Consumer 입력: Kafka 메시지
        messageFrom "order.created"

        messageHeaders {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")
        }

        messageBody([
            eventId: "550e8400-e29b-41d4-a716-446655440000",
            eventTimestamp: 1705060800000L,
            orderId: "660e8400-e29b-41d4-a716-446655440000",
            userId: "770e8400-e29b-41d4-a716-446655440000",
            storeId: "880e8400-e29b-41d4-a716-446655440000",
            items: [
                [
                    productId: "990e8400-e29b-41d4-a716-446655440000",
                    quantity: 2,
                    unitPrice: 10000.00
                ]
            ],
            totalAmount: 20000.00,
            createdAt: 1705060800000L
        ])
    }

    outputMessage {
        sentTo "stock.reserved"

        body([
            eventId: $(anyUuid()),
            eventTimestamp: $(anyPositiveInt()),
            orderId: "660e8400-e29b-41d4-a716-446655440000",
            reservedItems: [
                [
                    productId: "990e8400-e29b-41d4-a716-446655440000",
                    quantity: 2,
                    reservedStock: $(anyPositiveInt())
                ]
            ],
            reservedAt: $(anyPositiveInt())
        ])
    }
}

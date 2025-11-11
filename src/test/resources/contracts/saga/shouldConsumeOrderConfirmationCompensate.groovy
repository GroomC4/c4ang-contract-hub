package contracts.saga

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Product Service가 주문 확정 보상 이벤트를 소비하고 재고를 복원한다"

    label "product.service.saga.order-confirmation.compensate"

    input {
        messageFrom "saga.order-confirmation.compensate"

        messageHeaders {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")
        }

        messageBody([
            eventId: "c10e8400-e29b-41d4-a716-446655440000",
            eventTimestamp: 1705060800000L,
            orderId: "660e8400-e29b-41d4-a716-446655440000",
            items: [
                [
                    productId: "990e8400-e29b-41d4-a716-446655440000",
                    quantity: 2
                ]
            ],
            compensationReason: "결제 실패",
            compensatedAt: 1705060800000L
        ])
    }

    outputMessage {
        // 재고 복원 검증
        assertThat("productRepository.findById('990e8400-e29b-41d4-a716-446655440000').get().stock >= 2")
    }
}

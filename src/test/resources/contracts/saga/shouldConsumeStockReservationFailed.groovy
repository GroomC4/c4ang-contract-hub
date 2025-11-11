package contracts.saga

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Order Service가 재고 예약 실패 이벤트를 소비하고 주문을 취소한다 (SAGA 보상)"

    label "order.service.saga.stock-reservation.failed"

    input {
        messageFrom "saga.stock-reservation.failed"

        messageHeaders {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "660e8400-e29b-41d4-a716-446655440000")
        }

        messageBody([
            eventId: "a10e8400-e29b-41d4-a716-446655440000",
            eventTimestamp: 1705060800000L,
            orderId: "660e8400-e29b-41d4-a716-446655440000",
            failedItems: [
                [
                    productId: "990e8400-e29b-41d4-a716-446655440000",
                    requestedQuantity: 10,
                    availableStock: 5
                ]
            ],
            failureReason: "재고 부족",
            failedAt: 1705060800000L
        ])
    }

    outputMessage {
        // 주문 취소 처리 (보상 트랜잭션)
        assertThat("orderRepository.findById('660e8400-e29b-41d4-a716-446655440000').get().status == OrderStatus.ORDER_CANCELLED")
    }
}

package contracts.payment

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Order Service가 결제 완료 이벤트를 소비하고 재고를 확정한다"
    label "order.service.payment.completed"

    input {
        messageFrom "payment.completed"

        messageHeaders {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "880e8400-e29b-41d4-a716-446655440000")
        }

        messageBody([
            eventId: "bb0e8400-e29b-41d4-a716-446655440000",
            eventTimestamp: 1705060800000L,
            paymentId: "cc0e8400-e29b-41d4-a716-446655440000",
            orderId: "880e8400-e29b-41d4-a716-446655440000",
            userId: "770e8400-e29b-41d4-a716-446655440000",
            totalAmount: 50000.00,
            paymentMethod: "CARD",
            pgApprovalNumber: "APPROVAL-12345",
            completedAt: 1705060800000L
        ])
    }

    outputMessage {
        sentTo "order.stock.confirmed"

        body([
            eventId: $(anyUuid()),
            eventTimestamp: $(anyPositiveInt()),
            orderId: "880e8400-e29b-41d4-a716-446655440000",
            paymentId: "cc0e8400-e29b-41d4-a716-446655440000",
            confirmedItems: [
                [productId: $(anyUuid()), quantity: $(anyPositiveInt())]
            ],
            confirmedAt: $(anyPositiveInt())
        ])
    }
}

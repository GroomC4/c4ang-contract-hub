package contracts.saga

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Payment Service가 재고 확정 실패 이벤트를 소비하고 결제를 취소한다 (SAGA 보상)"

    label "payment.service.saga.stock-confirmation.failed"

    input {
        messageFrom "saga.stock-confirmation.failed"

        messageHeaders {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "880e8400-e29b-41d4-a716-446655440000")
        }

        messageBody([
            eventId: "b10e8400-e29b-41d4-a716-446655440000",
            eventTimestamp: 1705060800000L,
            orderId: "880e8400-e29b-41d4-a716-446655440000",
            paymentId: "cc0e8400-e29b-41d4-a716-446655440000",
            failureReason: "재고 부족",
            failedAt: 1705060800000L
        ])
    }

    outputMessage {
        sentTo "saga.payment-completion.compensate"

        body([
            eventId: $(anyUuid()),
            eventTimestamp: $(anyPositiveInt()),
            paymentId: "cc0e8400-e29b-41d4-a716-446655440000",
            orderId: "880e8400-e29b-41d4-a716-446655440000",
            refundAmount: $(anyDouble()),
            compensationReason: "재고 확정 실패",
            compensatedAt: $(anyPositiveInt())
        ])
    }
}

package contracts.payment

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Payment Service가 결제 완료 이벤트를 발행한다"
    label "payment.completed"

    input {
        triggeredBy("completePayment()")
    }

    outputMessage {
        sentTo "payment.completed"

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", $(producer(anyUuid())))  // orderId
        }

        body([
            eventId: $(producer(anyUuid()), consumer("bb0e8400-e29b-41d4-a716-446655440000")),
            eventTimestamp: $(producer(anyPositiveInt()), consumer(1705060800000L)),
            paymentId: $(producer(anyUuid()), consumer("cc0e8400-e29b-41d4-a716-446655440000")),
            orderId: $(producer(anyUuid()), consumer("880e8400-e29b-41d4-a716-446655440000")),
            userId: $(producer(anyUuid()), consumer("770e8400-e29b-41d4-a716-446655440000")),
            totalAmount: $(producer(anyDouble()), consumer(50000.00)),
            paymentMethod: "CARD",
            pgApprovalNumber: "APPROVAL-12345",
            completedAt: $(producer(anyPositiveInt()), consumer(1705060800000L))
        ])
    }
}

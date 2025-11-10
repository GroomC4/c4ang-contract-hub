import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

/**
 * Kafka Messaging Contract for PaymentCompletedEvent
 *
 * Avro 스키마: src/main/avro/events/PaymentCompletedEvent.avsc
 * Topic: c4ang.payment.completed
 */
contract {
    description = "결제 완료 이벤트 - Kafka 메시지 Contract"

    input {
        triggeredBy("triggerPaymentCompletedEvent()")
    }

    outputMessage {
        sentTo("c4ang.payment.completed")

        body("""
            {
                "metadata": {
                    "eventId": "${value(client(regex("[A-Z]+-[0-9]+")), server("EVT-12346"))}",
                    "eventType": "PaymentCompleted",
                    "timestamp": ${value(client(anyNumber()), server(1704880805000))},
                    "correlationId": "ORD-12345",
                    "version": "1.0",
                    "source": "payment-service"
                },
                "paymentId": "PAY-12345",
                "orderId": "ORD-12345",
                "amount": "50000.00",
                "paymentMethod": "CREDIT_CARD",
                "transactionId": "TXN-67890"
            }
        """.trimIndent())

        headers {
            messagingContentType(applicationJson())
        }
    }
}

package contracts.monitoring

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Product Service가 재고 불일치 알림을 발행한다"

    label "stock.sync.alert"

    input {
        triggeredBy("detectStockDiscrepancy()")
    }

    outputMessage {
        sentTo "stock.sync.alert"

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", $(producer(anyUuid())))  // productId
        }

        body([
            eventId: $(producer(anyUuid()), consumer("ee0e8400-e29b-41d4-a716-446655440000")),
            eventTimestamp: $(producer(anyPositiveInt()), consumer(1705060800000L)),
            productId: $(producer(anyUuid()), consumer("990e8400-e29b-41d4-a716-446655440000")),
            dbStock: $(producer(anyPositiveInt()), consumer(100)),
            redisStock: $(producer(anyPositiveInt()), consumer(95)),
            discrepancy: $(producer(anyPositiveInt()), consumer(5)),
            action: $(producer(regex("REDIS_RESTORED|MANUAL_REVIEW_REQUIRED")), consumer("REDIS_RESTORED")),
            severity: $(producer(regex("LOW|MEDIUM|HIGH|CRITICAL")), consumer("MEDIUM")),
            detectedAt: $(producer(anyPositiveInt()), consumer(1705060800000L))
        ])
    }
}

package contracts.analytics

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Order Service가 일일 통계 리포트를 발행한다"

    label "analytics.daily.statistics"

    input {
        triggeredBy("generateDailyStatistics()")
    }

    outputMessage {
        sentTo "analytics.daily.statistics"

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", $(producer(regex("\\d{4}-\\d{2}-\\d{2}"))))  // date
        }

        body([
            eventId: $(producer(anyUuid()), consumer("ff0e8400-e29b-41d4-a716-446655440000")),
            eventTimestamp: $(producer(anyPositiveInt()), consumer(1705060800000L)),
            date: $(producer(regex("\\d{4}-\\d{2}-\\d{2}")), consumer("2025-01-12")),
            totalOrders: $(producer(anyPositiveInt()), consumer(150)),
            totalSales: $(producer(anyDouble()), consumer(15000000.00)),
            avgOrderAmount: $(producer(anyDouble()), consumer(100000.00)),
            topProducts: [
                [
                    productId: $(producer(anyUuid()), consumer("990e8400-e29b-41d4-a716-446655440000")),
                    productName: $(producer(anyNonEmptyString()), consumer("Best Product")),
                    totalSold: $(producer(anyPositiveInt()), consumer(50))
                ]
            ],
            generatedAt: $(producer(anyPositiveInt()), consumer(1705060800000L))
        ])
    }
}

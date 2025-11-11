import org.springframework.cloud.contract.spec.ContractDsl.Companion.contract

/**
 * Kafka Messaging Contract for StoreDeleted Event
 *
 * 시나리오: Store 삭제 (Cascade Operation)
 * - Store 소유자가 스토어를 삭제하면 StoreDeleted 이벤트 발행
 * - Product Service가 해당 이벤트를 소비하여 연관 상품 DISCONTINUED 처리
 *
 * Avro 스키마: src/main/avro/store/StoreDeleted.avsc
 * Topic: store.deleted
 * Producer: Store Service
 * Consumer: Product Service
 */
contract {
    description = "스토어 삭제 이벤트 - Product Service가 연관 상품 처리"

    // Producer 트리거
    input {
        triggeredBy("deleteStore()")
    }

    // Kafka 메시지 발행
    outputMessage {
        sentTo("store.deleted")

        // Avro 스키마 기반 메시지 본문
        body("""
            {
                "eventId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("550e8400-e29b-41d4-a716-446655440000"))}",
                "eventTimestamp": ${value(client(anyNumber()), server(1705060800000))},
                "storeId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("770e8400-e29b-41d4-a716-446655440000"))}",
                "ownerId": "${value(client(regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")), server("880e8400-e29b-41d4-a716-446655440000"))}",
                "deletedAt": ${value(client(anyNumber()), server(1705060800000))}
            }
        """.trimIndent())

        // Kafka 메시지 헤더 (파티션 키: storeId)
        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "770e8400-e29b-41d4-a716-446655440000")
        }
    }
}

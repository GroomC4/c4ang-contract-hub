package contracts.store

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Store Service가 StoreDeleted 이벤트를 발행한다"

    label "store.deleted"

    input {
        triggeredBy("deleteStore()")
    }

    outputMessage {
        sentTo "store.deleted"

        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", $(producer(anyUuid())))  // storeId
        }

        body([
            eventId: $(producer(anyUuid()), consumer("dd0e8400-e29b-41d4-a716-446655440000")),
            eventTimestamp: $(producer(anyPositiveInt()), consumer(1705060800000L)),
            storeId: $(producer(anyUuid()), consumer("880e8400-e29b-41d4-a716-446655440000")),
            ownerId: $(producer(anyUuid()), consumer("770e8400-e29b-41d4-a716-446655440000")),
            deletedAt: $(producer(anyPositiveInt()), consumer(1705060800000L))
        ])
    }
}

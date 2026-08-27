package contracts.messaging

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "transfer_incoming_notification"
    label "transfer_incoming_notification"
    input {
        triggeredBy("transferIncoming()")
    }
    outputMessage {
        sentTo "bank.notification"
        body([
                eventId       : "55555555-5555-5555-5555-555555555555",
                operationId   : "66666666-6666-6666-6666-666666666666",
                source        : "TRANSFER",
                type          : "TRANSFER_INCOMING",
                recipientLogin: "dmitry",
                message       : "Получен перевод от alexey: 741.94 CNY",
                occurredAt    : "2026-08-27T05:02:00Z",
                amount        : "741.94",
                currency      : "CNY"
        ])
        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "dmitry")
        }
    }
}
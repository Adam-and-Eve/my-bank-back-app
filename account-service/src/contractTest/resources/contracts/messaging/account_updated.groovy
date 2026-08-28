package contracts.messaging

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "account_updated_notification"
    label "account_updated_notification"
    input {
        triggeredBy("accountUpdated()")
    }
    outputMessage {
        sentTo "bank.notification"
        body([
                eventId       : "11111111-1111-1111-1111-111111111111",
                operationId   : "22222222-2222-2222-2222-222222222222",
                source        : "ACCOUNT",
                type          : "ACCOUNT_UPDATED",
                recipientLogin: "alexey",
                message       : "Данные профиля обновлены",
                occurredAt    : "2026-08-27T05:00:00Z",
                amount        : null,
                currency      : null
        ])
        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "alexey")
        }
    }
}
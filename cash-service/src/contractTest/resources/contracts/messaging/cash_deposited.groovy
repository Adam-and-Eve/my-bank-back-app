package contracts.messaging

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "cash_deposited_notification"
    label "cash_deposited_notification"
    input {
        triggeredBy("cashDeposited()")
    }
    outputMessage {
        sentTo "bank.notification"
        body([
                eventId       : "33333333-3333-3333-3333-333333333333",
                operationId   : "44444444-4444-4444-4444-444444444444",
                source        : "CASH",
                type          : "CASH_DEPOSITED",
                recipientLogin: "alexey",
                message       : "Счёт пополнен на 100.00 RUB",
                occurredAt    : "2026-08-27T05:01:00Z",
                amount        : "100.00",
                currency      : "RUB"
        ])
        headers {
            messagingContentType(applicationJson())
            header("kafka_messageKey", "alexey")
        }
    }
}
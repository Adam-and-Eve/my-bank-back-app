package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "create_notification"
    request {
        method POST()
        url "/api/notification"
        headers {
            contentType applicationJson()
        }
        body(
                recipientLogin: "dmitry",
                type: "CASH_DEPOSIT",
                message: "Счёт пополнен на 250.00 RUB",
                operationId: "operation-1"
        )
    }
    response {
        status ACCEPTED()
        headers {
            contentType applicationJson()
        }
        body(
                status: "ACCEPTED"
        )
    }
}
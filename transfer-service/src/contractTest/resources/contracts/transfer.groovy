package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    name "create_transfer"
    request {
        method POST()
        url "/api/transfer"
        headers {
            contentType applicationJson()
            header "Authorization", "Bearer token"
            header "Idempotency-Key", "99999999-9999-9999-9999-999999999999"
        }
        body(
                recipientLogin: "dmitry",
                amount: "150.00",
                currency: "RUB"
        )
    }
    response {
        status OK()
        headers {
            contentType applicationJson()
        }
        body(
                senderLogin: "alexey",
                recipientLogin: "dmitry",
                senderBalance: "850.00",
                currency: "RUB",
                message: "Transfer completed"
        )
    }
}
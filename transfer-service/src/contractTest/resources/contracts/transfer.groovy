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
                senderLogin: "dmitry",
                recipientLogin: "alexey",
                senderBalance: "850.00",
                currency: "RUB",
                message: "Transfer completed"
        )
    }
}
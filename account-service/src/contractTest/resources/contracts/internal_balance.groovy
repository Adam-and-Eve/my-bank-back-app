package contracts

import org.springframework.cloud.contract.spec.Contract

[
        Contract.make {
            name "deposit_internal_balance"
            request {
                method POST()
                url "/api/account/internal/balance/deposit"
                headers {
                    contentType applicationJson()
                }
                body(
                        login: "dmitry",
                        amount: "250.00",
                        currency: "RUB",
                        operationId: "operation-1"
                )
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body(
                        login: "dmitry",
                        balance: "1000250.00",
                        currency: "RUB"
                )
            }
        },
        Contract.make {
            name "withdraw_internal_balance"
            request {
                method POST()
                url "/api/account/internal/balance/withdraw"
                headers {
                    contentType applicationJson()
                }
                body(
                        login: "dmitry",
                        amount: "100.00",
                        currency: "RUB",
                        operationId: "operation-2"
                )
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body(
                        login: "dmitry",
                        balance: "999900.00",
                        currency: "RUB"
                )
            }
        },
        Contract.make {
            name "transfer_internal_balance"
            request {
                method POST()
                url "/api/account/internal/balance/transfer"
                headers {
                    contentType applicationJson()
                }
                body(
                        senderLogin: "dmitry",
                        recipientLogin: "alexey",
                        amount: "150.00",
                        currency: "RUB",
                        operationId: "operation-3"
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
                        senderBalance: "999850.00",
                        currency: "RUB"
                )
            }
        }
]
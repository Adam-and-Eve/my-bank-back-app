package contracts

import org.springframework.cloud.contract.spec.Contract

[
        Contract.make {
            name "get_current_account"
            request {
                method GET()
                url "/api/account/me"
                headers {
                    header "Authorization", "Bearer token"
                }
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body(
                        login: "dmitry",
                        name: "Дмитрий Волков",
                        birthdate: "1999-09-19",
                        balance: "1000000.00",
                        currency: "RUB"
                )
            }
        },
        Contract.make {
            name "update_current_account"
            request {
                method PUT()
                url "/api/account/me"
                headers {
                    contentType applicationJson()
                    header "Authorization", "Bearer token"
                }
                body(
                        name: "Дмитрий Обновлённый",
                        birthdate: "1999-09-19"
                )
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body(
                        login: "dmitry",
                        name: "Дмитрий Обновлённый",
                        birthdate: "1999-09-19",
                        balance: "1000000.00",
                        currency: "RUB"
                )
            }
        },
        Contract.make {
            name "get_recipients"
            request {
                method GET()
                url "/api/account/recipients"
                headers {
                    header "Authorization", "Bearer token"
                }
            }
            response {
                status OK()
                headers {
                    contentType applicationJson()
                }
                body([
                        [
                                login: "alexey",
                                name : "Алексей Морозов"
                        ],
                        [
                                login: "elena",
                                name : "Елена Кузнецова"
                        ]
                ])
            }
        }
]
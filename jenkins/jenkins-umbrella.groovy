def runCommand(String linuxCommand, String windowsCommand = null) {
    if (isUnix()) {
        sh linuxCommand
    } else {
        bat(windowsCommand ?: linuxCommand)
    }
}

def gradle(String args) {
    runCommand(
            "./gradlew --no-daemon --console=plain ${args}",
            ".\\gradlew.bat --no-daemon --console=plain ${args}"
    )
}

def serviceValuesArgs(List services) {
    services.collect { service ->
        "-f helm/values/services/${service.name}.yaml"
    }.join(' ')
}

def decryptSecrets(String environment) {
    withCredentials([
            file(
                    credentialsId: 'my-bank-sops-age-key',
                    variable: 'SOPS_AGE_KEY_FILE'
            )
    ]) {
        runCommand(
                "mkdir -p envs/runtime && " +
                        "sops --decrypt " +
                        "envs/secrets/values-secrets-${environment}.enc.yaml " +
                        "> envs/runtime/values-secrets-${environment}.yaml"
        )

        runCommand(
                "sops --decrypt " +
                        "envs/secrets/my-bank-realm-realm.enc.json " +
                        "> envs/runtime/my-bank-realm-realm.json"
        )
    }
}

def cleanupRuntimeSecrets() {
    runCommand(
            "rm -f " +
                    "envs/runtime/values-secrets-test.yaml " +
                    "envs/runtime/values-secrets-prod.yaml " +
                    "envs/runtime/my-bank-realm-realm.json",

            "if exist envs\\runtime\\values-secrets-test.yaml del /f /q envs\\runtime\\values-secrets-test.yaml " +
                    "& if exist envs\\runtime\\values-secrets-prod.yaml del /f /q envs\\runtime\\values-secrets-prod.yaml " +
                    "& if exist envs\\runtime\\my-bank-realm-realm.json del /f /q envs\\runtime\\my-bank-realm-realm.json"
    )
}

def createKeycloakRealmSecret(String namespace) {
    withCredentials([
            file(
                    credentialsId: 'my-bank-kubeconfig',
                    variable: 'KUBECONFIG'
            )
    ]) {
        runCommand(
                "kubectl create secret generic keycloak-realm " +
                        "--from-file=my-bank-realm-realm.json=envs/runtime/my-bank-realm-realm.json " +
                        "--namespace ${namespace} " +
                        "--dry-run=client -o yaml | kubectl apply -f -"
        )
    }
}

def helmDeploy(
        List services,
        String namespace,
        String valuesFile,
        String secretsFile,
        String imageRegistry,
        String imageTag
) {
    withCredentials([
            file(
                    credentialsId: 'my-bank-kubeconfig',
                    variable: 'KUBECONFIG'
            )
    ]) {
        def values = serviceValuesArgs(services)

        def command =
                "helm upgrade --install my-bank helm/my-bank " +
                        "--namespace ${namespace} " +
                        "--create-namespace " +
                        "--rollback-on-failure " +
                        "--timeout 5m " +
                        "-f ${valuesFile} " +
                        "${values} " +
                        "-f ${secretsFile} " +
                        "--set global.imageRegistry=${imageRegistry} " +
                        "--set global.imageTag=${imageTag}"

        runCommand(
                "${command} --dry-run=client"
        )

        runCommand(command)
    }
}

def runUmbrellaPipeline() {
    def services = [
            [name: 'notification-service', image: 'my-bank-notification-service'],
            [name: 'cash-service', image: 'my-bank-cash-service'],
            [name: 'blocker-service', image: 'my-bank-blocker-service'],
            [name: 'transfer-service', image: 'my-bank-transfer-service'],
            [name: 'exchange-service', image: 'my-bank-exchange-service'],
            [name: 'exchange-generator-service', image: 'my-bank-exchange-generator-service'],
            [name: 'account-service', image: 'my-bank-account-service'],
            [name: 'front-ui-service', image: 'my-bank-front-ui-service'],
            [name: 'api-gateway', image: 'my-bank-api-gateway']
    ]

    properties([
            parameters([
                    string(
                            name: 'IMAGE_REGISTRY',
                            defaultValue: 'registry.example.com/my-bank',
                            description: 'Container registry namespace'
                    ),
                    string(
                            name: 'IMAGE_TAG',
                            defaultValue: '',
                            description: 'Image tag. Empty value uses Jenkins BUILD_NUMBER.'
                    ),
                    booleanParam(
                            name: 'BUILD_IMAGES',
                            defaultValue: false,
                            description: 'Build all images using Docker'
                    ),
                    booleanParam(
                            name: 'PUSH_IMAGES',
                            defaultValue: false,
                            description: 'Build and push all images to registry'
                    ),
                    booleanParam(
                            name: 'DEPLOY_TEST',
                            defaultValue: false,
                            description: 'Deploy umbrella chart to test namespace'
                    ),
                    booleanParam(
                            name: 'DEPLOY_PROD',
                            defaultValue: false,
                            description: 'Deploy umbrella chart to prod namespace after manual approval'
                    )
            ])
    ])

    def imageTag = params.IMAGE_TAG?.trim()
            ? params.IMAGE_TAG.trim()
            : env.BUILD_NUMBER

    def registryHost = params.IMAGE_REGISTRY.tokenize('/')[0]

    try {
        stage('Validate') {
            gradle('projects')
        }

        stage('Java tests') {
            gradle('test contractTest')
        }

        stage('bootJar') {
            gradle('clean bootJar')
        }

        stage('Docker build') {
            if (params.BUILD_IMAGES || params.PUSH_IMAGES) {
                services.each { service ->
                    runCommand(
                            "docker build " +
                                    "-t ${params.IMAGE_REGISTRY}/${service.image}:${imageTag} " +
                                    "${service.name}"
                    )
                }
            } else {
                echo 'Docker builds skipped by parameter.'
            }
        }

        stage('Image push') {
            if (params.PUSH_IMAGES) {
                withCredentials([
                        usernamePassword(
                                credentialsId: 'my-bank-registry-credentials',
                                usernameVariable: 'REGISTRY_USERNAME',
                                passwordVariable: 'REGISTRY_PASSWORD'
                        )
                ]) {
                    runCommand(
                            "printf '%s' \"\\$REGISTRY_PASSWORD\" | " +
                                    "docker login ${registryHost} " +
                                    "--username \"\\$REGISTRY_USERNAME\" " +
                                    "--password-stdin",

                            "echo %REGISTRY_PASSWORD%| " +
                                    "docker login ${registryHost} " +
                                    "--username %REGISTRY_USERNAME% " +
                                    "--password-stdin"
                    )

                    services.each { service ->
                        runCommand(
                                "docker push " +
                                        "${params.IMAGE_REGISTRY}/${service.image}:${imageTag}"
                        )
                    }
                }
            } else {
                echo 'Image push skipped by parameter.'
            }
        }

        stage('Prepare test secrets') {
            if (params.DEPLOY_TEST) {
                decryptSecrets('test')
            } else {
                echo 'Test secrets preparation skipped by parameter.'
            }
        }

        stage('Helm lint and template') {
            def values = serviceValuesArgs(services)

            runCommand('helm dependency update helm/my-bank')

            runCommand(
                    "helm lint helm/my-bank " +
                            "-f helm/my-bank/values-test.yaml " +
                            "${values}"
            )

            if (params.DEPLOY_TEST) {
                runCommand(
                        "helm lint helm/my-bank " +
                                "-f helm/my-bank/values-test.yaml " +
                                "${values} " +
                                "-f envs/runtime/values-secrets-test.yaml"
                )

                runCommand(
                        "helm template my-bank helm/my-bank " +
                                "--namespace test " +
                                "-f helm/my-bank/values-test.yaml " +
                                "${values} " +
                                "-f envs/runtime/values-secrets-test.yaml " +
                                "--set global.imageRegistry=${params.IMAGE_REGISTRY} " +
                                "--set global.imageTag=${imageTag}"
                )
            } else {
                runCommand(
                        "helm template my-bank helm/my-bank " +
                                "--namespace test " +
                                "-f helm/my-bank/values-test.yaml " +
                                "${values} " +
                                "--set global.imageRegistry=${params.IMAGE_REGISTRY} " +
                                "--set global.imageTag=${imageTag}"
                )
            }
        }

        stage('Deploy test') {
            if (params.DEPLOY_TEST) {
                createKeycloakRealmSecret('test')

                helmDeploy(
                        services,
                        'test',
                        'helm/my-bank/values-test.yaml',
                        'envs/runtime/values-secrets-test.yaml',
                        params.IMAGE_REGISTRY,
                        imageTag
                )

                withCredentials([
                        file(
                                credentialsId: 'my-bank-kubeconfig',
                                variable: 'KUBECONFIG'
                        )
                ]) {
                    runCommand('helm test my-bank --namespace test')
                }
            } else {
                echo 'Test deploy skipped by parameter.'
            }
        }

        stage('Manual approval') {
            if (params.DEPLOY_PROD) {
                input(
                        message: 'Deploy my-bank umbrella release to prod?',
                        ok: 'Deploy'
                )
            } else {
                echo 'Production deploy skipped by parameter.'
            }
        }

        stage('Prepare prod secrets') {
            if (params.DEPLOY_PROD) {
                decryptSecrets('prod')
            } else {
                echo 'Production secrets preparation skipped by parameter.'
            }
        }

        stage('Deploy prod') {
            if (params.DEPLOY_PROD) {
                createKeycloakRealmSecret('prod')

                helmDeploy(
                        services,
                        'prod',
                        'helm/my-bank/values-prod.yaml',
                        'envs/runtime/values-secrets-prod.yaml',
                        params.IMAGE_REGISTRY,
                        imageTag
                )
            } else {
                echo 'Production deploy skipped by parameter.'
            }
        }
    } finally {
        stage('Cleanup secrets') {
            echo 'Removing decrypted secrets from Jenkins workspace...'
            cleanupRuntimeSecrets()
        }
    }
}

return this
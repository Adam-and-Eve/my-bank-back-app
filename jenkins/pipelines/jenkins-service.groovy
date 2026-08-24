def runCommand(String linuxCommand, String windowsCommand = null) {
    if (isUnix()) {
        sh linuxCommand
    } else {
        bat(windowsCommand ?: linuxCommand)
    }
}

def gradle(String args) {
    runCommand(
            "chmod +x ./gradlew && " +
                    "./gradlew --no-daemon --console=plain ${args}",
            ".\\gradlew.bat --no-daemon --console=plain ${args}"
    )
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
                        "> envs/runtime/values-secrets-${environment}.yaml",

                "if not exist envs\\runtime mkdir envs\\runtime && " +
                        "sops --decrypt " +
                        "envs\\secrets\\values-secrets-${environment}.enc.yaml " +
                        "> envs\\runtime\\values-secrets-${environment}.yaml"
        )
    }
}

def cleanupRuntimeSecrets() {
    runCommand(
            "rm -f " +
                    "envs/runtime/values-secrets-test.yaml " +
                    "envs/runtime/values-secrets-prod.yaml",

            "if exist envs\\runtime\\values-secrets-test.yaml " +
                    "del /f /q envs\\runtime\\values-secrets-test.yaml " +
                    "& if exist envs\\runtime\\values-secrets-prod.yaml " +
                    "del /f /q envs\\runtime\\values-secrets-prod.yaml"
    )
}

def deployService(
        Map service,
        String namespace,
        String secretsFile,
        String imageRepository,
        String imageTag
) {
    withCredentials([
            file(
                    credentialsId: 'my-bank-kubeconfig',
                    variable: 'KUBECONFIG'
            )
    ]) {
        def command =
                "helm upgrade --install ${service.serviceName} " +
                        "${service.chartPath} " +
                        "--namespace ${namespace} " +
                        "--create-namespace " +
                        "--rollback-on-failure " +
                        "--timeout 5m " +
                        "-f ${service.valuesPath} " +
                        "-f ${secretsFile} " +
                        "--set image.repository=${imageRepository} " +
                        "--set image.tag=${imageTag}"

        runCommand(
                "${command} --dry-run=client",

                "${command} --dry-run=client"
        )

        runCommand(
                command,

                command
        )
    }
}

def runServicePipeline(Map service) {
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
                            name: 'BUILD_IMAGE',
                            defaultValue: false,
                            description: 'Build image using Docker'
                    ),
                    booleanParam(
                            name: 'PUSH_IMAGE',
                            defaultValue: false,
                            description: 'Build and push image to registry'
                    ),
                    booleanParam(
                            name: 'DEPLOY_TEST',
                            defaultValue: false,
                            description: 'Deploy chart to test namespace'
                    ),
                    booleanParam(
                            name: 'DEPLOY_PROD',
                            defaultValue: false,
                            description: 'Deploy chart to prod namespace after manual approval'
                    )
            ])
    ])

    def imageTag = params.IMAGE_TAG?.trim()
            ? params.IMAGE_TAG.trim()
            : env.BUILD_NUMBER

    def registryHost = params.IMAGE_REGISTRY.tokenize('/')[0]

    def imageRepository =
            "${params.IMAGE_REGISTRY}/${service.imageRepository}"

    def image = "${imageRepository}:${imageTag}"

    try {
        stage('Validate') {
            gradle(
                    "${service.gradleModule}:compileJava " +
                            "${service.gradleModule}:processResources"
            )
        }

        stage('Java tests') {
            gradle("${service.gradleModule}:test")

            if (fileExists("${service.serviceName}/src/contractTest")) {
                gradle("${service.gradleModule}:contractTest")
            }
        }

        stage('bootJar') {
            gradle(
                    "${service.gradleModule}:clean " +
                            "${service.gradleModule}:bootJar"
            )
        }

        stage('Docker build') {
            if (params.BUILD_IMAGE || params.PUSH_IMAGE) {
                runCommand(
                        "docker build -t ${image} ${service.serviceName}",

                        "docker build -t ${image} ${service.serviceName}"
                )
            } else {
                echo 'Docker build skipped by parameter.'
            }
        }

        stage('Image push') {
            if (params.PUSH_IMAGE) {
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

                    runCommand(
                            "docker push ${image}",

                            "docker push ${image}"
                    )
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
            if (params.DEPLOY_TEST) {
                runCommand(
                        "helm lint ${service.chartPath} " +
                                "-f ${service.valuesPath} " +
                                "-f envs/runtime/values-secrets-test.yaml",

                        "helm lint ${service.chartPath} " +
                                "-f ${service.valuesPath} " +
                                "-f envs\\runtime\\values-secrets-test.yaml"
                )

                runCommand(
                        "helm template ${service.serviceName} " +
                                "${service.chartPath} " +
                                "--namespace test " +
                                "-f ${service.valuesPath} " +
                                "-f envs/runtime/values-secrets-test.yaml " +
                                "--set image.repository=${imageRepository} " +
                                "--set image.tag=${imageTag}",

                        "helm template ${service.serviceName} " +
                                "${service.chartPath} " +
                                "--namespace test " +
                                "-f ${service.valuesPath} " +
                                "-f envs\\runtime\\values-secrets-test.yaml " +
                                "--set image.repository=${imageRepository} " +
                                "--set image.tag=${imageTag}"
                )
            } else {
                runCommand(
                        "helm lint ${service.chartPath} " +
                                "-f ${service.valuesPath}",

                        "helm lint ${service.chartPath} " +
                                "-f ${service.valuesPath}"
                )

                runCommand(
                        "helm template ${service.serviceName} " +
                                "${service.chartPath} " +
                                "--namespace test " +
                                "-f ${service.valuesPath} " +
                                "--set image.repository=${imageRepository} " +
                                "--set image.tag=${imageTag}",

                        "helm template ${service.serviceName} " +
                                "${service.chartPath} " +
                                "--namespace test " +
                                "-f ${service.valuesPath} " +
                                "--set image.repository=${imageRepository} " +
                                "--set image.tag=${imageTag}"
                )
            }
        }

        stage('Deploy test') {
            if (params.DEPLOY_TEST) {
                deployService(
                        service,
                        'test',
                        'envs/runtime/values-secrets-test.yaml',
                        imageRepository,
                        imageTag
                )
            } else {
                echo 'Test deploy skipped by parameter.'
            }
        }

        stage('Manual approval') {
            if (params.DEPLOY_PROD) {
                input(
                        message: "Deploy ${service.serviceName} to prod?",
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
                deployService(
                        service,
                        'prod',
                        'envs/runtime/values-secrets-prod.yaml',
                        imageRepository,
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
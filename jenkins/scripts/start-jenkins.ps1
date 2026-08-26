$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$JenkinsDir = Resolve-Path (Join-Path $ScriptDir "..")

$JenkinsImage = "my-bank-jenkins:1.0"
$Network = "jenkins"
$KindNetwork = "kind"
$Kubeconfig = Join-Path $ScriptDir "jenkins-kubeconfig.yaml"

Write-Host "Creating Jenkins kubeconfig..."

kind get kubeconfig --name kind |
    ForEach-Object {
        $_ -replace 'server: https://[^:]+:\d+', `
             'server: https://kind-control-plane:6443'
    } |
    Set-Content -Path $Kubeconfig

if ($LASTEXITCODE -ne 0) {
    throw "Failed to create Jenkins kubeconfig."
}

if (-not (Test-Path $Kubeconfig)) {
    throw "Jenkins kubeconfig was not created: $Kubeconfig"
}

Write-Host "Jenkins kubeconfig created: $Kubeconfig"

Write-Host "Creating Docker network..."

docker network inspect $Network *> $null

if ($LASTEXITCODE -ne 0) {
    docker network create $Network

    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create Docker network '$Network'."
    }
}

Write-Host "Starting Jenkins Docker daemon..."

if (docker ps -a --format "{{.Names}}" | Select-String "^jenkins-docker$") {
    docker rm -f jenkins-docker

    if ($LASTEXITCODE -ne 0) {
        throw "Failed to remove existing jenkins-docker container."
    }
}

docker run `
    --name jenkins-docker `
    --detach `
    --privileged `
    --network $Network `
    --network-alias docker `
    --env DOCKER_TLS_CERTDIR=/certs `
    --volume jenkins-docker-certs:/certs/client `
    --volume jenkins-data:/var/jenkins_home `
    --publish 2376:2376 `
    docker:dind `
    --storage-driver overlay2

if ($LASTEXITCODE -ne 0) {
    throw "Failed to start Jenkins Docker daemon."
}

Write-Host "Connecting Jenkins Docker daemon to Kind network..."

docker network connect $KindNetwork jenkins-docker

if ($LASTEXITCODE -ne 0) {
    throw "Failed to connect jenkins-docker to Kind network '$KindNetwork'."
}

Write-Host "Building Jenkins image..."

docker build `
    --tag $JenkinsImage `
    --file (Join-Path $JenkinsDir "Dockerfile") `
    $JenkinsDir

if ($LASTEXITCODE -ne 0) {
    throw "Failed to build Jenkins image."
}

Write-Host "Starting Jenkins..."

if (docker ps -a --format "{{.Names}}" | Select-String "^my-bank-jenkins$") {
    docker rm -f my-bank-jenkins

    if ($LASTEXITCODE -ne 0) {
        throw "Failed to remove existing my-bank-jenkins container."
    }
}

docker run `
    --name my-bank-jenkins `
    --restart=on-failure `
    --detach `
    --network $Network `
    --env DOCKER_HOST=tcp://docker:2376 `
    --env DOCKER_CERT_PATH=/certs/client `
    --env DOCKER_TLS_VERIFY=1 `
    --volume jenkins-data:/var/jenkins_home `
    --volume jenkins-docker-certs:/certs/client:ro `
    --volume "${Kubeconfig}:/var/jenkins_home/.kube/config:ro" `
    --publish 8090:8080 `
    --publish 50000:50000 `
    $JenkinsImage

if ($LASTEXITCODE -ne 0) {
    throw "Failed to start Jenkins."
}

Write-Host "Connecting Jenkins to Kind network..."

docker network connect $KindNetwork my-bank-jenkins

if ($LASTEXITCODE -ne 0) {
    throw "Failed to connect Jenkins to Kind network '$KindNetwork'."
}

Write-Host "Removing temporary Jenkins kubeconfig..."

Remove-Item -Path $Kubeconfig -Force

if (Test-Path $Kubeconfig) {
    throw "Failed to remove temporary Jenkins kubeconfig."
}

Write-Host ""
Write-Host "Jenkins started successfully."
Write-Host "URL: http://localhost:8090"
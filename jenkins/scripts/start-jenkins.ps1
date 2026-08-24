$ErrorActionPreference = "Stop"

$JenkinsImage = "my-bank-jenkins:1.0"
$Network = "jenkins"

Write-Host "Creating Docker network..."
docker network inspect $Network *> $null

if ($LASTEXITCODE -ne 0) {
    docker network create $Network
}

Write-Host "Starting Jenkins Docker daemon..."

docker rm -f jenkins-docker 2>$null

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

Write-Host "Building Jenkins image..."

docker build `
    --tag $JenkinsImage `
    .

Write-Host "Starting Jenkins..."

docker rm -f my-bank-jenkins 2>$null

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
    --publish 8080:8080 `
    --publish 50000:50000 `
    $JenkinsImage

Write-Host ""
Write-Host "Jenkins started successfully."
Write-Host "URL: http://localhost:8080"
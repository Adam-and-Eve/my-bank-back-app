$ErrorActionPreference = "Stop"

Write-Host "Stopping Jenkins..."

docker rm -f my-bank-jenkins 2>$null
docker rm -f jenkins-docker 2>$null

Write-Host "Jenkins stopped."
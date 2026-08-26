$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$KindConfig = Join-Path $ScriptDir "kind-config.yaml"

Write-Host "Creating Kind cluster..."

kind create cluster `
    --name kind `
    --config $KindConfig `
    --wait 60s

if ($LASTEXITCODE -ne 0) {
    throw "Failed to create Kind cluster."
}

Write-Host "Installing Gateway API CRDs..."

kubectl apply --server-side=true `
    -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.5.1/standard-install.yaml

if ($LASTEXITCODE -ne 0) {
    throw "Failed to install Gateway API CRDs."
}

Write-Host "Installing NGINX Gateway Fabric..."

helm upgrade --install ngf `
    oci://ghcr.io/nginx/charts/nginx-gateway-fabric `
    --version 2.6.6 `
    --namespace nginx-gateway `
    --create-namespace `
    --wait

if ($LASTEXITCODE -ne 0) {
    throw "Failed to install NGINX Gateway Fabric."
}

Write-Host "Kind cluster bootstrap completed."
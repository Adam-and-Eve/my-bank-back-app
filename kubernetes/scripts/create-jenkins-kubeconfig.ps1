$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$Kubeconfig = Join-Path $ScriptDir "jenkins-kubeconfig.yaml"

Write-Host "Generating kubeconfig for Jenkins..."

kind get kubeconfig --name kind |
    ForEach-Object {
        $_ -replace 'server: https://[^:]+:\d+', `
            'server: https://kind-control-plane:6443'
    } |
    Set-Content -Path $Kubeconfig -Encoding UTF8


if ($LASTEXITCODE -ne 0) {
    throw "Failed to generate kubeconfig."
}


if (-not (Test-Path $Kubeconfig)) {
    throw "Kubeconfig file was not created."
}


Write-Host ""
Write-Host "Jenkins kubeconfig created:"
Write-Host $Kubeconfig
Write-Host ""

Write-Host "Upload this file to Jenkins credential:"
Write-Host "ID: my-bank-kubeconfig"
Write-Host "Type: Secret file"
Write-Host ""

Read-Host "Press ENTER after uploading kubeconfig to Jenkins"


Write-Host "Removing temporary kubeconfig..."

Remove-Item $Kubeconfig -Force


if (Test-Path $Kubeconfig) {
    throw "Failed to remove kubeconfig."
}


Write-Host "Temporary kubeconfig removed."
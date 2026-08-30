param (
    [Parameter(Mandatory=$false)]
    [string]$ClusterName = "test"
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$Kubeconfig = Join-Path $ScriptDir "jenkins-kubeconfig-${ClusterName}.yaml"
$ControlPlaneNode = "${ClusterName}-control-plane"

Write-Host "Generating kubeconfig for Jenkins (Cluster: $ClusterName)..."

kind get kubeconfig --name $ClusterName |
    ForEach-Object {
        $_ -replace 'server: https://[^:]+:\d+', "server: https://${ControlPlaneNode}:6443"
    } |
    Set-Content -Path $Kubeconfig -Encoding UTF8

if (-not (Test-Path $Kubeconfig)) {
    throw "Kubeconfig file was not created."
}

Write-Host ""
Write-Host "Jenkins kubeconfig created:"
Write-Host $Kubeconfig
Write-Host ""

Write-Host "Upload this file to Jenkins credentials:"
Write-Host "ID: my-bank-kubeconfig-${ClusterName}"
Write-Host "Type: Secret file"
Write-Host ""

Read-Host "Press ENTER after uploading kubeconfig to Jenkins"

Write-Host "Removing temporary kubeconfig..."

Remove-Item $Kubeconfig -Force

if (Test-Path $Kubeconfig) {
    throw "Failed to remove kubeconfig."
}

Write-Host "Temporary kubeconfig removed."
param(
    [string]$AgentPath = "$PSScriptRoot\agents\opentelemetry-javaagent.jar"
)

$ErrorActionPreference = "Stop"

$agentDirectory = Split-Path -Parent $AgentPath
if (-not (Test-Path $agentDirectory)) {
    New-Item -ItemType Directory -Path $agentDirectory | Out-Null
}

$downloadUrl = "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar"
Write-Host "Downloading OpenTelemetry Java agent to $AgentPath"
Invoke-WebRequest -Uri $downloadUrl -OutFile $AgentPath
Write-Host "Downloaded $AgentPath"

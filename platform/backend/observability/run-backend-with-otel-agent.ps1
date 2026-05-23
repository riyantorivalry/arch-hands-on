param(
    [string]$AgentPath = "$PSScriptRoot\agents\opentelemetry-javaagent.jar",
    [string]$ServiceName = "platform-backend",
    [string]$OtlpEndpoint = "http://localhost:4318"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $AgentPath)) {
    & "$PSScriptRoot\download-opentelemetry-javaagent.ps1" -AgentPath $AgentPath
}

$backendDirectory = Resolve-Path "$PSScriptRoot\.."

$env:OTEL_SERVICE_NAME = $ServiceName
$env:OTEL_EXPORTER_OTLP_ENDPOINT = $OtlpEndpoint
$env:OTEL_EXPORTER_OTLP_PROTOCOL = "http/protobuf"
$env:OTEL_TRACES_EXPORTER = "otlp"
$env:OTEL_METRICS_EXPORTER = "otlp"
$env:OTEL_LOGS_EXPORTER = "none"
$env:OTEL_TRACES_SAMPLER = "parentbased_traceidratio"
$env:OTEL_TRACES_SAMPLER_ARG = "1.0"
$env:PYROSCOPE_AGENT_ENABLED = "true"
$env:PYROSCOPE_SERVER_ADDRESS = "http://localhost:4040"
$env:PYROSCOPE_APPLICATION_NAME = $ServiceName

$jvmArguments = "-javaagent:$AgentPath -Dspring.profiles.active=local"

Push-Location $backendDirectory
try {
    mvn spring-boot:run "-Dspring-boot.run.jvmArguments=$jvmArguments"
}
finally {
    Pop-Location
}

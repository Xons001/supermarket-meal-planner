$ErrorActionPreference = "Stop"

function New-E2eSecret([int] $Size = 48, [switch] $UrlSafe) {
    $bytes = New-Object byte[] $Size
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    }
    finally {
        $generator.Dispose()
    }
    $value = [Convert]::ToBase64String($bytes)
    if ($UrlSafe) { return $value.Replace('+', '-').Replace('/', '_') }
    return $value
}

$env:MEAL_PLAN_PREVIEW_HMAC_SECRET = New-E2eSecret
$env:APP_AUTH_ACCESS_TOKEN_SECRET = New-E2eSecret
$env:APP_AUTH_REFRESH_TOKEN_SECRET = New-E2eSecret
$env:AIRFLOW_FERNET_KEY = New-E2eSecret -Size 32 -UrlSafe
$env:AIRFLOW_API_JWT_SECRET = New-E2eSecret
$env:AIRFLOW_WEBSERVER_SECRET_KEY = New-E2eSecret
$env:AIRFLOW_METADATA_DB_PASSWORD = New-E2eSecret
$env:AIRFLOW_ADMIN_PASSWORD = New-E2eSecret
$env:AIRFLOW_ADMIN_USERNAME = "e2e-admin"
$env:APP_ADMIN_ENABLED = "true"
$env:APP_ADMIN_EMAIL = "e2e-admin@example.test"
$env:APP_ADMIN_PASSWORD = New-E2eSecret
$projectName = "supermarket-meal-planner-e2e"
$e2eExitCode = 0

try {
    docker compose `
        --project-name $projectName `
        -f docker-compose.yml `
        -f docker-compose.e2e.yml `
        --profile e2e `
        up --build -d postgres airflow-postgres
    if ($LASTEXITCODE -ne 0) { throw "No se pudieron iniciar las bases E2E" }
    docker compose `
        --project-name $projectName `
        -f docker-compose.yml `
        -f docker-compose.e2e.yml `
        --profile e2e `
        up --build airflow-init
    if ($LASTEXITCODE -ne 0) { throw "No se pudo inicializar Airflow E2E" }
    docker compose `
        --project-name $projectName `
        -f docker-compose.yml `
        -f docker-compose.e2e.yml `
        --profile e2e `
        up --no-deps --build --abort-on-container-exit --exit-code-from e2e `
        airflow-apiserver airflow-scheduler airflow-dag-processor airflow-triggerer backend frontend e2e
    $e2eExitCode = $LASTEXITCODE
}
catch {
    Write-Error $_
    $e2eExitCode = 1
}
finally {
    docker compose `
        --project-name $projectName `
        -f docker-compose.yml `
        -f docker-compose.e2e.yml `
        --profile e2e `
        down --volumes --remove-orphans
    $cleanupExitCode = $LASTEXITCODE
    if ($e2eExitCode -eq 0 -and $cleanupExitCode -ne 0) {
        $e2eExitCode = $cleanupExitCode
    }
}

exit $e2eExitCode

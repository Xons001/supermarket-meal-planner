$ErrorActionPreference = "Stop"

function New-E2eSecret {
    $bytes = New-Object byte[] 48
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    }
    finally {
        $generator.Dispose()
    }
    return [Convert]::ToBase64String($bytes)
}

$env:MEAL_PLAN_PREVIEW_HMAC_SECRET = New-E2eSecret
$env:APP_AUTH_ACCESS_TOKEN_SECRET = New-E2eSecret
$env:APP_AUTH_REFRESH_TOKEN_SECRET = New-E2eSecret
$projectName = "supermarket-meal-planner-e2e"
$e2eExitCode = 0

try {
    docker compose `
        --project-name $projectName `
        -f docker-compose.yml `
        -f docker-compose.e2e.yml `
        --profile e2e `
        up --build --abort-on-container-exit --exit-code-from e2e
    $e2eExitCode = $LASTEXITCODE
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

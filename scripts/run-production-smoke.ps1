$ErrorActionPreference = 'Stop'
function New-Secret([int]$Size=48, [switch]$UrlSafe) {
  $bytes=New-Object byte[] $Size
  $generator=[Security.Cryptography.RandomNumberGenerator]::Create()
  try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
  $value=[Convert]::ToBase64String($bytes)
  if ($UrlSafe) { $value=$value.Replace('+','-').Replace('/','_') }
  return $value
}
$certificateDir=Join-Path (Get-Location) 'certificates/prod-e2e'
New-Item -ItemType Directory -Force $certificateDir | Out-Null
docker run --rm -v "${certificateDir}:/tls" alpine/openssl req -x509 -newkey rsa:2048 -nodes -days 1 -subj '/CN=localhost' -keyout /tls/privkey.pem -out /tls/fullchain.pem
if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar el certificado autofirmado' }
docker run --rm -v "${certificateDir}:/tls" alpine chmod 644 /tls/privkey.pem /tls/fullchain.pem
if ($LASTEXITCODE -ne 0) { throw 'No se pudieron preparar los permisos del certificado' }
$env:TLS_CERT_PATH=(Join-Path $certificateDir 'fullchain.pem')
$env:TLS_KEY_PATH=(Join-Path $certificateDir 'privkey.pem')
$env:POSTGRES_PASSWORD=New-Secret
$env:AIRFLOW_METADATA_DB_PASSWORD=New-Secret -UrlSafe
$env:AIRFLOW_FERNET_KEY=New-Secret -Size 32 -UrlSafe
$env:AIRFLOW_API_JWT_SECRET=New-Secret
$env:AIRFLOW_WEBSERVER_SECRET_KEY=New-Secret
$env:AIRFLOW_ADMIN_USERNAME='smoke-operator'
$env:AIRFLOW_ADMIN_PASSWORD=New-Secret
$env:APP_AUTH_ACCESS_TOKEN_SECRET=New-Secret
$env:APP_AUTH_REFRESH_TOKEN_SECRET=New-Secret
$env:MEAL_PLAN_PREVIEW_HMAC_SECRET=New-Secret
$env:APP_OBSERVABILITY_USER_HASH_SECRET=New-Secret
$env:APP_AUTH_ISSUER='https://localhost'
$env:APP_AUTH_ALLOWED_ORIGINS='https://localhost'
$env:PUBLIC_BASE_URL='https://localhost'
$project='supermarket-meal-planner-prod-e2e'
$env:COMPOSE_PROJECT_NAME=$project
try {
  docker compose --project-name $project -f docker-compose.prod.yml -f docker-compose.prod-e2e.yml up --build --abort-on-container-exit --exit-code-from smoke smoke
  if ($LASTEXITCODE -ne 0) { throw 'El smoke productivo falló' }
} finally {
  docker compose --project-name $project -f docker-compose.prod.yml -f docker-compose.prod-e2e.yml down --volumes --remove-orphans
}

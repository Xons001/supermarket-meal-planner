param([ValidateSet('app','airflow','both')][string]$Target='both', [string]$BackupDir='backups', [int]$RetentionDays=14)
$ErrorActionPreference = 'Stop'
$stamp = (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssZ')
New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null
function Backup-Service($service, $database, $user, $label) {
  $file = "smp-$label-$stamp.dump"
  docker compose exec -T $service sh -ec "PGPASSWORD=`"`$POSTGRES_PASSWORD`" pg_dump -U `"$user`" -d `"$database`" --format=custom --file=/tmp/$file"
  if ($LASTEXITCODE -ne 0) { throw "pg_dump falló para $label" }
  $container = docker compose ps -q $service
  docker cp "${container}:/tmp/$file" (Join-Path $BackupDir $file)
  docker compose exec -T $service rm -f "/tmp/$file"
  $hash = (Get-FileHash (Join-Path $BackupDir $file) -Algorithm SHA256).Hash.ToLowerInvariant()
  "$hash  $file" | Set-Content -Encoding ascii (Join-Path $BackupDir "$file.sha256")
  @{database=$label;createdAt=$stamp;format='postgres-custom';checksum=$hash} | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $BackupDir "$file.manifest.json")
}
$appDb = if ($env:POSTGRES_DB) {$env:POSTGRES_DB} else {'meal_planner'}
$appUser = if ($env:POSTGRES_USER) {$env:POSTGRES_USER} else {'meal_planner'}
if ($Target -in @('app','both')) { Backup-Service postgres $appDb $appUser app }
if ($Target -in @('airflow','both')) { Backup-Service airflow-postgres airflow airflow airflow }
Get-ChildItem -LiteralPath $BackupDir -File | Where-Object {$_.Name -like 'smp-*' -and $_.LastWriteTimeUtc -lt (Get-Date).ToUniversalTime().AddDays(-$RetentionDays)} | Remove-Item

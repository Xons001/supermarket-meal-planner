param([Parameter(Mandatory)][string]$Dump, [Parameter(Mandatory)][string]$Destination, [string]$Service='postgres', [switch]$AllowProductionRestore)
$ErrorActionPreference = 'Stop'
if ($Destination -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') { throw 'Nombre de base no válido' }
$main = if ($env:POSTGRES_DB) {$env:POSTGRES_DB} else {'meal_planner'}
if ($Service -eq 'postgres' -and $Destination -eq $main -and -not $AllowProductionRestore) { throw 'Destino principal rechazado. Usa -AllowProductionRestore conscientemente.' }
& "$PSScriptRoot/verify-backup.ps1" -Dump $Dump -Service $Service
if ($Service -eq 'postgres' -and $Destination -eq $main) { & "$PSScriptRoot/backup.ps1" -Target app }
$container = docker compose ps -q $Service
$temp = "/tmp/$([IO.Path]::GetFileName($Dump))"
docker cp $Dump "${container}:$temp"
docker compose exec -T $Service sh -ec "PGPASSWORD=`"`$POSTGRES_PASSWORD`" createdb -U `"`$POSTGRES_USER`" '$Destination' 2>/dev/null || true; PGPASSWORD=`"`$POSTGRES_PASSWORD`" pg_restore -U `"`$POSTGRES_USER`" -d '$Destination' --clean --if-exists --no-owner '$temp'"
docker compose exec -T $Service rm -f $temp
if ($LASTEXITCODE -ne 0) { throw 'La restauración falló' }
Write-Host "Restauración completada en $Destination"

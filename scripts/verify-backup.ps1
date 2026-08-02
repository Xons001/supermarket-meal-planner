param([Parameter(Mandatory)][string]$Dump, [string]$Service='postgres')
$ErrorActionPreference = 'Stop'
$expected = (Get-Content "$Dump.sha256").Split(' ')[0].Trim()
$actual = (Get-FileHash $Dump -Algorithm SHA256).Hash
if ($expected -ne $actual) { throw 'El checksum SHA-256 no coincide' }
$container = docker compose ps -q $Service
$temp = "/tmp/$([IO.Path]::GetFileName($Dump))"
docker cp $Dump "${container}:$temp"
docker compose exec -T $Service pg_restore --list $temp | Out-Null
docker compose exec -T $Service rm -f $temp
if ($LASTEXITCODE -ne 0) { throw 'pg_restore no reconoce el backup' }
Write-Host "Backup verificado: $Dump"

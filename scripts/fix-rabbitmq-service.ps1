# fix-rabbitmq-service.ps1
# Repair or disable the broken RabbitMQ Windows service registration.
#
# Run in an ELEVATED PowerShell (Run as Administrator):
#   Set-ExecutionPolicy -Scope Process Bypass
#   .\fix-rabbitmq-service.ps1 -Action Disable     # recommended
#   .\fix-rabbitmq-service.ps1 -Action Reinstall   # optional
#
# ---------------------------------------------------------------
# Why the service is broken
# ---------------------------------------------------------------
# The registration lives in:
#   HKLM:\SOFTWARE\Ericsson\Erlang\ErlSrv\1.1\RabbitMQ
# On this machine its "Args" value is EMPTY and "Env" holds a single
# empty entry, i.e. the service was never registered with the
# arguments rabbitmq-service.bat normally passes. A healthy
# registration looks roughly like:
#   Args = -env RABBITMQ_NODENAME="rabbit@localhost" -sname rabbit@localhost ...
# With empty Args, erlsrv starts a bare Erlang VM that dies at once
# (Event ID 17: "Erlang machine stopped instantly").
#
# Additionally the host name of this machine is non-ASCII. Erlang
# derives a short node name from the host name, so even a correct
# registration would fail unless RABBITMQ_NODENAME is pinned to
# rabbit@localhost.
#
# ---------------------------------------------------------------
# Which action to choose
# ---------------------------------------------------------------
# Disable    Stop the service from auto-starting at boot, and rely on
#            console mode started by start-infra.ps1. Recommended,
#            because:
#              - the node keeps ONE data directory
#                (%APPDATA%\RabbitMQ\db\rabbit@localhost), shared by
#                the server and by rabbitmqctl;
#              - the service would run as LocalSystem, whose %APPDATA%
#                is C:\Windows\System32\config\systemprofile, so it
#                would use a DIFFERENT, empty data directory and the
#                two modes would silently see different queues.
# Reinstall  Re-register the service with the node name pinned. Only
#            pick this if you want RabbitMQ up before you log in.
#            It still runs as LocalSystem, so expect the separate
#            data directory described above.

param(
    [ValidateSet('Disable', 'Reinstall')]
    [string]$Action = 'Disable'
)

$ErrorActionPreference = 'Stop'

$principal = New-Object Security.Principal.WindowsPrincipal(
    [Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw 'This script must run in an elevated PowerShell (Run as Administrator).'
}

$Sbin    = 'D:\RabbitMQ\app\rabbitmq_server-4.4.0\sbin'
$Node    = 'rabbit@localhost'
$SvcKey  = 'HKLM:\SOFTWARE\Ericsson\Erlang\ErlSrv\1.1\RabbitMQ'

function Test-Port {
    param([int]$Port)
    return ($null -ne (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue))
}

function Wait-Port {
    param([int]$Port, [int]$TimeoutSec = 60)
    $sw = [Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $TimeoutSec) {
        if (Test-Port -Port $Port) { return $true }
        Start-Sleep -Milliseconds 500
    }
    return $false
}

Write-Host ("Action: {0}" -f $Action) -ForegroundColor Cyan
Write-Host ''

if ($Action -eq 'Disable') {
    $svc = Get-Service -Name RabbitMQ -ErrorAction SilentlyContinue
    if ($null -eq $svc) {
        Write-Host '[SKIP] RabbitMQ service not present'
        return
    }

    if ($svc.Status -eq 'Running') {
        Write-Host '[....] stopping RabbitMQ service'
        Stop-Service -Name RabbitMQ -Force
        Start-Sleep -Seconds 2
    }

    Write-Host '[....] setting startup type to Disabled'
    Set-Service -Name RabbitMQ -StartupType Disabled

    $svc = Get-Service -Name RabbitMQ
    Write-Host ("[OK]   service {0}: status={1} startup={2}" -f $svc.Name, $svc.Status, $svc.StartType) -ForegroundColor Green
    Write-Host ''
    Write-Host 'RabbitMQ will no longer try to start at boot. Use start-infra.ps1 instead;'
    Write-Host 'the node name is pinned by %APPDATA%\RabbitMQ\rabbitmq-env-conf.bat.'
    return
}

# ---------------- Reinstall ----------------
if (Test-Port -Port 5672) {
    Write-Host '[....] stopping the running node first (console mode is holding 5672)'
    & (Join-Path $Sbin 'rabbitmqctl.bat') shutdown 2>&1 | Select-Object -Last 2
    $sw = [Diagnostics]::Stopwatch::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt 40 -and (Test-Port -Port 5672)) { Start-Sleep -Milliseconds 500 }
}

Write-Host '[1/4] removing the existing (broken) service registration'
& (Join-Path $Sbin 'rabbitmq-service.bat') remove 2>&1 | Select-Object -Last 3

Write-Host '[2/4] re-registering with the node name pinned'
$env:RABBITMQ_NODENAME = $Node
& (Join-Path $Sbin 'rabbitmq-service.bat') install 2>&1 | Select-Object -Last 3

Write-Host '[3/4] first start (this initialises the data directory, may take ~30s)'
Start-Service -Name RabbitMQ
$ok = Wait-Port -Port 5672 -TimeoutSec 120

Write-Host '[4/4] verifying the registration and the node identity'
if (Test-Path $SvcKey) {
    $reg = Get-ItemProperty $SvcKey
    Write-Host ("      Args = {0}" -f $reg.Args)
    Write-Host ("      Env  = {0}" -f ($reg.Env -join '; '))
}
if ($ok) {
    & (Join-Path $Sbin 'rabbitmqctl.bat') status 2>&1 |
        Select-String -Pattern 'Status of node|Uptime|RabbitMQ version' |
        ForEach-Object { Write-Host ("      " + $_.Line) }
    Write-Host '[OK]   service is running and the node name is correct' -ForegroundColor Green
    Write-Host ''
    Write-Host 'Reminder: the service runs as LocalSystem and uses its own data directory.'
    Write-Host 'Queues created in console mode will NOT be visible to the service.'
} else {
    Write-Host '[FAIL] 5672 not listening; inspect %APPDATA%\RabbitMQ\log and the System event log' -ForegroundColor Red
    Write-Host ("       current startup type: {0}" -f (Get-Service RabbitMQ).StartType)
    throw 'Service reinstall did not bring RabbitMQ up.'
}

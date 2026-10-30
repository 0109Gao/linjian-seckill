# start-infra.ps1
# Start local middleware for linjian-seckill (MySQL / Redis / RabbitMQ).
# Run in PowerShell:
#   Set-ExecutionPolicy -Scope Process Bypass; .\start-infra.ps1
# Elevation is only needed if you choose to start RabbitMQ as a Windows service.
#
# Why this script exists:
# MySQL and Redis are extracted (portable) builds on D: and are NOT registered
# as Windows services, so they do not start after a reboot. MySQL must be
# started with its own my.ini, otherwise it will look for a datadir on C:.
#
# NOTE: keep every comment in this file ASCII-only. PowerShell 5.1 reads a
# .ps1 without a BOM as ANSI, so non-ASCII characters can shift and corrupt
# the following line.

$ErrorActionPreference = 'Stop'

$MyBase    = 'D:\mysql\app\mysql-26.10.0-winx64'
$MyExe     = Join-Path $MyBase 'bin\mysqld.exe'
$MyIni     = Join-Path $MyBase 'my.ini'
$RedisDir  = 'D:\Redis\app'
$RedisExe  = Join-Path $RedisDir 'redis-server.exe'
$RedisConf = Join-Path $RedisDir 'redis.windows.conf'
$RabbitSbin = 'D:\RabbitMQ\app\rabbitmq_server-4.4.0\sbin'

function Test-Port {
    param([int]$Port)
    $conn = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    return ($null -ne $conn)
}

function Wait-Port {
    param([int]$Port, [int]$TimeoutSec = 30, [string]$Name)
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $TimeoutSec) {
        if (Test-Port -Port $Port) {
            Write-Host "[OK]   $Name is listening on port $Port" -ForegroundColor Green
            return $true
        }
        Start-Sleep -Milliseconds 500
    }
    Write-Host "[FAIL] $Name did not listen on port $Port within $TimeoutSec s" -ForegroundColor Red
    return $false
}

# ---------- MySQL (3306) ----------
if (Test-Port -Port 3306) {
    Write-Host "[SKIP] MySQL already running on 3306"
} else {
    if (-not (Test-Path $MyExe)) { throw "mysqld.exe not found: $MyExe" }
    if (-not (Test-Path $MyIni)) { throw "my.ini not found: $MyIni" }
    Write-Host "[....] starting MySQL"
    Start-Process -FilePath $MyExe -ArgumentList "--defaults-file=$MyIni" -WindowStyle Hidden
    if (-not (Wait-Port -Port 3306 -Name 'MySQL')) { throw 'MySQL failed to start, check data\*.err' }
}

# ---------- Redis (6379) ----------
if (Test-Port -Port 6379) {
    Write-Host "[SKIP] Redis already running on 6379"
} else {
    if (-not (Test-Path $RedisExe)) { throw "redis-server.exe not found: $RedisExe" }
    Write-Host "[....] starting Redis"
    Start-Process -FilePath $RedisExe -ArgumentList "`"$RedisConf`"" -WorkingDirectory $RedisDir -WindowStyle Hidden
    if (-not (Wait-Port -Port 6379 -Name 'Redis')) { throw 'Redis failed to start' }
}

# ---------- RabbitMQ (5672 + 15672) ----------
# Known problem on this machine: the host name is non-ASCII, and Erlang builds
# a short node name from the host name, so net_kernel refuses to start and the
# VM dies before writing a log line (System event log, ID 17:
# "Erlang machine stopped instantly").
#
# Permanent fix (no elevation needed, already applied):
#   %APPDATA%\RabbitMQ\rabbitmq-env-conf.bat sets RABBITMQ_NODENAME=rabbit@localhost
#   That file is loaded by rabbitmq-env.bat for every CLI and for console mode.
#
# The Windows service is a separate matter: its registration is incomplete on
# this box (Args empty), so it cannot start RabbitMQ at all. Do not silently
# rely on it. Either disable it (recommended) or reinstall it with
#   .\fix-rabbitmq-service.ps1 -Action Disable|Reinstall
# run in an elevated shell.
if (Test-Port -Port 5672) {
    Write-Host '[SKIP] RabbitMQ already running on 5672'
} else {
    $svc = Get-Service -Name RabbitMQ -ErrorAction SilentlyContinue
    $started = $false

    # Only touch the service when it is enabled; a Disabled service is the
    # expected state after running fix-rabbitmq-service.ps1 -Action Disable.
    if ($null -ne $svc -and $svc.StartType -ne 'Disabled') {
        Write-Host '[....] trying RabbitMQ Windows service'
        try {
            Start-Service -Name RabbitMQ -ErrorAction Stop
            $started = Wait-Port -Port 5672 -Name 'RabbitMQ (service)' -TimeoutSec 20
        } catch {
            Write-Host ("      service start failed: " + $_.Exception.Message) -ForegroundColor Yellow
        }
    } elseif ($null -ne $svc) {
        Write-Host '[INFO] RabbitMQ service is disabled, using console mode'
    }

    if (-not $started) {
        $serverBat = Join-Path $RabbitSbin 'rabbitmq-server.bat'
        if (-not (Test-Path $serverBat)) {
            Write-Host "[WARN] rabbitmq-server.bat not found under $RabbitSbin" -ForegroundColor Yellow
        } else {
            $envFile = Join-Path $env:APPDATA 'RabbitMQ\rabbitmq-env-conf.bat'
            if (-not (Test-Path $envFile)) {
                Write-Host "[WARN] missing $envFile - the node name will fall back to the" -ForegroundColor Yellow
                Write-Host '       non-ASCII host name and RabbitMQ will refuse to start.' -ForegroundColor Yellow
            }
            Write-Host '[....] starting RabbitMQ in console mode'
            Start-Process -FilePath $serverBat -WorkingDirectory $RabbitSbin -WindowStyle Hidden
            $started = Wait-Port -Port 5672 -Name 'RabbitMQ (console)' -TimeoutSec 90
        }
    }

    if ($started) {
        if (-not (Wait-Port -Port 15672 -Name 'RabbitMQ management' -TimeoutSec 20)) {
            Write-Host '      management plugin not listening, run: rabbitmq-plugins enable rabbitmq_management' -ForegroundColor Yellow
        }
    } else {
        Write-Host '[WARN] RabbitMQ not listening. See %APPDATA%\RabbitMQ\log for details.' -ForegroundColor Yellow
    }
}

Write-Host ''
Write-Host 'Infrastructure status:' -ForegroundColor Cyan
'3306','6379','5672','15672' | ForEach-Object {
    $state = if (Test-Port -Port $_) { 'UP' } else { 'DOWN' }
    Write-Host ("  port {0,-6} {1}" -f $_, $state)
}

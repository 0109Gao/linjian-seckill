# stop-infra.ps1
# Stop local middleware for linjian-seckill.
# Run in PowerShell:
#   Set-ExecutionPolicy -Scope Process Bypass; .\stop-infra.ps1
#
# IMPORTANT: MySQL is stopped with mysqladmin shutdown (a clean shutdown).
# Do NOT kill mysqld.exe with Stop-Process: InnoDB needs to flush its redo log,
# and a hard kill can leave the data dictionary in a recovery-required state.
#
# Same idea for RabbitMQ: it is stopped with "rabbitmqctl shutdown", which lets
# the broker flush queues and close cleanly. Never Stop-Process erl.exe.

$ErrorActionPreference = 'Stop'

# MySQL root password: prefer environment variable, fall back to local dev default.
$MyPassword = if ($env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { '123456789' }

$MyBase    = 'D:\mysql\app\mysql-26.10.0-winx64'
$MyAdmin   = Join-Path $MyBase 'bin\mysqladmin.exe'
$RedisDir  = 'D:\Redis\app'
$RedisCli  = Join-Path $RedisDir 'redis-cli.exe'
$RabbitSbin = 'D:\RabbitMQ\app\rabbitmq_server-4.4.0\sbin'

function Test-Port {
    param([int]$Port)
    $conn = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    return ($null -ne $conn)
}

# ---------- MySQL ----------
if (Test-Port -Port 3306) {
    Write-Host '[....] shutting down MySQL'
    & $MyAdmin -u root "-p$MyPassword" shutdown 2>$null
    Start-Sleep -Seconds 2
    if (Test-Port -Port 3306) {
        Write-Host '[WARN] MySQL still listening on 3306, check manually' -ForegroundColor Yellow
    } else {
        Write-Host '[OK]   MySQL stopped'
    }
} else {
    Write-Host '[SKIP] MySQL not running'
}

# ---------- Redis ----------
if (Test-Port -Port 6379) {
    Write-Host '[....] shutting down Redis'
    & $RedisCli -p 6379 shutdown nosave 2>$null
    Start-Sleep -Seconds 1
    if (Test-Port -Port 6379) {
        Write-Host '[WARN] Redis still listening on 6379, check manually' -ForegroundColor Yellow
    } else {
        Write-Host '[OK]   Redis stopped'
    }
} else {
    Write-Host '[SKIP] Redis not running'
}

# ---------- RabbitMQ ----------
# Covers both modes: the service (if enabled) and console mode (the normal
# setup on this machine). rabbitmqctl knows the node name through
# %APPDATA%\RabbitMQ\rabbitmq-env-conf.bat, so no -n flag is required.
$svc = Get-Service -Name RabbitMQ -ErrorAction SilentlyContinue
if ($null -ne $svc -and $svc.Status -eq 'Running') {
    Write-Host '[....] stopping RabbitMQ service'
    Stop-Service -Name RabbitMQ
    Start-Sleep -Seconds 2
    if (Test-Port -Port 5672) {
        Write-Host '[WARN] 5672 still listening after service stop' -ForegroundColor Yellow
    } else {
        Write-Host '[OK]   RabbitMQ service stopped'
    }
} elseif (Test-Port -Port 5672) {
    Write-Host '[....] shutting down RabbitMQ console node'
    & (Join-Path $RabbitSbin 'rabbitmqctl.bat') shutdown 2>&1 | Select-Object -Last 2
    $sw = [Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt 40 -and (Test-Port -Port 5672)) {
        Start-Sleep -Milliseconds 500
    }
    if (Test-Port -Port 5672) {
        Write-Host '[WARN] RabbitMQ still listening on 5672, check manually' -ForegroundColor Yellow
    } else {
        Write-Host '[OK]   RabbitMQ stopped'
    }
} else {
    Write-Host '[SKIP] RabbitMQ not running'
}

Write-Host ''
Write-Host 'Infrastructure status:' -ForegroundColor Cyan
'3306','6379','5672','15672' | ForEach-Object {
    $state = if (Test-Port -Port $_) { 'UP' } else { 'DOWN' }
    Write-Host ("  port {0,-6} {1}" -f $_, $state)
}

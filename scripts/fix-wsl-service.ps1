# Restart WslService and re-verify.
# Run as Administrator. Pure ASCII (PowerShell 5.1 ANSI decode safety).

$ErrorActionPreference = 'Continue'

$Wsl        = 'C:\Program Files\WSL\wsl.exe'
$Result     = 'D:\WSL2\fix-result.txt'
$OutDir     = 'D:\WSL2\fix'
$TimeoutSec = 25

$lines = New-Object System.Collections.Generic.List[string]
function Add-Line { param([string]$Text = '') ; $lines.Add($Text) }

# ---- admin gate -------------------------------------------------------------
$id = [Security.Principal.WindowsIdentity]::GetCurrent()
$pr = New-Object Security.Principal.WindowsPrincipal($id)
if (-not $pr.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host ''
    Write-Host 'ERROR: this script must run as Administrator.' -ForegroundColor Red
    Write-Host 'Right-click PowerShell -> Run as administrator, then run it again.' -ForegroundColor Yellow
    Write-Host ''
    exit 1
}

New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
New-Item -ItemType Directory -Path (Split-Path $Result) -Force | Out-Null

Add-Line ('=' * 66)
Add-Line ('WSL service restart  ' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
Add-Line ('=' * 66)

# ---- step 1: kill stale clients --------------------------------------------
Write-Host ''
Write-Host '[1/5] killing stale wsl client processes...' -ForegroundColor Cyan
Add-Line ''
Add-Line '--- step 1: stale clients ---'
foreach ($n in 'wsl', 'wslhost', 'wslg', 'wslsettings', 'wslconfig') {
    Get-Process -Name $n -ErrorAction SilentlyContinue | ForEach-Object {
        try {
            $null = $_.Kill()
            Write-Host ('  killed ' + $n + ' pid=' + $_.Id)
            Add-Line ('  killed ' + $n + ' pid=' + $_.Id)
        } catch {
            Write-Host ('  could not kill ' + $n + ' pid=' + $_.Id) -ForegroundColor Yellow
            Add-Line ('  could not kill ' + $n + ' pid=' + $_.Id)
        }
    }
}

# ---- step 2: stop service --------------------------------------------------
Write-Host ''
Write-Host '[2/5] stopping WslService...' -ForegroundColor Cyan
Add-Line ''
Add-Line '--- step 2: stop service ---'
try {
    Stop-Service -Name 'WslService' -Force -ErrorAction Stop
    Write-Host '  stop issued'
} catch {
    Write-Host ('  stop failed: ' + $_.Exception.Message) -ForegroundColor Yellow
    Add-Line ('  stop failed: ' + $_.Exception.Message)
}
Start-Sleep -Seconds 3
$svc = Get-Service -Name 'WslService' -ErrorAction SilentlyContinue
$state = if ($svc) { $svc.Status } else { 'gone' }
Write-Host ('  state = ' + $state)
Add-Line ('  state after stop = ' + $state)

# The VM process can survive a service stop; kill it explicitly.
Get-Process -Name 'vmmemWSL' -ErrorAction SilentlyContinue | ForEach-Object {
    try { $null = $_.Kill(); Write-Host '  killed vmmemWSL'; Add-Line '  killed vmmemWSL' }
    catch { Write-Host '  vmmemWSL still alive (protected)'; Add-Line '  vmmemWSL still alive (protected)' }
}

# ---- step 3: start service -------------------------------------------------
Write-Host ''
Write-Host '[3/5] starting WslService...' -ForegroundColor Cyan
Add-Line ''
Add-Line '--- step 3: start service ---'
try {
    Start-Service -Name 'WslService' -ErrorAction Stop
    Write-Host '  started'
} catch {
    Write-Host ('  start failed: ' + $_.Exception.Message) -ForegroundColor Yellow
    Add-Line ('  start failed: ' + $_.Exception.Message)
}
Start-Sleep -Seconds 5
$svc = Get-Service -Name 'WslService' -ErrorAction SilentlyContinue
$state = if ($svc) { $svc.Status } else { 'gone' }
$spid  = if ($svc) { (Get-CimInstance Win32_Service -Filter "Name='WslService'").ProcessId } else { 0 }
Write-Host ('  state = ' + $state + '  pid=' + $spid)
Add-Line ('  state after start = ' + $state + '  pid=' + $spid)

# ---- step 4: re-probe ------------------------------------------------------
function Invoke-Probe {
    param([string]$Label, [string[]]$WslArgs, [int]$Wait)

    Write-Host ('  probing ' + $Label + ' ...') -ForegroundColor Gray
    Add-Line ''
    Add-Line ('--- probe: ' + $Label + ' (args: ' + ($WslArgs -join ' ') + ') ---')

    $oFile = Join-Path $OutDir ('o_' + $Label + '.txt')
    $eFile = Join-Path $OutDir ('e_' + $Label + '.txt')
    Remove-Item $oFile, $eFile -ErrorAction SilentlyContinue

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $p = Start-Process -FilePath $Wsl -ArgumentList $WslArgs -NoNewWindow -PassThru `
                           -RedirectStandardOutput $oFile -RedirectStandardError $eFile
    } catch {
        Write-Host ('    START FAILED: ' + $_.Exception.Message) -ForegroundColor Red
        Add-Line ('  START FAILED: ' + $_.Exception.Message)
        return $false
    }

    $ok = $p.WaitForExit($Wait * 1000)
    $sw.Stop()

    if (-not $ok) {
        try { $p.Kill() } catch { }
        Write-Host ('    TIMEOUT ' + $Wait + 's') -ForegroundColor Red
        Add-Line ('  RESULT: TIMEOUT after ' + $Wait + 's')
        return $false
    }

    Write-Host ('    exit=' + $p.ExitCode + '  ' + [math]::Round($sw.Elapsed.TotalSeconds, 1) + 's') -ForegroundColor Green
    Add-Line ('  RESULT: exit=' + $p.ExitCode + '  elapsed=' + [math]::Round($sw.Elapsed.TotalSeconds, 1) + 's')

    foreach ($pair in @(@('stdout', $oFile), @('stderr', $eFile))) {
        if (Test-Path $pair[1]) {
            $t = Get-Content $pair[1] -Raw -ErrorAction SilentlyContinue
            if ($t -and $t.Trim().Length -gt 0) {
                $t = $t.Trim()
                if ($t.Length -gt 1500) { $t = $t.Substring(0, 1500) + ' ...' }
                Add-Line ('  ' + $pair[0] + ':')
                foreach ($ln in ($t -split "`r?`n")) { Add-Line ('    | ' + $ln) }
                Write-Host ('    ' + ($t -split "`r?`n")[0])
            }
        }
    }
    return ($p.ExitCode -eq 0)
}

Write-Host ''
Write-Host '[4/5] re-probing after restart...' -ForegroundColor Cyan
Add-Line ''
Add-Line '--- step 4: re-probe ---'

$rStatus = Invoke-Probe -Label 'status' -WslArgs @('--status') -Wait $TimeoutSec
$rList   = Invoke-Probe -Label 'list'   -WslArgs @('--list', '--verbose') -Wait $TimeoutSec
$rRoot   = Invoke-Probe -Label 'root'   -WslArgs @('-d', 'Ubuntu', '-u', 'root', '--', 'echo', 'WSL_ROOT_OK') -Wait $TimeoutSec

# ---- step 5: verdict -------------------------------------------------------
Write-Host ''
Write-Host '[5/5] verdict' -ForegroundColor Cyan
Add-Line ''
Add-Line '--- step 5: verdict ---'

if ($rStatus -and $rRoot) {
    Add-Line '  VERDICT: FIXED. Service responds and distro shell works.'
    Write-Host '  FIXED - service is healthy again.' -ForegroundColor Green
    Write-Host '  Next: run  wsl -d Ubuntu  to finish the user setup.' -ForegroundColor Green
} elseif ($rStatus) {
    Add-Line '  VERDICT: PARTIAL. Service responds but shell entry fails.'
    Write-Host '  PARTIAL - service ok, distro shell still stuck.' -ForegroundColor Yellow
} else {
    Add-Line '  VERDICT: STILL STUCK. Restart did not help -> likely version incompatibility.'
    Write-Host '  STILL STUCK - restart did not help.' -ForegroundColor Red
    Write-Host '  This points to a WSL 3.0.1 compatibility issue on this Windows build.' -ForegroundColor Red
}

$lines | Set-Content -Path $Result -Encoding UTF8
Write-Host ''
Write-Host ('Report: ' + $Result) -ForegroundColor Cyan
Write-Host 'Tell me it finished; I will read the file.' -ForegroundColor Cyan
Write-Host ''

# WSL diagnostic probe.
# Purpose: determine which layer is stuck (client / service / VM / distro).
# Writes everything to D:\WSL2\diag-result.txt so it can be reviewed offline.
# NOTE: keep this file pure ASCII. PowerShell 5.1 decodes BOM-less files as ANSI;
# a Chinese comment once swallowed a newline and broke an argument array.

$ErrorActionPreference = 'Continue'

$Wsl      = 'C:\Program Files\WSL\wsl.exe'
$OutDir   = 'D:\WSL2\diag'
$Result   = 'D:\WSL2\diag-result.txt'
$TimeoutSec = 20

New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
New-Item -ItemType Directory -Path (Split-Path $Result) -Force | Out-Null

$lines = New-Object System.Collections.Generic.List[string]
function Add-Line { param([string]$Text = '') ; $lines.Add($Text) }

Add-Line ('=' * 68)
Add-Line ('WSL diagnostic  ' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
Add-Line ('=' * 68)
Add-Line ''

Add-Line '--- environment ---'
Add-Line (' client      : ' + $Wsl + '  exists=' + (Test-Path $Wsl))
if (Test-Path $Wsl) {
    $v = (Get-Item $Wsl).VersionInfo.FileVersion
    Add-Line (' client ver  : ' + $v)
}
$svc = Get-CimInstance Win32_Service -Filter "Name='WslService'" -ErrorAction SilentlyContinue
if ($svc) {
    Add-Line (' service     : ' + $svc.State + '  pid=' + $svc.ProcessId + '  mode=' + $svc.StartMode)
} else {
    Add-Line ' service     : NOT FOUND'
}
$vm = Get-Process -Name 'vmmem*' -ErrorAction SilentlyContinue
if ($vm) {
    foreach ($p in $vm) {
        Add-Line (' vmmem       : ' + $p.ProcessName + ' pid=' + $p.Id + ' ws=' + [math]::Round($p.WorkingSet64 / 1MB, 0) + 'MB')
    }
} else {
    Add-Line ' vmmem       : not running'
}
$cfg = Join-Path $env:USERPROFILE '.wslconfig'
Add-Line (' .wslconfig  : ' + (Test-Path $cfg))
Add-Line ''

# Each probe gets a hard timeout so a hang cannot block the whole run.
function Invoke-Probe {
    param([string]$Label, [string[]]$WslArgs)

    Add-Line ('--- probe: ' + $Label + ' ---')
    Add-Line (' args: ' + ($WslArgs -join ' '))

    if (-not (Test-Path $Wsl)) {
        Add-Line ' SKIP: client binary missing'
        Add-Line ''
        return
    }

    $oFile = Join-Path $OutDir ('o_' + $Label + '.txt')
    $eFile = Join-Path $OutDir ('e_' + $Label + '.txt')
    Remove-Item $oFile, $eFile -ErrorAction SilentlyContinue

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $p = Start-Process -FilePath $Wsl -ArgumentList $WslArgs -NoNewWindow -PassThru `
                           -RedirectStandardOutput $oFile -RedirectStandardError $eFile
    } catch {
        Add-Line (' START FAILED: ' + $_.Exception.Message)
        Add-Line ''
        return
    }

    $finished = $p.WaitForExit($TimeoutSec * 1000)
    $sw.Stop()

    if (-not $finished) {
        try { $p.Kill() } catch { }
        Add-Line (' RESULT: TIMEOUT after ' + $TimeoutSec + 's -> killed (pid ' + $p.Id + ')')
    } else {
        Add-Line (' RESULT: exit=' + $p.ExitCode + '  elapsed=' + [math]::Round($sw.Elapsed.TotalSeconds, 1) + 's')
    }

    foreach ($pair in @(@('stdout', $oFile), @('stderr', $eFile))) {
        if (Test-Path $pair[1]) {
            $t = Get-Content $pair[1] -Raw -ErrorAction SilentlyContinue
            if ($t -and $t.Trim().Length -gt 0) {
                $t = $t.Trim()
                if ($t.Length -gt 2000) { $t = $t.Substring(0, 2000) + ' ...[truncated]' }
                Add-Line (' ' + $pair[0] + ':')
                foreach ($ln in ($t -split "`r?`n")) { Add-Line ('   | ' + $ln) }
            } else {
                Add-Line (' ' + $pair[0] + ': (empty)')
            }
        }
    }
    Add-Line ''
}

# Layer 1: pure client, no VM involved. If this hangs the binary itself is broken.
Invoke-Probe -Label 'version' -WslArgs @('--version')

# Layer 2: client -> service RPC, still no shell.
Invoke-Probe -Label 'status' -WslArgs @('--status')

# Layer 3: enumerate registered distros.
Invoke-Probe -Label 'list' -WslArgs @('--list', '--verbose')

# Layer 4: actually enter the distro as root, bypassing the OOBE user wizard.
Invoke-Probe -Label 'root-echo' -WslArgs @('-d', 'Ubuntu', '-u', 'root', '--', 'echo', 'WSL_ROOT_OK')

# Layer 5: plain entry as configured (hits the OOBE wizard if RunOOBE is set).
Invoke-Probe -Label 'default-echo' -WslArgs @('-d', 'Ubuntu', '--', 'echo', 'WSL_DEFAULT_OK')

Add-Line '--- registry snapshot ---'
$lx = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Lxss'
Get-ChildItem $lx -ErrorAction SilentlyContinue | ForEach-Object {
    $p = Get-ItemProperty $_.PSPath -ErrorAction SilentlyContinue
    if ($p.DistributionName) {
        Add-Line (' ' + $p.DistributionName + '  State=' + $p.State + '  Uid=' + $p.DefaultUid + `
                  '  Version=' + $p.Version + '  RunOOBE=' + $p.RunOOBE + '  BasePath=' + $p.BasePath)
    }
}
Add-Line ''
Add-Line '--- crash events (last 24h) ---'
try {
    Get-WinEvent -FilterHashtable @{ LogName = 'Application'; StartTime = (Get-Date).AddDays(-1) } -ErrorAction Stop |
        Where-Object { $_.Id -in 1000, 1001 -and $_.Message -match 'wsl' } |
        Select-Object -First 5 |
        ForEach-Object {
            $m = ($_.Message -replace "`r`n", ' ' -replace '\s+', ' ').Trim()
            if ($m.Length -gt 260) { $m = $m.Substring(0, 260) }
            Add-Line (' [' + $_.TimeCreated.ToString('HH:mm:ss') + '] ID=' + $_.Id + ' ' + $m)
        }
} catch {
    Add-Line (' read failed: ' + $_.Exception.Message)
}

$lines | Set-Content -Path $Result -Encoding UTF8
Write-Host ''
Write-Host ('DONE. Report written to: ' + $Result) -ForegroundColor Green
Write-Host ''
$lines | ForEach-Object { Write-Host $_ }
Write-Host ''
Write-Host 'Copy the whole output above, or just tell me it finished.' -ForegroundColor Cyan

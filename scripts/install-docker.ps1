# install-docker.ps1
# Install Docker Desktop with BOTH the program and the image data on D:.
#
# RUN AS ADMINISTRATOR, and only AFTER install-wsl2.ps1 has completed
# (Docker Desktop's default backend is WSL2 and needs the WSL kernel).
#
# Flags used (per Docker official docs, "Install Docker Desktop on Windows"):
#   --installation-dir        changes the program location (default C:\Program Files\Docker\Docker)
#   --wsl-default-data-root   changes where images/containers/volumes are stored (WSL2 backend)
#   --backend=wsl-2           explicit, so the installer never picks Hyper-V on this machine
#   --accept-license          accept the subscription agreement non-interactively
#
# Important pitfall: do NOT place the installer .exe inside the target install
# directory. The installer creates a .staging folder next to itself and the run
# fails with "Access to the path ... is denied".

param(
    [string]$InstallDir = 'D:\Docker\app',
    [string]$DataRoot   = 'D:\Docker\data',
    [string]$InstallerPath = "$env:TEMP\DockerDesktopInstaller.exe"
)

$ErrorActionPreference = 'Stop'

function Assert-Admin {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        throw 'This script must run in an elevated (Administrator) PowerShell window.'
    }
}

Assert-Admin

# ---------- 0. preflight: WSL2 must be present AND responsive ----------
# Do not call bare `wsl`: PATH resolves to the 268 KB stub in System32, and if
# WslService is wedged the call blocks forever with no output. Always use the
# 3.x binary under Program Files with a hard timeout.
Write-Host '[0/4] checking WSL2'
$wslExe = 'C:\Program Files\WSL\wsl.exe'
if (-not (Test-Path $wslExe)) { $wslExe = 'wsl.exe' }

# Use System.Diagnostics.Process directly. Start-Process -PassThru leaves
# .ExitCode as $null on some builds, and $null -ne 0 is TRUE, which would
# report a healthy WSL as broken.
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName               = $wslExe
$psi.Arguments              = '--status'
$psi.UseShellExecute        = $false
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError  = $true
$psi.CreateNoWindow         = $true
# WSL writes UTF-16LE to redirected pipes.
$psi.StandardOutputEncoding = [System.Text.Encoding]::Unicode
$psi.StandardErrorEncoding  = [System.Text.Encoding]::Unicode

$proc = New-Object System.Diagnostics.Process
$proc.StartInfo = $psi
[void]$proc.Start()

if (-not $proc.WaitForExit(20000)) {
    try { $proc.Kill() } catch { }
    throw 'wsl --status did not respond within 20s. WslService is stuck. Run scripts/fix-wsl-service.ps1 (as Administrator), then retry.'
}

$wslOut  = $proc.StandardOutput.ReadToEnd()
$wslErr  = $proc.StandardError.ReadToEnd()
$wslCode = $proc.ExitCode

if ($wslCode -ne 0) {
    throw ("wsl --status failed, exit={0}. stderr: {1}" -f $wslCode, $wslErr.Trim())
}
if (-not $wslOut -or $wslOut.Trim().Length -eq 0) {
    throw 'wsl --status returned nothing. WSL is not healthy.'
}
$wslHead = ($wslOut -split "`r?`n" | Where-Object { $_.Trim() } | Select-Object -First 2) -join ' | '
Write-Host ('      OK: ' + $wslHead)

# Refuse to reinstall over an existing installation.
foreach ($candidate in @('D:\Docker\app\Docker\Docker Desktop.exe', 'C:\Program Files\Docker\Docker\Docker Desktop.exe')) {
    if (Test-Path $candidate) {
        throw "Docker Desktop is already installed at $candidate. Uninstall it first, or run it directly."
    }
}

# ---------- 1. download installer ----------
Write-Host "[1/4] downloading installer to $InstallerPath"
$url = 'https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe'
# Prefer a copy that was already fetched with curl: Invoke-WebRequest is markedly
# slower on a 592 MB file because the progress bar dominates the run.
if (-not (Test-Path $InstallerPath)) {
    $cached = 'D:\WSL2\pkg\DockerDesktopInstaller.exe'
    if (Test-Path $cached) {
        Write-Host "      using pre-downloaded copy: $cached"
        $InstallerPath = $cached
    }
}
if (Test-Path $InstallerPath) {
    Write-Host '      installer already downloaded, reusing'
} else {
    Invoke-WebRequest -Uri $url -OutFile $InstallerPath -UseBasicParsing
}
Write-Host ("      size: {0:N1} MB" -f ((Get-Item $InstallerPath).Length / 1MB))

# ---------- 2. guard against target collisions ----------
foreach ($dir in @($InstallDir, $DataRoot)) {
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
}
if ((Split-Path $InstallerPath -Parent) -eq $InstallDir) {
    throw 'Installer must not sit inside the install directory (staging conflict). Move it and retry.'
}

# ---------- 3. install ----------
Write-Host '[2/4] running installer (this takes several minutes, do not close the window)'
# Note: never name a variable $args - it is a PowerShell automatic variable.
$installerArgs = @(
    'install',
    "--installation-dir=$InstallDir",
    "--wsl-default-data-root=$DataRoot",
    '--backend=wsl-2',
    '--accept-license',
    '--always-run-service'
)
$proc = Start-Process -FilePath $InstallerPath -ArgumentList $installerArgs -Wait -PassThru -NoNewWindow
if ($proc.ExitCode -ne 0) {
    throw "Installer exited with code $($proc.ExitCode). Check %TEMP%\DockerDesktopInstaller logs."
}

# ---------- 4. verify ----------
Write-Host '[3/4] verifying installation'
$dockerExe = Join-Path $InstallDir 'Docker\Docker Desktop.exe'
if (Test-Path $dockerExe) {
    Write-Host "      program: $dockerExe"
} else {
    Write-Host "[WARN] expected exe not found at $dockerExe - the installer may have nested one more level" -ForegroundColor Yellow
}

Write-Host '[4/4] done'
Write-Host ''
Write-Host 'Next steps:' -ForegroundColor Cyan
Write-Host '  1. Launch Docker Desktop once and finish the first-run wizard.'
Write-Host '  2. In Settings > Resources > Advanced, confirm "Disk image location" is on D:.'
Write-Host '  3. Sign out and back in so the docker-users group membership applies.'
Write-Host '  4. Verify with:  docker version ; docker info'
Write-Host ''
Write-Host "Data root requested: $DataRoot" -ForegroundColor Cyan
Write-Host 'If images still land on C:, move them later via Settings > Resources > Advanced.' -ForegroundColor Yellow

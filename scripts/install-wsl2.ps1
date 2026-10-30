# install-wsl2.ps1
# Install WSL2 and relocate the Ubuntu distribution to D:\WSL2\app
#
# RUN AS ADMINISTRATOR. This script has TWO phases because phase 1 requires a reboot.
#
#   Phase 1:  .\install-wsl2.ps1 -Phase Core     (then REBOOT)
#   Phase 2:  .\install-wsl2.ps1 -Phase Distro   (after reboot)
#
# Why two phases: `wsl --install --no-distribution` enables the Virtual Machine
# Platform and WSL optional features, which only take effect after a restart.
# The Ubuntu distro cannot be installed before that.
#
# Disk note: by default a WSL distro's ext4.vhdx lives under
# %LOCALAPPDATA%\wsl\{GUID}\ on C:. A 20-50 GB VHDX is normal.
# `wsl --manage <Distro> --move <Path>` relocates it (WSL 2.0.4+).

param(
    [ValidateSet('Core', 'Kernel', 'Store', 'Distro', 'Status')]
    [string]$Phase = 'Status',

    [string]$Distro = 'Ubuntu',
    [string]$TargetDir = 'D:\WSL2\app',
    [string]$KernelMsi = 'D:\WSL2\pkg\wsl_update_x64.msi'
)

$ErrorActionPreference = 'Stop'

function Assert-Admin {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        throw 'This script must run in an elevated (Administrator) PowerShell window.'
    }
}

function Show-Status {
    Write-Host '--- WSL version ---' -ForegroundColor Cyan
    wsl --version
    Write-Host '--- Installed distributions ---' -ForegroundColor Cyan
    wsl --list --verbose
    Write-Host '--- VHDX locations ---' -ForegroundColor Cyan
    Get-ChildItem 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Lxss' -ErrorAction SilentlyContinue |
        ForEach-Object {
            $p = Get-ItemProperty $_.PSPath
            Write-Host ("  {0} -> {1}" -f $p.DistributionName, $p.BasePath)
        }
}

switch ($Phase) {

    'Status' {
        Show-Status
    }

    'Store' {
        # Why this phase exists: on Windows 11 24H2+ (build 26100 and later) WSL is a
        # standalone MSIX app, not the legacy lxss runtime. `wsl --install` fetches that
        # MSIX from GitHub, which hangs on blocked networks (observed: 62 min, 0 bytes).
        # The Store channel delivers the same app through Microsoft's own CDN
        # (displaycatalog.mp.microsoft.com answered 200 from this machine), so it is the
        # practical route when GitHub is unreachable.
        Write-Host '[1/2] installing WSL from Microsoft Store (product id 9P9TQF7MRM4R)'

        # WSL's MSIX carries a per-package-family policy that pins it to the system
        # volume. If "where new apps are saved" points at another drive, deployment is
        # rejected with 0x80073D21 (ERROR_DEPLOYMENT_BLOCKED_BY_VOLUME_POLICY_PACKAGE)
        # before a single byte is downloaded. Reset the default volume first.
        try {
            $vol = Get-AppxDefaultVolume -ErrorAction Stop
            if ($vol.PackageStorePath -notlike 'C:\*') {
                Write-Host "  Current default app volume is $($vol.PackageStorePath)" -ForegroundColor Yellow
                Write-Host '  WSL must install to the system drive (C:). Switching the default back...'
                Set-AppxDefaultVolume -Volume 'C:' -ErrorAction Stop
                Write-Host '  Default app volume is now C:\WindowsApps' -ForegroundColor Green
                Write-Host '  Note: only the WSL app itself goes to C: (~a few hundred MB).'
                Write-Host '        The Ubuntu disk image is still relocated to D:\WSL2\app later.'
            }
        } catch {
            Write-Host "  Could not inspect/reset the default app volume: $($_.Exception.Message)" -ForegroundColor Yellow
            Write-Host '  Fix manually: Settings > System > Storage > Advanced storage settings >'
            Write-Host '  Where new content is saved > "New apps will save to" = C:'
        }

        if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
            Write-Host '  winget not available. Install manually:' -ForegroundColor Yellow
            Write-Host '  ms-windows-store://pdp/?ProductId=9P9TQF7MRM4R'
            return
        }
        winget install --id 9P9TQF7MRM4R --source msstore `
            --accept-package-agreements --accept-source-agreements
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  winget exited with $LASTEXITCODE." -ForegroundColor Yellow
            Write-Host '  Fallback - open the Store page and click Install:' -ForegroundColor Yellow
            Write-Host '  ms-windows-store://pdp/?ProductId=9P9TQF7MRM4R'
            return
        }

        Write-Host '[2/2] verifying'
        wsl --version
        Write-Host ''
        Write-Host 'Next: install-wsl2.ps1 -Phase Distro' -ForegroundColor Green
    }

    'Kernel' {
        # Why this phase exists: `wsl --install --no-distribution` on Windows 11 24H2+
        # downloads the ~350 MB "WSL" MSI from GitHub releases. On networks where
        # github.com is unreachable the command does not fail fast - the TCP connection
        # is ESTABLISHED but zero bytes flow, and it hangs indefinitely (observed:
        # 62 minutes, 0 MB read, 1.6 s CPU). This phase installs the 16 MB legacy
        # kernel package instead, which comes from Microsoft's own Azure blob.
        Assert-Admin
        # Windows 11 24H2+ replaced the legacy WSL architecture: wsl.exe is now a thin
        # stub and the real runtime ships as a standalone MSIX from the Store/GitHub.
        # The 16 MB wsl_update_x64.msi (kernel 5.10.16) gates itself on the LxssManager
        # service, which no longer exists on those builds - it fails with 1603 and the
        # event log says "This update only applies to machines with the Windows
        # Subsystem for Linux". Detect that up front instead of letting msiexec fail.
        if (-not (Test-Path 'HKLM:\SYSTEM\CurrentControlSet\Services\LxssManager')) {
            throw @"
This machine has no LxssManager service, so the legacy 16 MB kernel package cannot
install here (it will fail with error 1603). Windows 11 24H2 and later need the
standalone WSL app instead. Use:

    install-wsl2.ps1 -Phase Store
"@
        }
        if (-not (Test-Path $KernelMsi)) {
            throw "Kernel package not found: $KernelMsi`nDownload it first:`n  curl -L -o D:\WSL2\pkg\wsl_update_x64.msi https://wslstorestorage.blob.core.windows.net/wslblob/wsl_update_x64.msi"
        }
        $sizeMb = [math]::Round((Get-Item $KernelMsi).Length / 1MB, 1)
        Write-Host "[1/2] installing WSL2 kernel ($sizeMb MB) from $KernelMsi"
        $proc = Start-Process msiexec.exe -ArgumentList "/i `"$KernelMsi`" /qn /norestart" -Wait -PassThru
        if ($proc.ExitCode -eq 3010) {
            Write-Host '  Installed. Reboot required to activate the kernel.' -ForegroundColor Yellow
        } elseif ($proc.ExitCode -ne 0) {
            throw "msiexec exited with code $($proc.ExitCode). Run it interactively to see the error: msiexec /i `"$KernelMsi`""
        }

        Write-Host '[2/2] verifying'
        wsl --set-default-version 2
        wsl --version
    }

    'Core' {
        Assert-Admin
        Write-Host '[1/3] enabling WSL and Virtual Machine Platform features'
        # These are optional Windows features; both are required by WSL2.
        dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart | Out-Host
        dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart | Out-Host

        Write-Host '[2/3] installing WSL core (no distribution yet)'
        # This step fetches a ~350 MB MSI from GitHub releases on Windows 11 24H2+.
        # Probe reachability first - on a blocked network the download hangs forever
        # instead of erroring out. Fall back to the legacy kernel (see -Phase Kernel).
        $githubReachable = $false
        try {
            $probe = Invoke-WebRequest -Uri 'https://github.com' -Method Head -TimeoutSec 8 -UseBasicParsing -ErrorAction Stop
            $githubReachable = $probe.StatusCode -lt 500
        } catch {
            $githubReachable = $false
        }
        if ($githubReachable) {
            wsl --install --no-distribution
        } else {
            Write-Host '  github.com is unreachable from here - skipping.' -ForegroundColor Yellow
            Write-Host '  The 350 MB WSL MSI cannot be downloaded. Use the 16 MB legacy kernel:'
            Write-Host '    install-wsl2.ps1 -Phase Kernel    (after reboot)' -ForegroundColor Yellow
        }

        Write-Host '[3/3] setting default version to 2'
        wsl --set-default-version 2

        Write-Host ''
        Write-Host 'DONE. REBOOT NOW, then run:' -ForegroundColor Yellow
        Write-Host '  .\install-wsl2.ps1 -Phase Distro'
    }

    'Distro' {
        Assert-Admin

        Write-Host "[1/4] installing distribution '$Distro' (lands on C: first)"

        # Ubuntu ships as an MSIX bundle too, so it obeys the same per-package volume
        # policy that blocked the WSL app with 0x80073D21. Verify the default app volume
        # points at the system drive before pulling ~600 MB.
        try {
            $vol = Get-AppxDefaultVolume -ErrorAction Stop
            if ($vol.PackageStorePath -notlike 'C:\*') {
                Write-Host "  Default app volume is $($vol.PackageStorePath); switching to C: (required by MSIX policy)" -ForegroundColor Yellow
                Set-AppxDefaultVolume -Volume 'C:' -ErrorAction Stop
            }
        } catch {
            Write-Host "  Could not verify the default app volume: $($_.Exception.Message)" -ForegroundColor Yellow
        }

        # --legacy only matters when the standalone WSL runtime is absent: it selects the
        # inbox implementation instead of fetching the app. WSL 3.x rejects the flag once
        # the runtime is installed, so switch it off based on what is actually on disk.
        if (Test-Path 'C:\Program Files\WSL\wsl.exe') {
            Write-Host '  WSL runtime detected - installing the distribution directly'
            wsl --install -d $Distro --no-launch
        } else {
            wsl --install -d $Distro --no-launch --legacy
            if ($LASTEXITCODE -ne 0) {
                Write-Host '  --legacy rejected by this build, retrying without it' -ForegroundColor Yellow
                wsl --install -d $Distro --no-launch
            }
        }

        Write-Host '[2/4] shutting down all WSL instances (required before move)'
        wsl --shutdown
        Start-Sleep -Seconds 3

        if (Test-Path $TargetDir) {
            $items = Get-ChildItem $TargetDir -Force -ErrorAction SilentlyContinue
            if ($items) {
                throw "Target directory $TargetDir is not empty. Move or clear it first: --move requires an empty/absent target."
            }
        } else {
            New-Item -ItemType Directory -Path $TargetDir -Force | Out-Null
        }

        Write-Host "[3/4] relocating distribution to $TargetDir"
        # --move requires WSL >= 2.0.4; if it fails, fall back to export/import below.
        wsl --manage $Distro --move $TargetDir
        if ($LASTEXITCODE -ne 0) {
            Write-Host ''
            Write-Host 'Move failed. Two common causes, in order of likelihood:' -ForegroundColor Yellow
            Write-Host ''
            Write-Host "  [1] The distribution has never been started, so it has no disk image yet." -ForegroundColor Yellow
            Write-Host "      A freshly installed distro creates ext4.vhdx on first launch, and" -ForegroundColor Yellow
            Write-Host "      --move has nothing to move until then. Fix: run" -ForegroundColor Yellow
            Write-Host "          wsl -d $Distro" -ForegroundColor Cyan
            Write-Host "      complete the UNIX username/password prompt, type 'exit', then re-run" -ForegroundColor Yellow
            Write-Host "          install-wsl2.ps1 -Phase Distro" -ForegroundColor Cyan
            Write-Host ''
            Write-Host '  [2] WSL older than 2.0.4 has no --move. Manual route:' -ForegroundColor Yellow
            Write-Host "          wsl --export $Distro D:\WSL2\backup-$Distro.vhdx --format vhd"
            Write-Host "          wsl --unregister $Distro"
            Write-Host "          wsl --import-in-place $Distro $TargetDir\ext4.vhdx"
            Write-Host '      then restore the default user in /etc/wsl.conf: [user] default=<name>'
            return
        }

        Write-Host '[4/4] verifying'
        wsl --list --verbose
        Show-Status
        Write-Host ''
        Write-Host "WSL2 ready. Distribution data now under $TargetDir" -ForegroundColor Green
    }
}

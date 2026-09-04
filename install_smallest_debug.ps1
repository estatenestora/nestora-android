[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$adbPath = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $adbPath)) {
    throw "Android adb was not found at $adbPath. Install Android platform-tools first."
}

$supportedAbis = @('arm64-v8a', 'armeabi-v7a', 'x86_64', 'x86')
$serials = @(
    & $adbPath devices |
        ForEach-Object {
            $parts = $_ -split "`t"
            if ($parts.Count -ge 2 -and $parts[1].Trim() -eq 'device') { $parts[0].Trim() }
        }
)
if ($serials.Count -eq 0) {
    throw 'No authorized Android device or emulator is connected.'
}

$devicesByAbi = @{}
foreach ($serial in $serials) {
    $abi = (& $adbPath -s $serial shell getprop ro.product.cpu.abi).Trim()
    if ($abi -notin $supportedAbis) {
        $abi = ((& $adbPath -s $serial shell getprop ro.product.cpu.abilist).Trim() -split ',') |
            Where-Object { $_ -in $supportedAbis } |
            Select-Object -First 1
    }
    if ([string]::IsNullOrWhiteSpace($abi)) {
        throw "Could not determine a supported CPU ABI for device $serial."
    }
    if (-not $devicesByAbi.ContainsKey($abi)) { $devicesByAbi[$abi] = @() }
    $devicesByAbi[$abi] += $serial
}

Push-Location $projectRoot
try {
    foreach ($abi in $devicesByAbi.Keys) {
        Write-Host "Building the smallest debug APK for $abi..."
        & .\gradlew.bat :app:assembleDebug --offline --no-build-cache "-Pandroid.injected.build.abi=$abi"
        if ($LASTEXITCODE -ne 0) { throw "Debug build failed for ABI $abi." }

        $apkPath = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
        if (-not (Test-Path -LiteralPath $apkPath)) {
            $apkPath = Join-Path $projectRoot 'app\build\intermediates\apk\debug\app-debug.apk'
        }
        if (-not (Test-Path -LiteralPath $apkPath)) {
            throw "Expected APK was not produced: $apkPath"
        }
        $sizeMb = [math]::Round((Get-Item -LiteralPath $apkPath).Length / 1MB, 1)
        foreach ($serial in $devicesByAbi[$abi]) {
            Write-Host "Installing ${sizeMb} MB $abi APK on $serial..."
            & $adbPath -s $serial install -r -t $apkPath
            if ($LASTEXITCODE -ne 0) { throw "Installation failed on $serial." }
            & $adbPath -s $serial shell am start -n 'com.estatenestora.app/.MainActivity'
            if ($LASTEXITCODE -ne 0) { throw "Launch failed on $serial." }
        }
    }
} finally {
    Pop-Location
}

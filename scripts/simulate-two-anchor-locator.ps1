param(
    [Parameter(Mandatory = $true)]
    [int]$LeftRssi,

    [int]$RightRssi = 0,

    [string]$LeftName = "vasya",

    [string]$RightName = "petya",

    [string]$TargetName = "zhenya"
)

$leftCount = 1
$rightCount = if ($PSBoundParameters.ContainsKey("RightRssi")) { 1 } else { 0 }

function Get-ClockDirection {
    param([int]$DeltaRightMinusLeft)

    if ($DeltaRightMinusLeft -ge 15) { return "3" }
    if ($DeltaRightMinusLeft -ge 9) { return "2" }
    if ($DeltaRightMinusLeft -ge 4) { return "1" }
    if ($DeltaRightMinusLeft -le -15) { return "9" }
    if ($DeltaRightMinusLeft -le -9) { return "10" }
    if ($DeltaRightMinusLeft -le -4) { return "11" }
    return "12"
}

function Get-DistanceBucket {
    param([int]$MeanRssi)

    if ($MeanRssi -ge -50) { return 5 }
    if ($MeanRssi -ge -58) { return 10 }
    if ($MeanRssi -ge -65) { return 15 }
    if ($MeanRssi -ge -71) { return 20 }
    return 25
}

if ($leftCount -lt 1 -or $rightCount -lt 1) {
    Write-Output ("target={0} left={1}:{2} right={3}:missing locator=INSUFFICIENT_DATA" -f `
            $TargetName, $LeftName, $LeftRssi, $RightName)
    exit 0
}

$delta = $RightRssi - $LeftRssi
$mean = [int][Math]::Round(($LeftRssi + $RightRssi) / 2.0, [MidpointRounding]::AwayFromZero)
$distance = Get-DistanceBucket -MeanRssi $mean
$clock = Get-ClockDirection -DeltaRightMinusLeft $delta
$confidence = if ([Math]::Abs($delta) -ge 4) { "LOW" } else { "LOW" }

Write-Output ("target={0} left={1}:{2} right={3}:{4} locator={5}m clock={6} deltaDb={7} meanRssi={8} confidence={9}" -f `
        $TargetName, $LeftName, $LeftRssi, $RightName, $RightRssi, $distance, $clock, $delta, $mean, $confidence)

param(
    [string] $BaseUrl = "http://localhost:8080",
    [string] $CasesPath = "src/test/resources/chatbot-scenarios.json",
    [int] $DelayMillis = 0
)

$ErrorActionPreference = "Stop"

function Resolve-CasePath {
    param([string] $Path)

    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }

    return Join-Path (Get-Location) $Path
}

function Test-ContainsText {
    param(
        [string] $Actual,
        [string] $Expected
    )

    if ([string]::IsNullOrWhiteSpace($Expected)) {
        return $true
    }

    return $Actual -like "*$Expected*"
}

function Get-Labels {
    param($SuggestedReplies)

    if ($null -eq $SuggestedReplies) {
        return @()
    }

    return @($SuggestedReplies | ForEach-Object { $_.label })
}

function Format-Failures {
    param([string[]] $Failures)

    if ($Failures.Count -eq 0) {
        return ""
    }

    return $Failures -join " | "
}

$resolvedCasesPath = Resolve-CasePath $CasesPath
if (-not (Test-Path $resolvedCasesPath)) {
    throw "Scenario file not found: $resolvedCasesPath"
}

$askUrl = "$BaseUrl/api/chatbot/ask"
$healthUrl = "$BaseUrl/api/chatbot/health"

try {
    Invoke-RestMethod -Method Get -Uri $healthUrl | Out-Null
} catch {
    throw "Chatbot server is not reachable at $healthUrl. Start the backend first, then run this script again."
}

$scenarios = Get-Content -Encoding UTF8 -Raw $resolvedCasesPath | ConvertFrom-Json
$results = New-Object System.Collections.Generic.List[object]
$passed = 0
$failed = 0

for ($scenarioIndex = 0; $scenarioIndex -lt $scenarios.Count; $scenarioIndex++) {
    $scenario = $scenarios[$scenarioIndex]
    $sessionId = "scenario-$scenarioIndex-$([Guid]::NewGuid().ToString('N'))"
    $steps = @($scenario.steps)

    for ($stepIndex = 0; $stepIndex -lt $steps.Count; $stepIndex++) {
        $step = $steps[$stepIndex]
        $body = @{
            sessionId = $sessionId
            message = $step.message
        } | ConvertTo-Json -Depth 5

        $failures = New-Object System.Collections.Generic.List[string]

        try {
            $response = Invoke-RestMethod `
                -Method Post `
                -Uri $askUrl `
                -ContentType "application/json; charset=utf-8" `
                -Body $body

            $data = $response.data
            $actualType = $data.responseType
            $actualAnswer = [string] $data.answer
            $actualMatchedPolicyCount = [int] $data.matchedPolicyCount
            $labels = Get-Labels $data.suggestedReplies

            if ($step.expectedType -and $actualType -ne $step.expectedType) {
                $failures.Add("type expected=$($step.expectedType), actual=$actualType")
            }

            if ($null -ne $step.expectedMatchedPolicyCount -and $actualMatchedPolicyCount -ne [int] $step.expectedMatchedPolicyCount) {
                $failures.Add("matchedPolicyCount expected=$($step.expectedMatchedPolicyCount), actual=$actualMatchedPolicyCount")
            }

            if ($null -ne $step.minMatchedPolicyCount -and $actualMatchedPolicyCount -lt [int] $step.minMatchedPolicyCount) {
                $failures.Add("matchedPolicyCount expected>=$($step.minMatchedPolicyCount), actual=$actualMatchedPolicyCount")
            }

            if ($step.answerContains -and -not (Test-ContainsText $actualAnswer $step.answerContains)) {
                $failures.Add("answer missing '$($step.answerContains)'")
            }

            if ($step.expectedSuggestedReplyLabels) {
                foreach ($expectedLabel in @($step.expectedSuggestedReplyLabels)) {
                    if ($labels -notcontains $expectedLabel) {
                        $failures.Add("missing suggestedReply label '$expectedLabel'")
                    }
                }
            }

            $status = if ($failures.Count -eq 0) { "PASS" } else { "FAIL" }
            if ($status -eq "PASS") {
                $passed++
            } else {
                $failed++
            }

            $results.Add([pscustomobject]@{
                Status = $status
                Scenario = $scenario.name
                Step = $stepIndex + 1
                Message = $step.message
                ExpectedType = $step.expectedType
                ActualType = $actualType
                Matched = $actualMatchedPolicyCount
                Answer = $actualAnswer
                SuggestedLabels = $labels -join ", "
                Failures = Format-Failures @($failures)
            })
        } catch {
            $failed++
            $results.Add([pscustomobject]@{
                Status = "ERROR"
                Scenario = $scenario.name
                Step = $stepIndex + 1
                Message = $step.message
                ExpectedType = $step.expectedType
                ActualType = ""
                Matched = ""
                Answer = ""
                SuggestedLabels = ""
                Failures = $_.Exception.Message
            })
        }

        if ($DelayMillis -gt 0) {
            Start-Sleep -Milliseconds $DelayMillis
        }
    }
}

$results | Format-Table -AutoSize
Write-Host ""
Write-Host "Summary: $passed passed, $failed failed"

if ($failed -gt 0) {
    Write-Host ""
    Write-Host "Failures:"
    $results |
        Where-Object { $_.Status -ne "PASS" } |
        Format-List Status, Scenario, Step, Message, ExpectedType, ActualType, Matched, Answer, SuggestedLabels, Failures

    exit 1
}

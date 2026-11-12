param(
    [ValidateRange(1, 65535)][int]$Port = 1433,
    [string]$AuthDirectory = (Join-Path $PSScriptRoot '../target'),
    [switch]$SqlAuthentication
)

$ErrorActionPreference = 'Stop'
$testVariables = @('RUN_SQLSERVER_TESTS', 'SQLSERVER_TEST_MODE', 'SQLSERVER_TEST_ALLOW_CREATE',
    'SQLSERVER_TEST_PORT', 'SQLSERVER_TEST_USERNAME', 'SQLSERVER_TEST_PASSWORD', 'PATH')
$previous = @{}
foreach ($name in $testVariables) {
    $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

Push-Location (Join-Path $PSScriptRoot '..')
try {
    $mavenArguments = @('test')
    if ($SqlAuthentication) {
        if (-not $env:SQLSERVER_TEST_USERNAME -or -not $env:SQLSERVER_TEST_PASSWORD) {
            throw 'Set SQLSERVER_TEST_USERNAME and SQLSERVER_TEST_PASSWORD through your environment first.'
        }
    } else {
        $resolvedAuth = (Resolve-Path -LiteralPath $AuthDirectory).Path
        # Match the JDBC version managed by the current Spring Boot BOM.
        if (-not (Test-Path -LiteralPath (Join-Path $resolvedAuth 'mssql-jdbc_auth-12.6.4.x64.dll'))) {
            throw 'Windows authentication requires Microsoft mssql-jdbc_auth-12.6.4.x64.dll in AuthDirectory.'
        }
        # Windows JVM resolves native libraries through PATH. Avoid nested cmd/Surefire quoting
        # of java.library.path when the checkout or driver directory contains spaces.
        $env:PATH = $resolvedAuth + [IO.Path]::PathSeparator + $env:PATH
        Remove-Item Env:SQLSERVER_TEST_USERNAME, Env:SQLSERVER_TEST_PASSWORD -ErrorAction SilentlyContinue
    }
    $env:RUN_SQLSERVER_TESTS = 'true'
    $env:SQLSERVER_TEST_MODE = 'local'
    $env:SQLSERVER_TEST_ALLOW_CREATE = 'true'
    $env:SQLSERVER_TEST_PORT = "$Port"
    # Includes ordinary tests plus both SQL Server suites; only randomly named test DBs are created.
    & .\mvnw.cmd @mavenArguments
    if ($LASTEXITCODE -ne 0) { throw "SQL Server verification failed (Maven exit $LASTEXITCODE)." }
} finally {
    foreach ($name in $testVariables) {
        [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process')
    }
    Pop-Location
}

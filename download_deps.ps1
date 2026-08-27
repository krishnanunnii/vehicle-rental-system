# PowerShell script to download required JAR dependencies for offline compilation and execution

$libDir = Join-Path $PSScriptRoot "lib"
if (-not (Test-Path $libDir)) {
    New-Item -ItemType Directory -Path $libDir | Out-Null
}

$deps = @(
    @{ Name = "javax.servlet-api-4.0.1.jar"; Url = "https://repo1.maven.org/maven2/javax/servlet/javax.servlet-api/4.0.1/javax.servlet-api-4.0.1.jar" },
    @{ Name = "h2-2.2.224.jar"; Url = "https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar" },
    @{ Name = "mysql-connector-j-8.3.0.jar"; Url = "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar" }
)

foreach ($dep in $deps) {
    $targetPath = Join-Path $libDir $dep.Name
    if (-not (Test-Path $targetPath)) {
        Write-Host "Downloading $($dep.Name)..."
        try {
            [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
            Invoke-WebRequest -Uri $dep.Url -OutFile $targetPath -UseBasicParsing
            Write-Host "Downloaded $($dep.Name) successfully."
        } catch {
            Write-Warning "Could not download $($dep.Name): $_"
        }
    } else {
        Write-Host "$($dep.Name) already exists."
    }
}

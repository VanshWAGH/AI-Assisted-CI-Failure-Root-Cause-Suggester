$baseUrl = "http://localhost:8080"
$webhookUrl = "$baseUrl/webhooks/jenkins/build"

$headers = @{
    "Content-Type" = "application/json"
    "X-Jenkins-Token" = "xyzjenkinssecret1234"
}

# Payload 1: Test Failure
$payload1 = @{
    name = "auth-service"
    build = @{
        number = 101
        phase = "COMPLETED"
        status = "FAILURE"
        branch = "main"
        commit = "c8f9a2b"
        log = "[INFO] Running com.rootcause.AuthTest`n[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 2.3 s <<< FAILURE!`n[ERROR] com.rootcause.AuthTest.testLogin  Time elapsed: 0.1 s  <<< FAILURE!`njava.lang.AssertionError: expected:<200> but was:<401>`n`tat com.rootcause.AuthTest.testLogin(AuthTest.java:45)"
    }
}

# Payload 2: Build Error
$payload2 = @{
    name = "frontend-app"
    build = @{
        number = 89
        phase = "COMPLETED"
        status = "FAILURE"
        branch = "feature/dashboard"
        commit = "a1b2c3d"
        log = "Failed to compile.`n`n./src/App.tsx`nModule not found: Can't resolve './components/Sidebar' in '/app/src'`n`nnpm ERR! build failed"
    }
}

# Payload 3: Infra Error
$payload3 = @{
    name = "data-pipeline"
    build = @{
        number = 12
        phase = "COMPLETED"
        status = "FAILURE"
        branch = "main"
        commit = "d4e5f6g"
        log = "Error: Kubernetes cluster unreachable: Get `"https://10.0.0.1:443/api/v1/pods`": dial tcp 10.0.0.1:443: connect: connection timed out`nMakefile:45: recipe for target 'deploy' failed"
    }
}

# Payload 4: Security Error
$payload4 = @{
    name = "payment-gateway"
    build = @{
        number = 205
        phase = "COMPLETED"
        status = "FAILURE"
        branch = "main"
        commit = "x7y8z9w"
        log = "[CRITICAL] Trivy scan found 1 vulnerability in container image`nCVE-2021-44228 (log4shell)`nSeverity: CRITICAL`nPackage: org.apache.logging.log4j:log4j-core`nPipeline failed due to security scan policy."
    }
}

Write-Host "Injecting Payload 1 (Test Failure)..."
Invoke-RestMethod -Uri $webhookUrl -Method Post -Headers $headers -Body ($payload1 | ConvertTo-Json)
Start-Sleep -Seconds 2

Write-Host "Injecting Payload 2 (Build Error)..."
Invoke-RestMethod -Uri $webhookUrl -Method Post -Headers $headers -Body ($payload2 | ConvertTo-Json)
Start-Sleep -Seconds 2

Write-Host "Injecting Payload 3 (Infra Error)..."
Invoke-RestMethod -Uri $webhookUrl -Method Post -Headers $headers -Body ($payload3 | ConvertTo-Json)
Start-Sleep -Seconds 2

Write-Host "Injecting Payload 4 (Security Error)..."
Invoke-RestMethod -Uri $webhookUrl -Method Post -Headers $headers -Body ($payload4 | ConvertTo-Json)
Start-Sleep -Seconds 2

Write-Host "Data injection complete! Check the dashboard."

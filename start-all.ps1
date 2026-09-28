Write-Host "=== KHỞI CHẠY HỆ THỐNG 3 MICROSERVICES DISTRIBUTED TRACING ===" -ForegroundColor Cyan

# 1. Khởi chạy User Service (Port 8082)
Write-Host "Đang khởi chạy User Service trên port 8082..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\user-service'; mvn spring-boot:run"

Start-Sleep -Seconds 5

# 2. Khởi chạy Order Service (Port 8081)
Write-Host "Đang khởi chạy Order Service trên port 8081..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\order-service'; mvn spring-boot:run"

Start-Sleep -Seconds 5

# 3. Khởi chạy Gateway Service (Port 8080)
Write-Host "Đang khởi chạy Gateway Service trên port 8080..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\gateway-service'; mvn spring-boot:run"

Write-Host "`nĐÃ KHỞI CHẠY XONG CẢ 3 DỊCH VỤ:" -ForegroundColor Green
Write-Host "1. Gateway Service : http://localhost:8080"
Write-Host "2. Order Service   : http://localhost:8081"
Write-Host "3. User Service    : http://localhost:8082"
Write-Host "`nBạn có thể gửi request test: curl http://localhost:8080/api/gateway/demo-failure" -ForegroundColor Cyan

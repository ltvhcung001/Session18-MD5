Write-Host "=== DUNG TOAN BO CAC MICROSERVICES TREN CONG 8080, 8081, 8082 ===" -ForegroundColor Yellow

$ports = @(8080, 8081, 8082)
foreach ($port in $ports) {
    $connections = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($connections) {
        $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($procId in $pids) {
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            Write-Host "Da dung tien trinh PID $procId tren cong $port" -ForegroundColor Green
        }
    } else {
        Write-Host "Cong $port khong co tien trinh nao dang chay." -ForegroundColor Gray
    }
}

Write-Host "Da giai phong toan bo cac cong mang." -ForegroundColor Green

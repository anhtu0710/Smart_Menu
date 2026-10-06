$styles = @('HIEN_DAI', 'SANG_TRONG', 'TRUYEN_THONG', 'TRE_TRUNG')
foreach ($s in $styles) {
    Write-Host ("=== GENERATING MENU FOR STYLE: " + $s + " ===")
    $body = @{ styleId = $s } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/smartcoffee/menu/74/generate-final-menu" -Method POST -ContentType "application/json" -Body $body
    Write-Host ("Style: " + $res.styleId + " | ImageUrl: " + $res.menuImageUrl)
}

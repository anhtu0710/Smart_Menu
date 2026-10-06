Add-Type -AssemblyName System.Drawing
$bmp = [System.Drawing.Bitmap]::FromFile("d:/Smart_Menu/backend/src/main/resources/menu-style/TRUYEN_THONG/reference.png")
$pts = @(@(200, 350), @(200, 420), @(500, 350), @(500, 420), @(200, 600), @(500, 600))
foreach ($p in $pts) {
    $c = $bmp.GetPixel($p[0], $p[1])
    Write-Host ("Point (" + $p[0] + "," + $p[1] + "): R=" + $c.R + ", G=" + $c.G + ", B=" + $c.B)
}
$bmp.Dispose()

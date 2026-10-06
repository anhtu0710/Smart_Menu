Add-Type -AssemblyName System.Drawing
$bmp = [System.Drawing.Bitmap]::FromFile("d:/Smart_Menu/backend/src/main/resources/menu-style/SANG_TRONG/reference.png")
Write-Host ("Size: " + $bmp.Width + "x" + $bmp.Height)
$pts = @(@(550, 40), @(850, 40), @(1050, 40), @(550, 200), @(850, 200), @(1050, 200), @(550, 330), @(850, 330), @(1050, 330))
foreach ($p in $pts) {
    $c = $bmp.GetPixel($p[0], $p[1])
    Write-Host ("Pixel (" + $p[0] + "," + $p[1] + "): R=" + $c.R + ", G=" + $c.G + ", B=" + $c.B)
}
$bmp.Dispose()

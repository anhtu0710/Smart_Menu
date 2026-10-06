Add-Type -AssemblyName System.Drawing
$file = "d:\Smart_Menu\backend\src\main\resources\menu-style\TRE_TRUNG\template_clean.png"
$bmp = [System.Drawing.Bitmap]::FromFile($file)

for ($y = 0; $y -lt $bmp.Height; $y += 5) {
    for ($x = 950; $x -lt 1200; $x += 5) {
        $c = $bmp.GetPixel($x, $y)
        if ($c.R -lt 70 -and $c.G -lt 70 -and $c.B -lt 70) {
            # xem có phải thuộc vùng nền kem không
            if ($y -gt 100 -and $y -lt 400 -and $x -lt 1120) {
                Write-Host "Found black at ($x, $y)"
            }
        }
    }
}
$bmp.Dispose()

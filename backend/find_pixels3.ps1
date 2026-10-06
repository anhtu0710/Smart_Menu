Add-Type -AssemblyName System.Drawing
$file = "d:\Smart_Menu\backend\src\main\resources\menu-style\TRE_TRUNG\template_clean.png"
$bmp = [System.Drawing.Bitmap]::FromFile($file)

for ($y = 700; $y -lt 850; $y += 3) {
    for ($x = 1050; $x -lt 1200; $x += 3) {
        $c = $bmp.GetPixel($x, $y)
        if ($c.R -lt 70 -and $c.G -lt 70 -and $c.B -lt 70) {
            Write-Host "Found black at ($x, $y)"
        }
    }
}
$bmp.Dispose()

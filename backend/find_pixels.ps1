Add-Type -AssemblyName System.Drawing
$file = "d:\Smart_Menu\backend\src\main\resources\menu-style\TRE_TRUNG\template_clean.png"
$bmp = [System.Drawing.Bitmap]::FromFile($file)

# Tìm chữ "M": quét vùng x: 800..1200, y: 50..200
for ($y = 80; $y -lt 150; $y++) {
    for ($x = 850; $x -lt 1100; $x++) {
        $c = $bmp.GetPixel($x, $y)
        # Nếu pixel là chữ đen (R < 100, G < 100, B < 100)
        if ($c.R -lt 100 -and $c.G -lt 100 -and $c.B -lt 100) {
            Write-Host "Found black pixel for 'M' at ($x, $y)"
        }
    }
}

# Tìm chữ "...tích cực !!": quét vùng x: 800..1200, y: 600..700
for ($y = 600; $y -lt 680; $y++) {
    for ($x = 800; $x -lt 1000; $x++) {
        $c = $bmp.GetPixel($x, $y)
        if ($c.R -lt 100 -and $c.G -lt 100 -and $c.B -lt 100) {
            Write-Host "Found black pixel for 'tich cuc' at ($x, $y)"
        }
    }
}
$bmp.Dispose()

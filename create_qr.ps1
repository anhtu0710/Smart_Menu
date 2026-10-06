Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap 450, 450
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::White)

$pen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(15, 23, 42), 6)
$brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(15, 23, 42))
$blueBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(37, 99, 235))
$grayBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(100, 116, 139))

# Header
$fontHeader = New-Object System.Drawing.Font('Arial', 14, [System.Drawing.FontStyle]::Bold)
$sf = New-Object System.Drawing.StringFormat
$sf.Alignment = [System.Drawing.StringAlignment]::Center
$g.DrawString('VIETQR - NGAN HANG MBBANK', $fontHeader, $blueBrush, 225, 25, $sf)

# 3 Corner Finder Patterns
# Top-Left
$g.DrawRectangle($pen, 50, 70, 90, 90)
$g.FillRectangle($brush, 70, 90, 50, 50)

# Top-Right
$g.DrawRectangle($pen, 310, 70, 90, 90)
$g.FillRectangle($brush, 330, 90, 50, 50)

# Bottom-Left
$g.DrawRectangle($pen, 50, 310, 90, 90)
$g.FillRectangle($brush, 70, 330, 50, 50)

# Matrix dots
$rnd = New-Object System.Random(42)
for ($x = 50; $x -le 380; $x += 18) {
    for ($y = 70; $y -le 380; $y += 18) {
        $inTL = ($x -lt 150 -and $y -lt 170)
        $inTR = ($x -gt 290 -and $y -lt 170)
        $inBL = ($x -lt 150 -and $y -gt 290)
        if (-not ($inTL -or $inTR -or $inBL)) {
            if ($rnd.Next(10) -lt 6) {
                $g.FillRectangle($brush, $x, $y, 14, 14)
            }
        }
    }
}

# Center Logo Box
$g.FillRectangle([System.Drawing.Brushes]::White, 185, 195, 80, 50)
$g.DrawRectangle($pen, 185, 195, 80, 50)
$fontLogo = New-Object System.Drawing.Font('Arial', 11, [System.Drawing.FontStyle]::Bold)
$g.DrawString('SMART', $fontLogo, $blueBrush, 225, 205, $sf)
$fontSub = New-Object System.Drawing.Font('Arial', 8, [System.Drawing.FontStyle]::Regular)
$g.DrawString('MENU', $fontSub, $grayBrush, 225, 223, $sf)

# Footer text
$fontFooter = New-Object System.Drawing.Font('Arial', 10, [System.Drawing.FontStyle]::Regular)
$g.DrawString('Quet ma bang app Ngan hang hoac Vi dien tu', $fontFooter, $grayBrush, 225, 415, $sf)

$bmp.Save('d:\Smart_Menu\public\images\payment_qr.png', [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Output "Successfully saved d:\Smart_Menu\public\images\payment_qr.png"

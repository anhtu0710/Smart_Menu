Add-Type -AssemblyName System.Drawing

$file = "d:\Smart_Menu\backend\src\main\resources\menu-style\TRE_TRUNG\template_clean.png"
$bmp = [System.Drawing.Bitmap]::FromFile($file)
$g = [System.Drawing.Graphics]::FromImage($bmp)

$brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(236, 222, 206))

# 1. Tẩy chữ "M" sót ở x: 1095..1130, y: 120..160
$g.FillRectangle($brush, 1095, 120, 40, 45)

# 2. Tẩy chữ "...tích cực !!" ở x: 1060..1215, y: 700..780
$g.FillRectangle($brush, 1060, 700, 160, 80)

# 3. Tẩy số giá sót ở x: 1070..1160, y: 785..830
$g.FillRectangle($brush, 1070, 785, 95, 50)

$g.Dispose()
$brush.Dispose()

$tmp = "d:\Smart_Menu\backend\src\main\resources\menu-style\TRE_TRUNG\template_clean_fixed.png"
$bmp.Save($tmp, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

Move-Item -Path $tmp -Destination $file -Force
Write-Host "Completely cleaned M and tich cuc!!"

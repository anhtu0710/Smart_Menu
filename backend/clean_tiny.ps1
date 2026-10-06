Add-Type -AssemblyName System.Drawing

$file = "d:\Smart_Menu\backend\src\main\resources\menu-style\TRE_TRUNG\template_clean.png"
$bmp = [System.Drawing.Bitmap]::FromFile($file)
$g = [System.Drawing.Graphics]::FromImage($bmp)

# Tông màu nền kem tại vùng đó: RGB(236, 222, 206)
$brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(236, 222, 206))

# 1. Xóa chữ "M" nhỏ ở x=880..900, y=100..125
$g.FillRectangle($brush, 880, 100, 25, 25)

# 2. Xóa chữ "...tích cực !!" ở x=845..930, y=625..650
$g.FillRectangle($brush, 845, 625, 90, 25)

# 3. Xóa vệt hồng nhỏ ở x=595, y=405
$g.FillRectangle($brush, 595, 405, 15, 65)

$g.Dispose()
$brush.Dispose()

# Lưu lại ảnh đè lên chính nó
$tmp = "d:\Smart_Menu\backend\src\main\resources\menu-style\TRE_TRUNG\template_clean_fixed.png"
$bmp.Save($tmp, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

Move-Item -Path $tmp -Destination $file -Force
Write-Host "Cleaned remaining tiny text on template_clean.png"

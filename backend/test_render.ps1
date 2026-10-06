Add-Type -AssemblyName System.Drawing
$ref = [System.Drawing.Bitmap]::FromFile("d:/Smart_Menu/backend/src/main/resources/menu-style/SANG_TRONG/reference.png")
$g = [System.Drawing.Graphics]::FromImage($ref)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAlias

# Test soft blend
$rect = New-Object System.Drawing.Rectangle(530, 25, 545, 320)
$brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect, [System.Drawing.Color]::FromArgb(205, 150, 95), [System.Drawing.Color]::FromArgb(235, 218, 195), 0.0)
$g.FillRectangle($brush, $rect)

$fontHeader = New-Object System.Drawing.Font("Georgia", 13, [System.Drawing.FontStyle]::Bold)
$fontItem = New-Object System.Drawing.Font("Georgia", 10, [System.Drawing.FontStyle]::Bold)
$brushWhite = [System.Drawing.Brushes]::White
$brushBlack = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(28, 20, 15))

$g.DrawString("CÀ PHÊ VIỆT", $fontHeader, $brushWhite, 550, 45)
$g.DrawString("Cà phê sữa đá", $fontItem, $brushBlack, 550, 75)
$g.DrawString("37.000", $fontItem, $brushBlack, 730, 75)

$ref.Save("C:/Users/-ACER-/.gemini/antigravity-ide/brain/e43a02ec-d55f-4409-9da7-8e1a6212771f/test_luxury_preview.jpg")
$g.Dispose()
$ref.Dispose()
Write-Host "Created test_luxury_preview.jpg"

Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile("d:\Smart_Menu\backend\src\main\resources\menu-style\TRE_TRUNG\reference.png")
Write-Host "Width = $($img.Width), Height = $($img.Height)"
$img.Dispose()

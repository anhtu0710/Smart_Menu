Add-Type -AssemblyName System.Drawing
$bmp = [System.Drawing.Bitmap]::FromFile("d:/Smart_Menu/backend/src/main/resources/menu-style/TRUYEN_THONG/reference.png")
Write-Host ("Kich thuoc anh TRUYEN_THONG moi: " + $bmp.Width + " x " + $bmp.Height)
$bmp.Dispose()

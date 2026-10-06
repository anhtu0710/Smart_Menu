Add-Type -AssemblyName System.Drawing
$ref = [System.Drawing.Bitmap]::FromFile("d:\Smart_Menu\backend\src\main\resources\menu-style\TRE_TRUNG\reference.png")
$blank = [System.Drawing.Bitmap]::FromFile("d:\Smart_Menu\backend\test_blank_clean_v2.png")

$minX = 9999
$maxX = 0
$minY = 9999
$maxY = 0
$diffCount = 0

for ($y = 0; $y -lt $ref.Height; $y += 5) {
    for ($x = 0; $x -lt $ref.Width; $x += 5) {
        $c1 = $ref.GetPixel($x, $y)
        $c2 = $blank.GetPixel($x, $y)
        $diff = [Math]::Abs([int]$c1.R - [int]$c2.R) + [Math]::Abs([int]$c1.G - [int]$c2.G) + [Math]::Abs([int]$c1.B - [int]$c2.B)
        if ($diff -gt 30) {
            $diffCount++
            if ($x -lt $minX) { $minX = $x }
            if ($x -gt $maxX) { $maxX = $x }
            if ($y -lt $minY) { $minY = $y }
            if ($y -gt $maxY) { $maxY = $y }
        }
    }
}
Write-Host "Diff count: $diffCount"
Write-Host "Bounding Box of Diff: X: $minX..$maxX, Y: $minY..$maxY"
$ref.Dispose()
$blank.Dispose()

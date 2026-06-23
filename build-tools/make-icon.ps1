Add-Type -AssemblyName System.Drawing

# Draws the MediaGetter icon at an arbitrary size and returns a Bitmap.
function New-IconBitmap([int]$size) {
    $s = $size / 256.0
    $bmp = New-Object System.Drawing.Bitmap($size, $size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.Clear([System.Drawing.Color]::Transparent)

    # Rounded-rect background with a red vertical gradient.
    $radius = 48 * $s
    $rect = New-Object System.Drawing.RectangleF(0, 0, $size, $size)
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = 2 * $radius
    $path.AddArc($rect.X, $rect.Y, $d, $d, 180, 90)
    $path.AddArc($rect.Right - $d, $rect.Y, $d, $d, 270, 90)
    $path.AddArc($rect.Right - $d, $rect.Bottom - $d, $d, $d, 0, 90)
    $path.AddArc($rect.X, $rect.Bottom - $d, $d, $d, 90, 90)
    $path.CloseFigure()

    $top = [System.Drawing.Color]::FromArgb(255, 255, 90, 90)
    $bottom = [System.Drawing.Color]::FromArgb(255, 214, 40, 40)
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect, $top, $bottom, 90)
    $g.FillPath($brush, $path)

    # Download symbol (white): arrow shaft + head, over a tray base.
    $white = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::White, (22 * $s))
    $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round

    # Shaft
    $g.DrawLine($pen, [single](128 * $s), [single](66 * $s), [single](128 * $s), [single](150 * $s))

    # Arrow head (filled triangle)
    $head = @(
        (New-Object System.Drawing.PointF([single](90 * $s), [single](132 * $s))),
        (New-Object System.Drawing.PointF([single](166 * $s), [single](132 * $s))),
        (New-Object System.Drawing.PointF([single](128 * $s), [single](184 * $s)))
    )
    $g.FillPolygon($white, $head)

    # Tray (open box at the bottom)
    $g.DrawLine($pen, [single](78 * $s), [single](176 * $s), [single](78 * $s), [single](206 * $s))
    $g.DrawLine($pen, [single](178 * $s), [single](176 * $s), [single](178 * $s), [single](206 * $s))
    $g.DrawLine($pen, [single](78 * $s), [single](206 * $s), [single](178 * $s), [single](206 * $s))

    $g.Dispose()
    return $bmp
}

$sizes = @(16, 32, 48, 64, 128, 256)
$pngs = @()
foreach ($size in $sizes) {
    $bmp = New-IconBitmap $size
    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $pngs += , $ms.ToArray()
    $bmp.Dispose()
}

# Assemble a multi-resolution .ico file with PNG-compressed entries.
$out = New-Object System.IO.MemoryStream
$bw = New-Object System.IO.BinaryWriter($out)
$bw.Write([uint16]0)           # reserved
$bw.Write([uint16]1)           # type = icon
$bw.Write([uint16]$sizes.Count)

$offset = 6 + (16 * $sizes.Count)
for ($i = 0; $i -lt $sizes.Count; $i++) {
    $sz = $sizes[$i]
    $len = $pngs[$i].Length
    $bw.Write([byte]($(if ($sz -ge 256) { 0 } else { $sz })))  # width
    $bw.Write([byte]($(if ($sz -ge 256) { 0 } else { $sz })))  # height
    $bw.Write([byte]0)         # palette
    $bw.Write([byte]0)         # reserved
    $bw.Write([uint16]1)       # color planes
    $bw.Write([uint16]32)      # bits per pixel
    $bw.Write([uint32]$len)    # bytes in resource
    $bw.Write([uint32]$offset) # offset
    $offset += $len
}
foreach ($png in $pngs) { $bw.Write($png) }
$bw.Flush()

$target = Join-Path $PSScriptRoot "..\icons\MediaGetter.ico"
$dir = Split-Path $target
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
[System.IO.File]::WriteAllBytes((Resolve-Path $dir).Path + "\MediaGetter.ico", $out.ToArray())
Write-Output "Wrote $((Resolve-Path $dir).Path)\MediaGetter.ico ($($out.Length) bytes)"

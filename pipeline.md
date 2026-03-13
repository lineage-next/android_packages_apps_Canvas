# Canvas render pipeline

All pixel values are integers in **[0, 255]** per channel (R, G, B, A).

Clamp is defined as:
- `clamp(v)` = `max(0, min(255, v))` for integer RGB values
- `clamp(v)` = `max(0.0, min(1.0, v))` for normalized [0.0, 1.0] values

## Stage 1 - Pixel adjustments

Operates on every pixel of the **source bitmap** before anything else is applied.

Produces `sourceBitmapWithAdjustments`. **Do not modify the alpha channel.**

Adjustments are always applied in order: **contrast first, then brightness.**

### Stage 1a - Contrast

Operates directly on **RGB channels** in `[0, 255]`.

`value ∈ [-1.0, 1.0]` - negative reduces contrast, positive increases it.

Convert to a multiplier:

```
factor = (1.0 + value)
```

Per channel:

```
c′ = clamp(factor * (c - 128) + 128)
```

- `value = 0.0` -> `factor = 1.0` -> identity, no change
- `value = 1.0` -> `factor = 2.0` -> doubles deviation from mid-grey
- `value = -1.0` -> `factor = 0.0` -> all channels collapse to 128 (flat grey)

### Stage 1b - Brightness

Operates on **luminosity (L)** in HSL space to preserve hue and saturation.

Brightness shifts L in normalized `[0.0, 1.0]` space.

`value ∈ [-1.0, 1.0]` - negative darkens, positive lightens.

#### Adjust L

```
L′ = clamp(L + value, 0.0, 1.0)
```
#### Combined Stage 1 form (contrast then brightness)

```
// Stage 1a - RGB per channel
c′ = clamp(factor * (c - 128) + 128)

// Stage 1b - HSL
(H, S, L)    = rgbToHsl(R′, G′, B′)
L′           = clamp(L + value, 0.0, 1.0)
(R″, G″, B″) = hslToRgb(H, S, L′)
```

## Stage 2 - Rotation

Applied to `sourceBitmapWithAdjustments` after pixel adjustments.

Rotation is always **clockwise around the image center**, restricted to multiples of 90°.

### Action model

```kotlin
data class Rotate(
    val angleDegrees: Float
) : Transformation
```

No pivot is stored, it is always derived at render time from the current bitmap dimensions.

### Effective rotation

Walk the action list in insertion order, accumulating the total angle and tracking dimension swaps:

```kotlin
var width = sourceBitmap.width
var height = sourceBitmap.height
var totalAngle = 0f

for (action in actions) {
    when (action) {
        is Rotate -> {
            totalAngle = (totalAngle + action.angleDegrees) % 360f
            if (action.angleDegrees % 180f != 0f) {
                val tmp = width; width = height; height = tmp
            }
        }
        is Resize -> {
            width = action.rect.width
            height = action.rect.height
        }
        else -> {}
    }
}
```

The pivot at each rotation step is `(width / 2f, height / 2f)` **before** the dimension swap.

### Output dimensions

| Total angle | outWidth | outHeight |
|---|---|---|
| 0° / 180° | w | h |
| 90° / 270° | h | w |

### Draw call

```kotlin
rotate(
    degrees = totalAngle,
    pivot = Offset(sourceBitmap.width / 2f, sourceBitmap.height / 2f)
) {
    drawImage(sourceBitmapWithAdjustments)
}
```

## Stage 3 - Drawing layer

Creates a **new blank bitmap** at the **post-rotation dimensions**, fully transparent (`ARGB_8888`, all pixels `(0, 0, 0, 0)`).

Actions from the history list are replayed in **insertion order** (index 0 = oldest, index N = newest).

### Eraser pre-pass

Before drawing anything, collect all markers that have been erased:

```kotlin
val erasedMarkers = actions
    .filterIsInstance<Action.Drawing.Eraser>()
    .map { it.marker }
    .toSet()
```

### Per-action draw loop

```kotlin
actions.forEach { action ->
    when (action) {
        is Action.Drawing.Marker  -> if (action !in erasedMarkers) drawMarker(action)
        is Action.Drawing.Text    -> drawText(action)
        is Action.Drawing.Eraser  -> { /* already handled in pre-pass */ }
        else                      -> { /* handled in other stages */ }
    }
}
```

### Drawing coordinate transform

`Drawing` positions are authored in the coordinate space of the image **at the time the action was inserted**.

Each drawing action must carry a snapshot of the image dimensions at authoring time:

```kotlin
data class Text(
    val text: String,
    val position: IntOffset,
    val style: TextStyle,
    val imageSize: IntSize,  // dimensions when this action was authored
) : Drawing
```

Before drawing, transform `position` forward by the **total rotation accumulated after this action was inserted**.

For 90° increments around center `(w/2, h/2)`:

| Clockwise angle | x′ | y′ |
|---|---|---|
| 0° | `x` | `y` |
| 90° | `y` | `w - x` |
| 180° | `w - x` | `h - y` |
| 270° | `h - y` | `x` |

Where `w` and `h` are the `imageSize` stored in the action.

### Text positioning

Text is positioned at the **top-left corner** of the bounding box of the text, at the transformed coordinates above.

### Marker drawing

Marker stores a path in source-image pixel coordinates.

Apply the same coordinate transform per point before stroking.

No conversion needed if no rotation has occurred after the marker was authored.

## Stage 4 - Composite

Creates a **new bitmap** at post-rotation source dimensions with the same config.

Two `drawImage` calls in order (bottom -> top):

```kotlin
drawImage(rotatedSourceBitmap)   // 1. adjusted + rotated source
drawImage(drawingActionsBitmap)  // 2. drawing overlay
```

## Stage 5 - Crop

Only the **last `Action.Transformation.Resize`** in the action list is used.

Earlier resize actions are discarded.

```kotlin
val cropRect = actions
    .lastOrNull { it is Action.Transformation.Resize }
    ?.rect
    ?: compositeBitmap.size.toIntRect()  // fallback: full image
```

### Output dimensions

```
outWidth  = cropRect.right  - cropRect.left
outHeight = cropRect.bottom - cropRect.top
```

### Pixel mapping

Each output pixel `(x, y)` samples the composite at:

```
srcX = cropRect.left + x
srcY = cropRect.top  + y
```

```kotlin
drawImage(
    image     = compositeBitmap,
    srcOffset = IntOffset(cropRect.left, cropRect.top),
    srcSize   = IntSize(cropRect.width, cropRect.height),
    // dstOffset = (0, 0)  implicit
    // dstSize   = srcSize implicit -> 1:1, no scaling
)
```

## Full pipeline summary

```
sourceBitmap  (raw, from URI)
    │
    ▼  Stage 1a - Contrast (RGB)
    │  c′ = clamp(factor * (c - 128) + 128)   per R, G, B
    │
    ▼  Stage 1b - Brightness (HSL)
    │  L′ = clamp(L + value, 0.0, 1.0)
    │
    ▼  Stage 2 - Rotation
    │  clockwise around center, 90° increments
    │  dimensions swap on 90°/270°
    │
    ├──────────────────────────────────────┐
    │                                      │
    ▼  Stage 3 - Drawing layer             ▼  pass-through
    │  blank ARGB bitmap                   │  rotated + adjusted bitmap
    │  replay actions in order             │
    │  transform coords for post-rotation  │
    │  skip erased markers                 │
    │                                      │
    └──────────┬───────────────────────────┘
               │
               ▼  Stage 4 - Composite
               │  drawImage(rotated source) + drawImage(drawings)
               │
               ▼  Stage 5 - Crop
               │  slice [cropRect.left, cropRect.top, w, h]
               │  output pixel (x, y) ← src pixel (left+x, top+y)
               │
               ▼
          finalResultBitmap
```
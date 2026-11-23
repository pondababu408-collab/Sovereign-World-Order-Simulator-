package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CandlePoint
import com.example.model.Country
import com.example.model.PoliticalParty
import com.example.model.Province
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExecutivePortrait(
    seed: Int,
    name: String,
    accent: Color = AccentCyan,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val s = abs(seed + name.hashCode())
    val skinTones = listOf(
        Color(0xFFF1C27D), Color(0xFFE0AC69), Color(0xFFC68642),
        Color(0xFF8D5524), Color(0xFFFFDBAC)
    )
    val hairColors = listOf(
        Color(0xFF1A1A1A), Color(0xFF3B2F2F), Color(0xFF7B5E3B),
        Color(0xFF9EA7B0), Color(0xFF2B1B17)
    )
    val suitColors = listOf(
        Color(0xFF151E2E), Color(0xFF1C222B), Color(0xFF241B2F),
        Color(0xFF142823), Color(0xFF28181A)
    )
    val skin = skinTones[s % skinTones.size]
    val hair = hairColors[(s / 5) % hairColors.size]
    val suit = suitColors[(s / 7) % suitColors.size]
    val hasGlasses = (s % 3 == 0)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.5.dp, accent.copy(alpha = 0.7f), CircleShape)
            .background(OledElevated)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Subtle radial aura
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.25f), Color(0xFF0A0D12)),
                    center = Offset(w * 0.5f, h * 0.35f),
                    radius = w * 0.75f
                )
            )

            // Shoulders / Executive Suit
            val suitPath = Path().apply {
                moveTo(w * 0.12f, h * 1.05f)
                quadraticTo(w * 0.18f, h * 0.70f, w * 0.50f, h * 0.68f)
                quadraticTo(w * 0.82f, h * 0.70f, w * 0.88f, h * 1.05f)
                close()
            }
            drawPath(suitPath, suit)

            // Shirt V-neck & Tie
            val collarPath = Path().apply {
                moveTo(w * 0.39f, h * 0.71f)
                lineTo(w * 0.50f, h * 0.90f)
                lineTo(w * 0.61f, h * 0.71f)
                close()
            }
            drawPath(collarPath, Color(0xFFEBEFF5))
            drawLine(
                color = accent,
                start = Offset(w * 0.50f, h * 0.75f),
                end = Offset(w * 0.50f, h * 0.96f),
                strokeWidth = w * 0.055f
            )

            // Neck & Head
            drawRect(
                color = skin.copy(alpha = 0.9f),
                topLeft = Offset(w * 0.43f, h * 0.56f),
                size = Size(w * 0.14f, h * 0.17f)
            )
            drawOval(
                color = skin,
                topLeft = Offset(w * 0.30f, h * 0.22f),
                size = Size(w * 0.40f, h * 0.44f)
            )

            // Hair crown
            val hairPath = Path().apply {
                moveTo(w * 0.29f, h * 0.40f)
                quadraticTo(w * 0.28f, h * 0.16f, w * 0.50f, h * 0.17f)
                quadraticTo(w * 0.72f, h * 0.16f, w * 0.71f, h * 0.40f)
                quadraticTo(w * 0.58f, h * 0.27f, w * 0.50f, h * 0.29f)
                quadraticTo(w * 0.40f, h * 0.27f, w * 0.29f, h * 0.40f)
                close()
            }
            drawPath(hairPath, hair)

            // Eyes
            drawCircle(Color(0xFF181818), radius = w * 0.025f, center = Offset(w * 0.42f, h * 0.43f))
            drawCircle(Color(0xFF181818), radius = w * 0.025f, center = Offset(w * 0.58f, h * 0.43f))

            if (hasGlasses) {
                drawRect(
                    color = accent.copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.35f, h * 0.39f),
                    size = Size(w * 0.12f, h * 0.08f),
                    style = Stroke(width = w * 0.02f)
                )
                drawRect(
                    color = accent.copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.53f, h * 0.39f),
                    size = Size(w * 0.12f, h * 0.08f),
                    style = Stroke(width = w * 0.02f)
                )
                drawLine(accent, Offset(w * 0.47f, h * 0.43f), Offset(w * 0.53f, h * 0.43f), strokeWidth = w * 0.02f)
            }
        }
    }
}

@Composable
fun CorporateLogo(
    ticker: String,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val palette = when (ticker) {
        "BTC" -> AccentBitcoin
        "NVDA" -> AccentGreen
        "AAPL" -> TextPrimary
        "RELIANCE", "TCS" -> AccentGold
        "LMT", "AIR" -> AccentCyan
        "BBVA", "SAN" -> AccentRed
        "MC" -> AccentPurple
        else -> AccentCyan
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.copy(alpha = 0.14f))
            .border(1.dp, palette.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ticker.take(3),
            color = palette,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
fun StockChartCanvas(
    candles: List<CandlePoint>,
    accent: Color = AccentCyan,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF06080C))
            .border(1.dp, OledBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        if (candles.isEmpty()) return@Canvas
        val minP = candles.minOf { it.low } * 0.995
        val maxP = candles.maxOf { it.high } * 1.005
        val range = (maxP - minP).coerceAtLeast(0.01)

        // Horizontal gridlines
        for (i in 0..3) {
            val y = size.height * (i / 3f)
            drawLine(
                color = OledBorder.copy(alpha = 0.55f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        val stepX = size.width / candles.size.coerceAtLeast(1)
        val bodyW = (stepX * 0.56f).coerceIn(3f, 16f)

        val linePath = Path()
        candles.forEachIndexed { idx, c ->
            val cx = idx * stepX + stepX / 2f
            fun yFor(p: Double) = size.height - (((p - minP) / range).toFloat() * size.height)

            val yHigh = yFor(c.high)
            val yLow = yFor(c.low)
            val yOpen = yFor(c.open)
            val yClose = yFor(c.close)

            val bull = c.close >= c.open
            val barColor = if (bull) AccentGreen else AccentRed

            // Wick
            drawLine(barColor, Offset(cx, yHigh), Offset(cx, yLow), strokeWidth = 1.6f)
            // Body
            val top = minOf(yOpen, yClose)
            val h = abs(yClose - yOpen).coerceAtLeast(2.5f)
            drawRect(
                color = barColor,
                topLeft = Offset(cx - bodyW / 2f, top),
                size = Size(bodyW, h)
            )

            if (idx == 0) linePath.moveTo(cx, yClose) else linePath.lineTo(cx, yClose)
        }

        drawPath(
            path = linePath,
            color = accent.copy(alpha = 0.65f),
            style = Stroke(width = 1.8f)
        )
    }
}

@Composable
fun ParliamentHemicycleCanvas(
    parties: List<PoliticalParty>,
    modifier: Modifier = Modifier
) {
    val partyColors = listOf(AccentCyan, AccentGold, AccentRed, AccentGreen, AccentPurple)
    val seats = remember(parties) {
        val list = mutableListOf<Color>()
        val total = parties.sumOf { it.seatsCount }.coerceAtLeast(1)
        parties.forEachIndexed { idx, p ->
            val col = partyColors[idx % partyColors.size]
            val count = ((p.seatsCount.toFloat() / total) * 60f).toInt().coerceAtLeast(1)
            repeat(count) { list.add(col) }
        }
        list.take(60)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(145.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF07090D))
            .border(1.dp, OledBorder, RoundedCornerShape(8.dp))
    ) {
        val cx = size.width / 2f
        val cy = size.height * 0.88f
        val rows = 3
        val baseRadius = size.height * 0.36f
        val rowGap = size.height * 0.19f
        var seatIdx = 0

        for (r in 0 until rows) {
            val radius = baseRadius + r * rowGap
            val seatsInRow = 16 + r * 4
            for (s in 0 until seatsInRow) {
                if (seatIdx >= seats.size) break
                val angle = PI + (PI * (s + 0.5) / seatsInRow)
                val x = cx + (radius * cos(angle)).toFloat()
                val y = cy + (radius * sin(angle)).toFloat()
                drawCircle(
                    color = seats[seatIdx],
                    radius = 4.6f,
                    center = Offset(x, y)
                )
                seatIdx++
            }
        }
    }
}

/**
 * Interactive dark-ocean provincial World Map with continental vector coastlines,
 * clickable provinces/enclaves, pan/zoom gestures, and bilateral relation color coding.
 */
@Composable
fun WorldMapCanvas(
    countries: List<Country>,
    provinces: List<Province>,
    selectedProvinceId: String?,
    homeCountryIso: String,
    playerRegionId: String,
    onProvinceTapped: (Province) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1.15f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val countryByIso = remember(countries) { countries.associateBy { it.iso } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(310.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MapOcean)
            .border(1.dp, OledBorder, RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1.0f, 4.5f)
                    offsetX = (offsetX + pan.x).coerceIn(-size.width * 0.8f, size.width * 0.8f)
                    offsetY = (offsetY + pan.y).coerceIn(-size.height * 0.8f, size.height * 0.8f)
                }
            }
            .pointerInput(provinces, scale, offsetX, offsetY) {
                detectTapGestures { tap ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val cx = w / 2f
                    val cy = h / 2f
                    // Convert tap back to normalized map space
                    val normX = ((tap.x - offsetX - cx) / scale + cx) / w
                    val normY = ((tap.y - offsetY - cy) / scale + cy) / h

                    val nearest = provinces.minByOrNull { p ->
                        val dx = p.mapX - normX
                        val dy = p.mapY - normY
                        dx * dx + dy * dy
                    }
                    if (nearest != null) {
                        val dx = nearest.mapX - normX
                        val dy = nearest.mapY - normY
                        if ((dx * dx + dy * dy) < (0.045f / scale)) {
                            onProvinceTapped(nearest)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            fun toScreen(nx: Float, ny: Float): Offset {
                val x = (nx * w - cx) * scale + cx + offsetX
                val y = (ny * h - cy) * scale + cy + offsetY
                return Offset(x, y)
            }

            // Lat/Lon Graticule
            for (i in 1..7) {
                val p1 = toScreen(i / 8f, 0f)
                val p2 = toScreen(i / 8f, 1f)
                drawLine(MapGrid, p1, p2, strokeWidth = 1f)
            }
            for (j in 1..5) {
                val p1 = toScreen(0f, j / 6f)
                val p2 = toScreen(1f, j / 6f)
                drawLine(MapGrid, p1, p2, strokeWidth = 1f)
            }

            // Vector Continental Coastlines (North America, South America, Europe, Africa, Asia, Australia, Japan, UK, Greenland)
            val landmasses = listOf(
                // North America
                listOf(0.08f to 0.14f, 0.25f to 0.10f, 0.33f to 0.16f, 0.29f to 0.28f, 0.25f to 0.34f, 0.26f to 0.42f, 0.24f to 0.45f, 0.20f to 0.39f, 0.16f to 0.30f, 0.12f to 0.22f),
                // Greenland
                listOf(0.35f to 0.05f, 0.44f to 0.04f, 0.42f to 0.13f, 0.36f to 0.14f),
                // South America
                listOf(0.27f to 0.46f, 0.35f to 0.48f, 0.40f to 0.56f, 0.36f to 0.68f, 0.32f to 0.80f, 0.29f to 0.72f, 0.27f to 0.56f),
                // Europe
                listOf(0.47f to 0.28f, 0.48f to 0.18f, 0.54f to 0.13f, 0.60f to 0.14f, 0.61f to 0.25f, 0.56f to 0.29f, 0.51f to 0.28f),
                // UK & Ireland
                listOf(0.47f to 0.19f, 0.50f to 0.18f, 0.50f to 0.23f, 0.47f to 0.22f),
                // Africa
                listOf(0.47f to 0.32f, 0.58f to 0.31f, 0.63f to 0.42f, 0.60f to 0.56f, 0.57f to 0.69f, 0.53f to 0.66f, 0.51f to 0.50f, 0.45f to 0.44f),
                // Eurasia / Asia
                listOf(0.60f to 0.13f, 0.78f to 0.10f, 0.90f to 0.16f, 0.86f to 0.32f, 0.82f to 0.40f, 0.76f to 0.46f, 0.72f to 0.45f, 0.69f to 0.35f, 0.62f to 0.38f, 0.59f to 0.28f),
                // Japan
                listOf(0.86f to 0.27f, 0.89f to 0.28f, 0.87f to 0.34f, 0.85f to 0.32f),
                // Maritime SE Asia
                listOf(0.78f to 0.48f, 0.85f to 0.49f, 0.87f to 0.55f, 0.79f to 0.54f),
                // Australia
                listOf(0.81f to 0.61f, 0.91f to 0.60f, 0.92f to 0.72f, 0.83f to 0.72f, 0.80f to 0.66f),
                // New Zealand
                listOf(0.94f to 0.70f, 0.97f to 0.72f, 0.95f to 0.77f)
            )

            landmasses.forEach { poly ->
                val path = Path()
                poly.forEachIndexed { idx, (nx, ny) ->
                    val pt = toScreen(nx, ny)
                    if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                path.close()
                drawPath(path, Color(0xFF141D2B))
                drawPath(path, Color(0xFF283A54), style = Stroke(width = 1.4f))
            }

            // Draw Province & Enclave Nodes
            provinces.forEach { p ->
                val country = countryByIso[p.countryIso]
                val rel = country?.bilateralRelation ?: 0
                val pt = toScreen(p.mapX, p.mapY)
                val isSelected = p.id == selectedProvinceId
                val isPlayerLoc = p.id == playerRegionId
                val isHome = p.countryIso == homeCountryIso

                val nodeColor = when {
                    isHome -> AccentCyan
                    rel >= 40 -> AccentGreen
                    rel <= -30 -> AccentRed
                    p.isMicroStateOrEnclave -> AccentGold
                    else -> Color(0xFF93A5C0)
                }
                val baseRadius = if (p.isMicroStateOrEnclave) 3.4f else 4.2f
                val r = (baseRadius * (0.85f + scale * 0.25f)).coerceAtMost(9f)

                if (isSelected || isPlayerLoc) {
                    drawCircle(
                        color = (if (isPlayerLoc) AccentGold else AccentCyan).copy(alpha = 0.28f),
                        radius = r * 2.2f,
                        center = pt
                    )
                    drawCircle(
                        color = if (isPlayerLoc) AccentGold else AccentCyan,
                        radius = r * 1.4f,
                        center = pt,
                        style = Stroke(width = 1.8f)
                    )
                }

                drawCircle(
                    color = nodeColor,
                    radius = r,
                    center = pt
                )
            }
        }

        // Overlay legend
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(OledBlack.copy(alpha = 0.8f))
                .border(1.dp, OledBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot("Home", AccentCyan)
            LegendDot("Allied", AccentGreen)
            LegendDot("Hostile", AccentRed)
            LegendDot("Enclave", AccentGold)
            Text("Pinch/Drag • Tap Node", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

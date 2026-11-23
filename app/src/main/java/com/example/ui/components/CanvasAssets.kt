package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorldGeoPolygons
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

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.25f), Color(0xFF0A0D12)),
                    center = Offset(w * 0.5f, h * 0.35f),
                    radius = w * 0.75f
                )
            )

            val suitPath = Path().apply {
                moveTo(w * 0.12f, h * 1.05f)
                quadraticTo(w * 0.18f, h * 0.70f, w * 0.50f, h * 0.68f)
                quadraticTo(w * 0.82f, h * 0.70f, w * 0.88f, h * 1.05f)
                close()
            }
            drawPath(suitPath, suit)

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

            val hairPath = Path().apply {
                moveTo(w * 0.29f, h * 0.40f)
                quadraticTo(w * 0.28f, h * 0.16f, w * 0.50f, h * 0.17f)
                quadraticTo(w * 0.72f, h * 0.16f, w * 0.71f, h * 0.40f)
                quadraticTo(w * 0.58f, h * 0.27f, w * 0.50f, h * 0.29f)
                quadraticTo(w * 0.40f, h * 0.27f, w * 0.29f, h * 0.40f)
                close()
            }
            drawPath(hairPath, hair)

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

            drawLine(barColor, Offset(cx, yHigh), Offset(cx, yLow), strokeWidth = 1.6f)
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
 * Interactive GeoJSON-based World Map with dark-navy ocean (#081018), authentic geographic proportions,
 * crisp outlined country labels, multi-pass glowing green (#00E676) selection border, layer switcher,
 * pinch-to-zoom + drag panning, and floating zoom/focus buttons.
 */
@Composable
fun WorldMapCanvas(
    countries: List<Country>,
    provinces: List<Province>,
    selectedProvinceId: String?,
    selectedCountryIso: String,
    homeCountryIso: String,
    playerRegionId: String,
    focusTrigger: Int = 0,
    onProvinceTapped: (Province) -> Unit,
    onCountryTapped: (String) -> Unit,
    onSearchClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val geoPolygons = remember { WorldGeoPolygons.loadFromAssets(context) }

    var scale by remember { mutableFloatStateOf(1.18f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var initializedCenter by remember { mutableStateOf(false) }
    var layerMode by remember { mutableIntStateOf(0) } // 0: Political, 1: Bilateral Relations, 2: Power Grid

    val countryByIso = remember(countries) { countries.associateBy { it.iso } }
    val layerNames = listOf("Political", "Diplomacy", "Energy Grid")

    val strokePaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            style = Paint.Style.STROKE
            strokeWidth = 4.2f
            color = android.graphics.Color.rgb(6, 12, 18)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    val fillPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
            color = android.graphics.Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MapOcean)
            .border(1.dp, OledBorder, RoundedCornerShape(12.dp))
    ) {
        val boxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val boxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val worldW = maxOf(boxW, boxH * 1.92f)
        val worldH = worldW / 1.92f
        val minScale = (boxW / worldW).coerceIn(0.45f, 0.95f)

        fun centerOnNorm(nx: Float, ny: Float, targetScale: Float = scale) {
            val clampedScale = targetScale.coerceIn(minScale, 6.5f)
            scale = clampedScale
            val maxPanX = worldW * clampedScale * 0.55f
            val maxPanY = worldH * clampedScale * 0.55f
            offsetX = (-(nx - 0.5f) * worldW * clampedScale).coerceIn(-maxPanX, maxPanX)
            offsetY = (-(ny - 0.5f) * worldH * clampedScale).coerceIn(-maxPanY, maxPanY)
        }

        // Center initially on player's home region
        LaunchedEffect(boxW, boxH, playerRegionId) {
            if (!initializedCenter && boxW > 1f && boxH > 1f) {
                val homePoly = geoPolygons.firstOrNull { it.iso == homeCountryIso }
                if (homePoly != null) {
                    centerOnNorm(
                        WorldGeoPolygons.lonToNormX(homePoly.labelLon),
                        WorldGeoPolygons.latToNormY(homePoly.labelLat),
                        1.25f
                    )
                } else {
                    val homeProv = provinces.firstOrNull { it.id == playerRegionId }
                    if (homeProv != null) centerOnNorm(homeProv.mapX, homeProv.mapY, 1.25f)
                }
                initializedCenter = true
            }
        }

        // Focus camera on searched / quick-switched country
        LaunchedEffect(focusTrigger) {
            if (focusTrigger > 0 && boxW > 1f && boxH > 1f) {
                val targetPoly = geoPolygons.firstOrNull { it.iso == selectedCountryIso }
                if (targetPoly != null) {
                    centerOnNorm(
                        WorldGeoPolygons.lonToNormX(targetPoly.labelLon),
                        WorldGeoPolygons.latToNormY(targetPoly.labelLat),
                        maxOf(scale, 1.85f)
                    )
                } else {
                    val targetProv = provinces.firstOrNull { it.countryIso == selectedCountryIso }
                    if (targetProv != null) centerOnNorm(targetProv.mapX, targetProv.mapY, maxOf(scale, 1.85f))
                }
            }
        }

        val gestureModifier = Modifier
            .fillMaxSize()
            .pointerInput(worldW, worldH, minScale) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    val newScale = (oldScale * zoom).coerceIn(minScale, 6.5f)
                    val zoomFactor = newScale / oldScale
                    val cx = boxW / 2f
                    val cy = boxH / 2f

                    // Anchor zoom smoothly around gesture centroid + apply pan
                    val dxFromCenter = centroid.x - cx - offsetX
                    val dyFromCenter = centroid.y - cy - offsetY
                    val nextOffsetX = centroid.x - cx - dxFromCenter * zoomFactor + pan.x
                    val nextOffsetY = centroid.y - cy - dyFromCenter * zoomFactor + pan.y

                    val maxPanX = worldW * newScale * 0.55f
                    val maxPanY = worldH * newScale * 0.55f
                    scale = newScale
                    offsetX = nextOffsetX.coerceIn(-maxPanX, maxPanX)
                    offsetY = nextOffsetY.coerceIn(-maxPanY, maxPanY)
                }
            }
            .pointerInput(geoPolygons, provinces, worldW, worldH) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        val nextScale = if (scale < 2.2f) (scale * 1.85f).coerceAtMost(6.5f) else 1.25f
                        val cx = boxW / 2f
                        val cy = boxH / 2f
                        val nx = ((tap.x - cx - offsetX) / scale) / worldW + 0.5f
                        val ny = ((tap.y - cy - offsetY) / scale) / worldH + 0.5f
                        centerOnNorm(nx.coerceIn(0f, 1f), ny.coerceIn(0f, 1f), nextScale)
                    },
                    onTap = { tap ->
                        val cx = boxW / 2f
                        val cy = boxH / 2f
                        val normX = ((tap.x - cx - offsetX) / scale) / worldW + 0.5f
                        val normY = ((tap.y - cy - offsetY) / scale) / worldH + 0.5f

                        // 1. Check GeoJSON country polygons first
                        val hitPoly = geoPolygons.firstOrNull { poly ->
                            poly.rings.any { ring -> WorldGeoPolygons.pointInRing(normX, normY, ring) }
                        }
                        if (hitPoly != null) {
                            onCountryTapped(hitPoly.iso)
                        } else {
                            // 2. Fallback nearest province marker / microstate
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
                )
            }

        Canvas(modifier = gestureModifier) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            fun toScreen(nx: Float, ny: Float): Offset {
                val x = (nx - 0.5f) * worldW * scale + cx + offsetX
                val y = (ny - 0.5f) * worldH * scale + cy + offsetY
                return Offset(x, y)
            }

            // Subtle dark-navy ocean depth vignette
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0C1826), MapOcean),
                    center = Offset(cx, cy),
                    radius = maxOf(w, h) * 0.85f
                )
            )

            // Lat/Lon Graticule
            for (i in 0..12) {
                val p1 = toScreen(i / 12f, 0f)
                val p2 = toScreen(i / 12f, 1f)
                drawLine(MapGrid.copy(alpha = 0.65f), p1, p2, strokeWidth = 1f)
            }
            for (j in 0..8) {
                val p1 = toScreen(0f, j / 8f)
                val p2 = toScreen(1f, j / 8f)
                drawLine(MapGrid.copy(alpha = 0.65f), p1, p2, strokeWidth = 1f)
            }
            // Subtle Equator guide line
            val eqY = WorldGeoPolygons.latToNormY(0f)
            drawLine(
                color = AccentCyan.copy(alpha = 0.14f),
                start = toScreen(0f, eqY),
                end = toScreen(1f, eqY),
                strokeWidth = 1.4f
            )

            fun buildRingPath(ring: List<Pair<Float, Float>>): Path {
                val path = Path()
                ring.forEachIndexed { idx, (lon, lat) ->
                    val pt = toScreen(WorldGeoPolygons.lonToNormX(lon), WorldGeoPolygons.latToNormY(lat))
                    if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                path.close()
                return path
            }

            // Pass 1: Draw all country polygons and standard coastline borders
            geoPolygons.forEach { poly ->
                val c = countryByIso[poly.iso]
                val isHome = poly.iso == homeCountryIso

                val baseFill = when (layerMode) {
                    1 -> {
                        val rel = c?.bilateralRelation ?: 0
                        when {
                            isHome -> Color(0xFF154555)
                            rel >= 35 -> Color(0xFF14422C)
                            rel <= -25 -> Color(0xFF4A1A22)
                            else -> Color(0xFF252D38)
                        }
                    }
                    2 -> {
                        val prov = provinces.firstOrNull { it.countryIso == poly.iso }
                        val ratio = if (prov != null && prov.powerDemandMw > 0) prov.powerSupplyMw.toFloat() / prov.powerDemandMw else 1f
                        if (ratio >= 1f) Color(0xFF193C38) else Color(0xFF4A2C1B)
                    }
                    else -> poly.fillColor
                }

                poly.rings.forEach { ring ->
                    val path = buildRingPath(ring)
                    drawPath(path, baseFill)
                    drawPath(path, Color(0xFF3E526B), style = Stroke(width = 1.25f))
                }
            }

            // Pass 2: Top-layer glowing neon-green (#00E676) border for the selected country
            val selectedPoly = geoPolygons.firstOrNull { it.iso == selectedCountryIso }
            selectedPoly?.rings?.forEach { ring ->
                val path = buildRingPath(ring)
                drawPath(path, AccentGreen.copy(alpha = 0.14f))
                drawPath(path, AccentGreen.copy(alpha = 0.16f), style = Stroke(width = 14f))
                drawPath(path, AccentGreen.copy(alpha = 0.36f), style = Stroke(width = 7f))
                drawPath(path, AccentGreen, style = Stroke(width = 2.6f))
            }

            // Pass 3: Capital / Province markers
            provinces.forEach { p ->
                val pt = toScreen(p.mapX, p.mapY)
                if (pt.x in -16f..(w + 16f) && pt.y in -16f..(h + 16f)) {
                    val isPlayerLoc = p.id == playerRegionId
                    val isSelectedProv = p.id == selectedProvinceId
                    val r = when {
                        isPlayerLoc -> 5.5f
                        p.isMicroStateOrEnclave -> 3.0f
                        else -> 2.8f
                    }
                    if (isPlayerLoc || isSelectedProv) {
                        drawCircle(
                            color = (if (isPlayerLoc) AccentRed else AccentGreen).copy(alpha = 0.36f),
                            radius = r * 2.2f,
                            center = pt
                        )
                    }
                    drawCircle(
                        color = if (isPlayerLoc) AccentRed else if (isSelectedProv) AccentGreen else Color(0xFFCBD5E1),
                        radius = r,
                        center = pt
                    )
                }
            }

            // Pass 4: Crisp outlined white country labels
            val labelTextSize = (22f * (0.76f + scale * 0.14f)).coerceIn(18f, 34f)
            strokePaint.textSize = labelTextSize
            fillPaint.textSize = labelTextSize

            val majorSet = setOf("RU", "CN", "IN", "US", "BR", "CA", "AU", "FR", "ES", "KZ", "DE", "AR", "SA", "ZA", "GL", "MX", "ID", "EG", "GB", "JP")
            geoPolygons.forEach { poly ->
                val pt = toScreen(
                    WorldGeoPolygons.lonToNormX(poly.labelLon),
                    WorldGeoPolygons.latToNormY(poly.labelLat)
                )
                if (pt.x in 16f..(w - 16f) && pt.y in 16f..(h - 16f)) {
                    val isSelected = poly.iso == selectedCountryIso
                    val isHome = poly.iso == homeCountryIso
                    if (poly.iso in majorSet || scale >= 1.65f || isSelected || isHome) {
                        fillPaint.color = if (isSelected) android.graphics.Color.rgb(160, 255, 200) else android.graphics.Color.WHITE
                        drawContext.canvas.nativeCanvas.drawText(poly.displayLabel, pt.x, pt.y, strokePaint)
                        drawContext.canvas.nativeCanvas.drawText(poly.displayLabel, pt.x, pt.y, fillPaint)
                    }
                }
            }
        }

        // Top-left active layer badge
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(OledBlack.copy(alpha = 0.84f))
                .border(1.dp, OledBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentGreen))
            Text(
                text = "GeoJSON • ${layerNames[layerMode]} (${String.format("%.1fx", scale)})",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
        }

        // Floating right-side circular map controls (Search, +, -, Red Location Pin, Globe Layer Toggle)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MapFloatingCircleButton(
                icon = Icons.Filled.Search,
                contentDescription = "Search Country",
                tint = TextPrimary,
                testTag = "map_search_btn",
                onClick = onSearchClicked
            )
            MapFloatingCircleButton(
                icon = Icons.Filled.Add,
                contentDescription = "Zoom In",
                tint = TextPrimary,
                testTag = "map_zoom_in_btn",
                onClick = {
                    val next = (scale * 1.30f).coerceAtMost(6.5f)
                    val ratio = next / scale
                    scale = next
                    val maxPanX = worldW * next * 0.55f
                    val maxPanY = worldH * next * 0.55f
                    offsetX = (offsetX * ratio).coerceIn(-maxPanX, maxPanX)
                    offsetY = (offsetY * ratio).coerceIn(-maxPanY, maxPanY)
                }
            )
            MapFloatingCircleButton(
                icon = Icons.Filled.Remove,
                contentDescription = "Zoom Out",
                tint = TextPrimary,
                testTag = "map_zoom_out_btn",
                onClick = {
                    val next = (scale / 1.30f).coerceAtLeast(minScale)
                    val ratio = next / scale
                    scale = next
                    val maxPanX = worldW * next * 0.55f
                    val maxPanY = worldH * next * 0.55f
                    offsetX = (offsetX * ratio).coerceIn(-maxPanX, maxPanX)
                    offsetY = (offsetY * ratio).coerceIn(-maxPanY, maxPanY)
                }
            )
            MapFloatingCircleButton(
                icon = Icons.Filled.LocationOn,
                contentDescription = "Center on Home Capital",
                tint = AccentRed,
                testTag = "map_home_pin_btn",
                onClick = {
                    val homeProv = provinces.firstOrNull { it.id == playerRegionId }
                    val homePoly = geoPolygons.firstOrNull { it.iso == homeCountryIso }
                    if (homeProv != null) {
                        val nx = homePoly?.let { WorldGeoPolygons.lonToNormX(it.labelLon) } ?: homeProv.mapX
                        val ny = homePoly?.let { WorldGeoPolygons.latToNormY(it.labelLat) } ?: homeProv.mapY
                        centerOnNorm(nx, ny, 2.1f)
                        onProvinceTapped(homeProv)
                    } else {
                        centerOnNorm(0.5f, 0.45f, 1.15f)
                    }
                }
            )
            MapFloatingCircleButton(
                icon = Icons.Filled.Public,
                contentDescription = "Switch Map Layer",
                tint = AccentCyan,
                testTag = "map_layer_btn",
                onClick = { layerMode = (layerMode + 1) % layerNames.size }
            )
        }
    }
}

@Composable
private fun MapFloatingCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .testTag(testTag)
            .size(44.dp)
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .background(Color(0xE60D0D0D))
            .border(1.dp, OledBorderHighlight, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}


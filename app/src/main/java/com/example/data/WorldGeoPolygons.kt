package com.example.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class CountryGeoPolygon(
    val iso: String,
    val displayLabel: String,
    val labelLon: Float,
    val labelLat: Float,
    val fillColor: Color,
    val rings: List<List<Pair<Float, Float>>> // exterior coastline rings of (lon, lat)
)

@JsonClass(generateAdapter = true)
data class GeoJsonFeatureCollection(
    val type: String = "FeatureCollection",
    val features: List<GeoJsonFeature> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GeoJsonFeature(
    val type: String = "Feature",
    val properties: GeoJsonCountryProps = GeoJsonCountryProps(),
    val geometry: GeoJsonGeometry = GeoJsonGeometry()
)

@JsonClass(generateAdapter = true)
data class GeoJsonCountryProps(
    val iso: String = "",
    val name: String = "",
    val labelLon: Double = 0.0,
    val labelLat: Double = 0.0,
    val color: String = "#263945"
)

@JsonClass(generateAdapter = true)
data class GeoJsonGeometry(
    val type: String = "Polygon",
    val coordinates: List<Any> = emptyList()
)

object WorldGeoPolygons {

    private val moshi: Moshi by lazy {
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    }

    @Volatile
    private var cachedFromAsset: List<CountryGeoPolygon>? = null

    fun lonToNormX(lon: Float): Float = ((lon + 180f) / 360f).coerceIn(0f, 1f)
    fun latToNormY(lat: Float): Float = ((84f - lat) / 142f).coerceIn(0f, 1f)

    fun pointInRing(x: Float, y: Float, ring: List<Pair<Float, Float>>): Boolean {
        var inside = false
        var j = ring.size - 1
        for (i in ring.indices) {
            val xi = lonToNormX(ring[i].first)
            val yi = latToNormY(ring[i].second)
            val xj = lonToNormX(ring[j].first)
            val yj = latToNormY(ring[j].second)
            val intersect = ((yi > y) != (yj > y)) &&
                (x < (xj - xi) * (y - yi) / ((yj - yi).let { if (it == 0f) 1e-6f else it }) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    fun loadFromAssets(context: Context): List<CountryGeoPolygon> {
        cachedFromAsset?.let { return it }
        return runCatching {
            val raw = context.assets.open("world_countries.geojson").bufferedReader().use { it.readText() }
            val parsed = parseGeoJson(raw)
            if (parsed.isNotEmpty()) {
                cachedFromAsset = parsed
                parsed
            } else {
                polygons
            }
        }.getOrElse { polygons }
    }

    @Suppress("UNCHECKED_CAST")
    fun parseGeoJson(rawJson: String): List<CountryGeoPolygon> {
        val adapter = moshi.adapter(GeoJsonFeatureCollection::class.java)
        val collection = adapter.fromJson(rawJson) ?: return emptyList()
        return collection.features.mapNotNull { feature ->
            val props = feature.properties
            if (props.iso.isBlank()) return@mapNotNull null

            val exteriorRings = mutableListOf<List<Pair<Float, Float>>>()
            when (feature.geometry.type) {
                "Polygon" -> {
                    val outerRing = feature.geometry.coordinates.firstOrNull() as? List<*>
                    parseCoordinateRing(outerRing)?.let { if (it.size >= 3) exteriorRings.add(it) }
                }
                "MultiPolygon" -> {
                    feature.geometry.coordinates.forEach { polyObj ->
                        val poly = polyObj as? List<*>
                        val outerRing = poly?.firstOrNull() as? List<*>
                        parseCoordinateRing(outerRing)?.let { if (it.size >= 3) exteriorRings.add(it) }
                    }
                }
            }
            if (exteriorRings.isEmpty()) return@mapNotNull null

            CountryGeoPolygon(
                iso = props.iso,
                displayLabel = props.name,
                labelLon = props.labelLon.toFloat(),
                labelLat = props.labelLat.toFloat(),
                fillColor = parseHexColor(props.color),
                rings = exteriorRings
            )
        }
    }

    private fun parseCoordinateRing(rawRing: List<*>?): List<Pair<Float, Float>>? {
        if (rawRing == null) return null
        return rawRing.mapNotNull { pt ->
            val coords = pt as? List<*> ?: return@mapNotNull null
            val lon = (coords.getOrNull(0) as? Number)?.toFloat() ?: return@mapNotNull null
            val lat = (coords.getOrNull(1) as? Number)?.toFloat() ?: return@mapNotNull null
            lon to lat
        }
    }

    private fun parseHexColor(hex: String): Color {
        val cleaned = hex.removePrefix("#")
        val rgb = cleaned.toLongOrNull(16) ?: 0x263945L
        return Color(0xFF000000L or rgb)
    }

    // Immediate in-memory GeoJSON fallback for headless JVM unit tests & zero-delay first frame
    val polygons: List<CountryGeoPolygon> = listOf(
        CountryGeoPolygon(
            "CA", "Canada", -104f, 58f, Color(0xFF263945),
            listOf(listOf(-141f to 60.2f, -141f to 69.6f, -125f to 70.2f, -95f to 69.5f, -79f to 53f, -64f to 60.3f, -55.7f to 52.2f, -67f to 45.1f, -75f to 45f, -84.5f to 46.5f, -95.1f to 49f, -123.1f to 49f, -132.5f to 55f))
        ),
        CountryGeoPolygon(
            "GL", "Greenland", -42f, 72f, Color(0xFF22313D),
            listOf(listOf(-70f to 76.5f, -55f to 81.5f, -32f to 83.2f, -17.5f to 79.5f, -22f to 70.5f, -43.5f to 60f, -51.5f to 64.2f))
        ),
        CountryGeoPolygon(
            "US", "United States", -99f, 39f, Color(0xFF1E3648),
            listOf(
                listOf(-124.7f to 48.4f, -95.1f to 49f, -84f to 46.2f, -75f to 45f, -67f to 45.1f, -70f to 41.6f, -75.5f to 38f, -80.1f to 25.8f, -82.7f to 28f, -89.2f to 29.1f, -97.2f to 26f, -106.5f to 31.8f, -117.1f to 32.5f, -122.5f to 37.8f),
                listOf(-168f to 65.6f, -156.5f to 71.3f, -141f to 69.6f, -141f to 60.2f, -154f to 58f, -162f to 55.2f)
            )
        ),
        CountryGeoPolygon(
            "MX", "Mexico", -102f, 23f, Color(0xFF283D35),
            listOf(listOf(-117.1f to 32.5f, -106.5f to 31.8f, -97.2f to 26f, -96.1f to 19.2f, -86.8f to 21.2f, -92.2f to 14.6f, -104.5f to 19f, -110f to 23f))
        ),
        CountryGeoPolygon(
            "CU", "Cuba", -79f, 21.5f, Color(0xFF412A34),
            listOf(listOf(-84.9f to 21.9f, -82f to 23.1f, -74.2f to 20.3f, -75.8f to 19.9f))
        ),
        CountryGeoPolygon(
            "CO", "Colombia", -73f, 4f, Color(0xFF2D3D30),
            listOf(listOf(-77.8f to 8.5f, -71.3f to 12.2f, -67f to 5.5f, -70f to -0.5f, -79f to 1.6f))
        ),
        CountryGeoPolygon(
            "VE", "Venezuela", -66f, 7f, Color(0xFF3C2E2B),
            listOf(listOf(-71.3f to 12.2f, -61.8f to 10.7f, -60f to 8f, -65.5f to 1.5f, -72.3f to 9f))
        ),
        CountryGeoPolygon(
            "BR", "Brazil", -51f, -11f, Color(0xFF1E3E33),
            listOf(listOf(-67f to 1.5f, -51.5f to 4.2f, -35f to -5.8f, -40.5f to -21.5f, -53.4f to -33.7f, -58f to -18f, -65f to -11f, -73.8f to -7.3f))
        ),
        CountryGeoPolygon(
            "PE", "Peru", -75f, -10f, Color(0xFF373145),
            listOf(listOf(-81.3f to -4.5f, -75.5f to -0.5f, -69f to -12.5f, -69.6f to -17.5f, -76.2f to -14f))
        ),
        CountryGeoPolygon(
            "BO", "Bolivia", -64f, -17f, Color(0xFF3D3A2B),
            listOf(listOf(-69.5f to -11f, -58f to -17.5f, -62.6f to -22.3f, -69.6f to -17.5f))
        ),
        CountryGeoPolygon(
            "CL", "Chile", -71f, -35f, Color(0xFF342A3E),
            listOf(listOf(-70.3f to -18.3f, -67.5f to -24f, -71.5f to -39.5f, -75f to -52.5f, -67.5f to -54.8f, -74.2f to -42f))
        ),
        CountryGeoPolygon(
            "AR", "Argentina", -65f, -36f, Color(0xFF213847),
            listOf(listOf(-67.5f to -22.8f, -54f to -26f, -58.2f to -34.5f, -65f to -44.5f, -68.5f to -54.8f, -73f to -46f, -70f to -32.5f))
        ),
        CountryGeoPolygon(
            "GB", "United Kingdom", -2f, 54f, Color(0xFF372B3E),
            listOf(listOf(-5.8f to 58.5f, -1.8f to 57.5f, 1.7f to 52.8f, -1.5f to 50.7f, -5.6f to 50.1f))
        ),
        CountryGeoPolygon(
            "IE", "Ireland", -8.2f, 53.2f, Color(0xFF223F31),
            listOf(listOf(-10.2f to 54.3f, -6f to 54.1f, -6.2f to 52.1f, -9.8f to 51.5f))
        ),
        CountryGeoPolygon(
            "PT", "Portugal", -8.2f, 39.5f, Color(0xFF2A4035),
            listOf(listOf(-8.9f to 42f, -6.5f to 41.8f, -7.4f to 37.1f, -9.5f to 38.8f))
        ),
        CountryGeoPolygon(
            "ES", "Spain", -4f, 40.3f, Color(0xFF423626),
            listOf(listOf(-9.2f to 43.1f, 3.3f to 42.4f, -0.4f to 38.3f, -5.6f to 36f, -7.4f to 37.1f))
        ),
        CountryGeoPolygon(
            "FR", "France", 2.3f, 46.5f, Color(0xFF22364C),
            listOf(listOf(-4.8f to 48.4f, 2.5f to 51.1f, 8.2f to 49f, 7.5f to 43.8f, 3.3f to 42.4f, -1.8f to 43.4f))
        ),
        CountryGeoPolygon(
            "DE", "Germany", 10.4f, 51.1f, Color(0xFF333642),
            listOf(listOf(6f to 51f, 8.4f to 54.9f, 14.2f to 53.9f, 15f to 51f, 12.8f to 47.6f, 7.6f to 47.6f))
        ),
        CountryGeoPolygon(
            "IT", "Italy", 12.6f, 42.8f, Color(0xFF26423B),
            listOf(listOf(6.8f to 45.8f, 13.8f to 45.8f, 18.5f to 40.1f, 15.6f to 37.9f, 12f to 42f))
        ),
        CountryGeoPolygon(
            "PL", "Poland", 19.3f, 52.1f, Color(0xFF3D2C34),
            listOf(listOf(14.2f to 53.9f, 19.5f to 54.4f, 23.8f to 53.6f, 22.6f to 49.1f, 14.8f to 50.9f))
        ),
        CountryGeoPolygon(
            "NO", "Norway", 9.5f, 62f, Color(0xFF243344),
            listOf(listOf(5f to 59f, 6.5f to 63.2f, 22f to 70.5f, 30.5f to 70.2f, 13.5f to 63.5f, 11.5f to 59f))
        ),
        CountryGeoPolygon(
            "SE", "Sweden", 16f, 62f, Color(0xFF283A4E),
            listOf(listOf(11.5f to 59f, 13.5f to 63.5f, 20.5f to 69f, 24.1f to 65.8f, 16f to 56.2f, 12.8f to 55.4f))
        ),
        CountryGeoPolygon(
            "FI", "Finland", 26f, 64f, Color(0xFF2F3E46),
            listOf(listOf(21f to 60.2f, 24.1f to 65.8f, 28f to 70f, 31.4f to 63f, 27.8f to 60.5f))
        ),
        CountryGeoPolygon(
            "UA", "Ukraine", 31.2f, 49f, Color(0xFF2E3F4F),
            listOf(listOf(22.2f to 48.5f, 24f to 51.5f, 40.2f to 49.2f, 34f to 44.5f, 28.2f to 46.5f))
        ),
        CountryGeoPolygon(
            "RU", "Russia", 92f, 62f, Color(0xFF243A3B),
            listOf(listOf(28f to 60f, 33f to 69.5f, 60f to 70f, 100f to 76.5f, 178f to 66.5f, 162f to 55f, 135f to 44f, 118f to 49.5f, 88f to 49.5f, 62f to 54f, 46.5f to 45.5f, 38.2f to 47f, 31.5f to 51.6f))
        ),
        CountryGeoPolygon(
            "KZ", "Kazakhstan", 67f, 48f, Color(0xFF2C3F4D),
            listOf(listOf(46.5f to 45.5f, 50f to 50f, 76f to 54.5f, 86.5f to 49.2f, 74f to 42.5f, 55f to 41.2f))
        ),
        CountryGeoPolygon(
            "MN", "Mongolia", 103f, 46.8f, Color(0xFF3B362D),
            listOf(listOf(88f to 49f, 100f to 51.5f, 119.5f to 48f, 105f to 41.8f, 95f to 43f))
        ),
        CountryGeoPolygon(
            "TR", "Turkey", 35f, 39f, Color(0xFF3A2C34),
            listOf(listOf(26.4f to 41.6f, 42.5f to 41.5f, 44.5f to 39f, 36f to 36.2f, 28f to 36.6f))
        ),
        CountryGeoPolygon(
            "IR", "Iran", 54f, 32.5f, Color(0xFF303B38),
            listOf(listOf(44.5f to 39f, 54f to 37.2f, 61f to 35.5f, 61.5f to 25.2f, 51f to 29f))
        ),
        CountryGeoPolygon(
            "SA", "Saudi Arabia", 45f, 24f, Color(0xFF3E3828),
            listOf(listOf(35f to 29.2f, 46f to 29.1f, 55.2f to 23.5f, 52f to 19f, 43.5f to 17f))
        ),
        CountryGeoPolygon(
            "PK", "Pakistan", 69f, 30f, Color(0xFF233B2E),
            listOf(listOf(61.5f to 25.2f, 74.5f to 36.8f, 76f to 34.5f, 68f to 24f))
        ),
        CountryGeoPolygon(
            "IN", "India", 79f, 22f, Color(0xFF29413D),
            listOf(listOf(73.5f to 35.5f, 81f to 30.5f, 88.2f to 27.8f, 97.2f to 28.2f, 88.5f to 21.8f, 77.5f to 8.1f, 68.2f to 23.6f))
        ),
        CountryGeoPolygon(
            "CN", "China", 104f, 35.5f, Color(0xFF3E292E),
            listOf(listOf(74f to 39.5f, 86.5f to 49.2f, 116f to 43.5f, 121f to 52.8f, 134.8f to 48.3f, 122f to 31f, 114.2f to 22.2f, 102f to 22f, 88.2f to 27.8f, 76f to 34.5f))
        ),
        CountryGeoPolygon(
            "JP", "Japan", 138f, 36.5f, Color(0xFF2C3A4E),
            listOf(listOf(131f to 34f, 140.8f to 35.6f, 145.5f to 43.4f, 141.5f to 45.4f, 136.8f to 37f))
        ),
        CountryGeoPolygon(
            "KR", "South Korea", 128f, 36.2f, Color(0xFF273E4A),
            listOf(listOf(126.2f to 38f, 128.6f to 38.4f, 129.4f to 35.1f, 126.4f to 34.5f))
        ),
        CountryGeoPolygon(
            "ID", "Indonesia", 114f, -2f, Color(0xFF2B3D3C),
            listOf(
                listOf(95.3f to 5.5f, 105.9f to -5.8f, 103.8f to -6.1f, 97.5f to 1.5f),
                listOf(109f to 1.2f, 117.5f to 3.5f, 117.6f to -3.5f, 110.2f to -3f)
            )
        ),
        CountryGeoPolygon(
            "DZ", "Algeria", 3f, 28f, Color(0xFF2A3D36),
            listOf(listOf(-2f to 35.1f, 8.6f to 36.9f, 11.6f to 24f, 4.2f to 19.1f, -8.7f to 27.3f))
        ),
        CountryGeoPolygon(
            "EG", "Egypt", 30f, 26.5f, Color(0xFF3F3728),
            listOf(listOf(25.1f to 31.6f, 34.2f to 31.2f, 36.8f to 22f, 25f to 22f))
        ),
        CountryGeoPolygon(
            "NG", "Nigeria", 8f, 9.5f, Color(0xFF253F35),
            listOf(listOf(2.8f to 13.5f, 14.1f to 13.1f, 13f to 6.2f, 2.8f to 6.5f))
        ),
        CountryGeoPolygon(
            "CD", "DR Congo", 23.5f, -2.5f, Color(0xFF293B3D),
            listOf(listOf(12.2f to -5.8f, 18f to 4.2f, 29.8f to 4f, 30.4f to -11.2f, 17f to -7.5f))
        ),
        CountryGeoPolygon(
            "ZA", "South Africa", 24f, -29f, Color(0xFF263B3D),
            listOf(listOf(16.5f to -28.5f, 31.5f to -22.2f, 32.8f to -26.8f, 28.2f to -33f, 18.3f to -34f))
        ),
        CountryGeoPolygon(
            "AU", "Australia", 134f, -25f, Color(0xFF3D342B),
            listOf(listOf(113.5f to -22f, 129f to -14f, 142.5f to -10.8f, 153.6f to -28.2f, 150f to -37.5f, 134f to -33f, 115f to -34.4f))
        ),
        CountryGeoPolygon(
            "NZ", "New Zealand", 172.5f, -41.5f, Color(0xFF263947),
            listOf(listOf(172.8f to -34.5f, 178.4f to -37.7f, 175.3f to -41.5f, 167.8f to -46.6f))
        )
    )
}

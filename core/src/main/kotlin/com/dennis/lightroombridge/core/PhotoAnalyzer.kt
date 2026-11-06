package com.dennis.lightroombridge.core

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Deliberately conservative, offline, deterministic first-pass JPEG/HEIC analysis. */
data class PhotoStats(
    val sampledPixels: Int,
    val lumaP05: Double,
    val lumaP50: Double,
    val lumaP95: Double,
    val highlightClippedFraction: Double,
    val darkFraction: Double,
    val greenFraction: Double,
    val intenseGreenFraction: Double,
    val neutralSamples: Int,
    val notes: List<String>
)

data class SuggestedEdit(val settings: Map<String, String>, val explanation: List<String>) {
    fun display(name: String): String = settings[name] ?: "—"
}

object PhotoAnalyzer {
    fun analyze(pixels: IntArray, width: Int, height: Int): Pair<PhotoStats, SuggestedEdit> {
        require(width > 0 && height > 0 && pixels.size.toLong() >= width.toLong() * height.toLong()) {
            "Pixel buffer is smaller than declared dimensions"
        }
        val stride = max(1, kotlin.math.sqrt((width.toDouble() * height / 42000.0)).toInt())
        val histogram = IntArray(256)
        var count = 0
        var clipped = 0
        var dark = 0
        var greens = 0
        var intenseGreens = 0
        var neutrals = 0
        for (y in 0 until height step stride) {
            for (x in 0 until width step stride) {
                val px = pixels[y * width + x]
                val r = (px ushr 16 and 255) / 255.0
                val g = (px ushr 8 and 255) / 255.0
                val b = (px and 255) / 255.0
                val lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
                histogram[(lum * 255).toInt().coerceIn(0,255)]++
                count++
                if (lum > 0.985 && max(r,max(g,b)) > 0.985) clipped++
                if (lum < 0.13) dark++
                val range = max(r, max(g,b)) - min(r,min(g,b))
                val green = g > r * 1.08 && g > b * 1.12 && g > 0.12
                if (green) {
                    greens++
                    if (range > 0.35 && g > 0.46) intenseGreens++
                }
                if (range < 0.065 && lum > 0.22 && lum < 0.9) neutrals++
            }
        }
        fun pct(fraction: Double): Double {
            val target = (count * fraction).toInt()
            var found = 0
            for (i in histogram.indices) {
                found += histogram[i]
                if (found >= target) return i / 255.0
            }
            return 1.0
        }
        val p05=pct(0.05); val p50=pct(0.50); val p95=pct(0.95)
        val contrastSpan = p95-p05
        val greenShare=greens.toDouble()/count
        val intenseShare=intenseGreens.toDouble()/count
        val clipping=clipped.toDouble()/count
        val darkShare=dark.toDouble()/count
        val notes = mutableListOf<String>()
        if (p50 < .34) notes += "Overall image is dark; mild exposure lift recommended."
        if (p50 > .62) notes += "Overall image is bright; avoid lifting exposure further."
        if (p95 > .89) notes += "Bright regions may benefit from lower highlights."
        if (p05 < .11) notes += "Dark regions are deep; preserve black point while opening some shadows."
        if (contrastSpan < .47) notes += "Tonal range is compressed; add modest contrast."
        if (greenShare > .22) notes += "Dominant foliage/grass; avoid fluorescent greens."
        if (notes.isEmpty()) notes += "Tone is relatively balanced; keep adjustment restrained."
        val ev = (((.45 - p50) * 1.20).coerceIn(-.45, .48) * 100).roundToInt()/100.0
        val highlights = when {
            p95 > .94 -> -38
            p95 > .86 -> -27
            p95 > .78 -> -17
            else -> -8
        } + if (clipping > .015) -9 else 0
        val shadows = when {
            p05 < .065 -> 29
            p05 < .12 -> 22
            p05 < .20 -> 14
            else -> 7
        } + if (darkShare > .15) 5 else 0
        val contrast = if (contrastSpan < .46) 15 else if (contrastSpan < .62) 10 else 6
        val greenSat = if (intenseShare > .12) -11 else if (greenShare > .22) -7 else -2
        val yellowSat = if (greenShare > .22) -6 else -1
        val settings = linkedMapOf(
            "Exposure2012" to (if (ev >= 0) "+%.2f".format(java.util.Locale.US, ev) else "%.2f".format(java.util.Locale.US, ev)),
            "Contrast2012" to "$contrast",
            "Highlights2012" to "$highlights",
            "Shadows2012" to "${shadows.coerceAtMost(34)}",
            "Whites2012" to (if (p95 > .92) "-5" else "+5"),
            "Blacks2012" to (if (p05 < .07) "-4" else "-9"),
            "Texture" to "+5",
            "Clarity2012" to "+4",
            "Dehaze" to "+2",
            "Vibrance" to (if (intenseShare > .12) "+2" else "+7"),
            "Saturation" to "-2",
            "SaturationAdjustmentGreen" to "$greenSat",
            "SaturationAdjustmentYellow" to "$yellowSat",
            "SaturationAdjustmentAqua" to "-3",
            "HueAdjustmentGreen" to (if (greenShare > .20) "-3" else "0"),
            "Sharpness" to "17",
            "SharpenRadius" to "1.0",
            "SharpenDetail" to "18",
            "SharpenEdgeMasking" to "65",
            "LuminanceSmoothing" to "6",
            "ColorNoiseReduction" to "20"
        )
        val stats = PhotoStats(count,p05,p50,p95,clipping,darkShare,greenShare,intenseShare,neutrals,notes)
        val explanation = listOf(
            "Exposure ${settings["Exposure2012"]} EV; highlights ${settings["Highlights2012"]}; shadows +${settings["Shadows2012"]}.",
            "Foliage-aware green saturation $greenSat; restrained sharpening to avoid JPEG halos.",
            "Original remains unchanged. Lightroom will render the actual adjustment."
        )
        return stats to SuggestedEdit(settings,explanation)
    }
}

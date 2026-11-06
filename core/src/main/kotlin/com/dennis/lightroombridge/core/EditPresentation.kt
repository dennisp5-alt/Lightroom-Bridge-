package com.dennis.lightroombridge.core

import java.util.Locale

/**
 * Honest, explainable labels derived from measured luminance and channel
 * statistics. These are NOT object/scene recognition labels.
 */
object EditPresentation {
    data class Label(val title: String, val explanation: String)

    fun label(stats: PhotoStats): Label {
        val greens = stats.greenFraction > .22
        val dark = stats.lumaP50 < .34
        val bright = stats.lumaP95 > .90
        val flat = stats.lumaP95 - stats.lumaP05 < .48
        val shadowy = stats.lumaP05 < .14

        val first = when {
            greens -> "Natural Greens"
            dark -> "Gentle Brightening"
            bright -> "Highlight Control"
            flat -> "Clean Contrast"
            else -> "Natural Colour"
        }
        val second = when {
            shadowy -> "Shadow Lift"
            bright -> "Highlight Recovery"
            flat -> "Depth Boost"
            dark -> "Soft Contrast"
            else -> "Balanced Depth"
        }
        val message = buildList {
            if (greens) add("Green-heavy image: restrain green and yellow saturation.")
            if (shadowy) add("Dark regions: lift shadows without flattening the black point.")
            if (bright) add("Bright regions: reduce highlights to protect detail.")
            if (flat) add("Compressed contrast: add modest tonal separation.")
            if (isEmpty()) add("Balanced image: keep the adjustments subtle and natural.")
        }.joinToString(" ")
        return Label("$first • $second", message)
    }

    private val friendly = mapOf(
        "Exposure2012" to "Exposure",
        "Contrast2012" to "Contrast",
        "Highlights2012" to "Highlights",
        "Shadows2012" to "Shadows",
        "Whites2012" to "Whites",
        "Blacks2012" to "Blacks",
        "Texture" to "Texture",
        "Clarity2012" to "Clarity",
        "Dehaze" to "Dehaze",
        "Vibrance" to "Vibrance",
        "Saturation" to "Saturation",
        "SaturationAdjustmentGreen" to "Green saturation (Mix)",
        "SaturationAdjustmentYellow" to "Yellow saturation (Mix)",
        "SaturationAdjustmentAqua" to "Aqua saturation (Mix)",
        "HueAdjustmentGreen" to "Green hue (Mix)",
        "Sharpness" to "Sharpening amount",
        "SharpenRadius" to "Sharpen radius",
        "SharpenDetail" to "Sharpen detail",
        "SharpenEdgeMasking" to "Sharpen masking",
        "LuminanceSmoothing" to "Noise reduction (luminance)",
        "ColorNoiseReduction" to "Noise reduction (colour)"
    )
    private val help = mapOf(
        "Exposure2012" to "overall brightness",
        "Contrast2012" to "tonal separation",
        "Highlights2012" to "reduce bright-area intensity",
        "Shadows2012" to "open dark regions",
        "Whites2012" to "brightest tones",
        "Blacks2012" to "darkest tones",
        "Texture" to "fine surface detail",
        "Clarity2012" to "mid-tone definition",
        "Dehaze" to "gentle atmospheric contrast",
        "Vibrance" to "subtle colour enhancement",
        "Saturation" to "overall colour intensity",
        "SaturationAdjustmentGreen" to "restrain overly bright foliage",
        "SaturationAdjustmentYellow" to "reduce yellow cast in grass",
        "SaturationAdjustmentAqua" to "restrain cyan colour shifts",
        "HueAdjustmentGreen" to "slight green hue correction",
        "Sharpness" to "conservative sharpening",
        "SharpenRadius" to "sharpening edge width",
        "SharpenDetail" to "avoid brittle microdetail",
        "SharpenEdgeMasking" to "protect smooth areas from halos",
        "LuminanceSmoothing" to "soften luminance noise",
        "ColorNoiseReduction" to "reduce coloured speckles"
    )
    private val light = listOf("Exposure2012", "Contrast2012", "Highlights2012",
        "Shadows2012", "Whites2012", "Blacks2012")
    private val colour = listOf("Vibrance", "Saturation", "SaturationAdjustmentGreen",
        "SaturationAdjustmentYellow", "SaturationAdjustmentAqua", "HueAdjustmentGreen")
    private val effects = listOf("Texture", "Clarity2012", "Dehaze")
    private val detail = listOf("Sharpness", "SharpenRadius", "SharpenDetail",
        "SharpenEdgeMasking", "LuminanceSmoothing", "ColorNoiseReduction")

    fun settingsText(edit: SuggestedEdit): String {
        val blocks = listOf("LIGHT" to light, "COLOUR & MIX" to colour,
            "EFFECTS" to effects, "DETAIL" to detail)
        return blocks.joinToString("\n\n") { (heading, keys) ->
            val rows = keys.mapNotNull { key ->
                val value = edit.settings[key] ?: return@mapNotNull null
                val shown = if (key == "Exposure2012") "$value EV" else value
                "${friendly[key] ?: key}: $shown — ${help[key] ?: "adjustment"}"
            }
            "$heading\n" + rows.joinToString("\n")
        }
    }

    fun checklist(stats: PhotoStats, edit: SuggestedEdit, photoName: String): String {
        val ident = label(stats)
        return listOf(
            "DENNIS LIGHTROOM BRIDGE · GUIDED EDIT",
            "Photo: $photoName",
            "Look: ${ident.title}",
            ident.explanation,
            "",
            "These are TARGET slider values, not changes to add on top of your current edits.",
            "Existing Lightroom settings may need resetting or comparing individually.",
            "",
            settingsText(edit),
            "",
            "Original photo unchanged; this text does not control Lightroom."
        ).joinToString("\n")
    }

    fun presetTitle(stats: PhotoStats): String = "DLB • ${label(stats).title}"

    fun fileName(stats: PhotoStats, photoName: String): String {
        val part = label(stats).title
            .replace("•", "_")
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(50)
        val photo = photoName.substringBeforeLast('.',photoName)
            .replace(Regex("[^a-zA-Z0-9_-]"), "_").take(25)
            .ifBlank { "Photo" }
        return "DLB_${part}_${photo}.xmp"
    }
}

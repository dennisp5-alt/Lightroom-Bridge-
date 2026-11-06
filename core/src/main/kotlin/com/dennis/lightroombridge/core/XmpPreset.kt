package com.dennis.lightroombridge.core

import java.util.UUID

/**
 * Exports a portable Camera Raw / Lightroom develop PRESET, not an XMP sidecar.
 * The envelope mirrors Adobe-authored preset examples: x:xmpmeta -> rdf:RDF ->
 * rdf:Description containing crs metadata and Name/Group rdf:Alt elements.
 * Intentionally omit xpacket PI/BOM and camera-profile overrides for compatibility.
 */
object XmpPreset {
    private val allowed = setOf(
        "Exposure2012", "Contrast2012", "Highlights2012", "Shadows2012",
        "Whites2012", "Blacks2012", "Texture", "Clarity2012", "Dehaze",
        "Vibrance", "Saturation", "SaturationAdjustmentGreen", "SaturationAdjustmentYellow",
        "SaturationAdjustmentAqua", "HueAdjustmentGreen", "Sharpness", "SharpenRadius",
        "SharpenDetail", "SharpenEdgeMasking", "LuminanceSmoothing", "ColorNoiseReduction"
    )
    private fun xmlText(value: String): String = value.replace("&", "&amp;")
        .replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        .replace("'", "&apos;")
    fun generate(
        name: String,
        edit: SuggestedEdit,
        group: String = "Dennis Lightroom Bridge",
        uuid: UUID = UUID.randomUUID()
    ): String {
        require(name.isNotBlank() && name.length <= 120) { "Preset needs a name" }
        require(group.isNotBlank() && group.length <= 120) { "Preset needs a group" }
        require(edit.settings.isNotEmpty()) { "Preset needs develop adjustments" }
        require(edit.settings.keys.all { it in allowed }) { "Unrecognised Camera Raw develop key" }
        require(edit.settings.values.all { it.matches(Regex("[+-]?[0-9]+(\\.[0-9]+)?")) }) {
            "Invalid develop value"
        }
        val settings = edit.settings.entries.joinToString("\n") {
            "   crs:${it.key}=\"${xmlText(it.value)}\""
        }
        val id = uuid.toString().replace("-", "").uppercase(java.util.Locale.ROOT)
        return """<x:xmpmeta xmlns:x="adobe:ns:meta/">
 <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
  <rdf:Description rdf:about=""
   xmlns:crs="http://ns.adobe.com/camera-raw-settings/1.0/"
   crs:PresetType="Normal"
   crs:Cluster=""
   crs:UUID="$id"
   crs:SupportsAmount="False"
   crs:SupportsColor="True"
   crs:SupportsMonochrome="True"
   crs:SupportsHighDynamicRange="True"
   crs:SupportsNormalDynamicRange="True"
   crs:SupportsSceneReferred="True"
   crs:SupportsOutputReferred="True"
   crs:CameraModelRestriction=""
   crs:Copyright=""
   crs:ContactInfo=""
   crs:Version="15.4"
   crs:ProcessVersion="11.0"
   crs:HasSettings="True"
$settings>
   <crs:Name><rdf:Alt><rdf:li xml:lang="x-default">${xmlText(name)}</rdf:li></rdf:Alt></crs:Name>
   <crs:Group><rdf:Alt><rdf:li xml:lang="x-default">${xmlText(group)}</rdf:li></rdf:Alt></crs:Group>
   <crs:Description><rdf:Alt><rdf:li xml:lang="x-default">Photo-specific, natural-colour auto edit</rdf:li></rdf:Alt></crs:Description>
  </rdf:Description>
 </rdf:RDF>
</x:xmpmeta>
"""
    }
}

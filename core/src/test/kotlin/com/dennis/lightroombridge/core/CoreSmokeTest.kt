package com.dennis.lightroombridge.core

import java.util.UUID
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource
import java.io.StringReader

fun main() {
    val neutral=IntArray(100*100) { 0xFF888888.toInt() }
    val (stats, edit) = PhotoAnalyzer.analyze(neutral,100,100)
    check(stats.sampledPixels > 0)
    check(edit.settings["Exposure2012"] != null)
    val xml = XmpPreset.generate("A & B <landscape>",edit,uuid=UUID.fromString("00000000-0000-0000-0000-000000000001"))
    val parser=DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true)
        setFeature("http://apache.org/xml/features/disallow-doctype-decl",true)
    }.newDocumentBuilder()
    val doc=parser.parse(InputSource(StringReader(xml)))
    val desc=doc.getElementsByTagNameNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#","Description").item(0)
    check(desc.attributes.getNamedItemNS("http://ns.adobe.com/camera-raw-settings/1.0/","PresetType").nodeValue == "Normal")
    check(doc.documentElement.textContent.contains("A & B <landscape>"))
    check(xml.contains("00000000000000000000000000000001"))
    check(xml.startsWith("<x:xmpmeta")) { "Must not prepend Unicode BOM or packet wrapper" }
    check(!xml.contains("<?xpacket")) { "Avoid legacy XMP packet wrapper" }
    val crs = "http://ns.adobe.com/camera-raw-settings/1.0/"
    check(desc.attributes.getNamedItemNS(crs,"Cluster").nodeValue == "")
    check(desc.attributes.getNamedItemNS(crs,"SupportsSceneReferred").nodeValue == "True")
    check(desc.attributes.getNamedItemNS(crs,"HasSettings").nodeValue == "True")
    check(desc.attributes.getNamedItemNS(crs,"Exposure2012") != null)
    check(doc.getElementsByTagNameNS(crs, "Name").length == 1)
    check(doc.getElementsByTagNameNS(crs, "Group").length == 1)
    check(xml.toByteArray(Charsets.UTF_8)[0] == '<'.code.toByte())

    val darkGreen=IntArray(100*100) { i ->
        if (i % 7 == 0) 0xFF101010.toInt() else 0xFF267A25.toInt()
    }
    val (greenStats, greenEdit)=PhotoAnalyzer.analyze(darkGreen,100,100)
    val name=EditPresentation.label(greenStats)
    check(name.title.contains("Natural Greens")) { "Look must explain green-heavy scene" }
    check(EditPresentation.presetTitle(greenStats).startsWith("DLB"))
    check(EditPresentation.fileName(greenStats,"IMG_1234.jpg").endsWith(".xmp"))
    val guide=EditPresentation.checklist(greenStats,greenEdit,"IMG_1234.jpg")
    check(guide.contains("NO PRESET") || guide.contains("GUIDED EDIT"))
    check(guide.contains("Exposure") && guide.contains("Green saturation"))
    check(guide.contains("TARGET slider values"))
    println("PASS: core histograms, constrained settings and well-formed XMP metadata")
}

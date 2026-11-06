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
    println("PASS: core histograms, constrained settings and well-formed XMP metadata")
}

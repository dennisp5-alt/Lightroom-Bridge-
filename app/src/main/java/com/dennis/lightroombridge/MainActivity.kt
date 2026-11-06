package com.dennis.lightroombridge

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.dennis.lightroombridge.core.PhotoAnalyzer
import com.dennis.lightroombridge.core.PhotoStats
import com.dennis.lightroombridge.core.SuggestedEdit
import com.dennis.lightroombridge.core.XmpPreset
import java.util.concurrent.Executors
import kotlin.math.max

/** v0.1 local-first only. Never accesses Lightroom's private databases. */
class MainActivity : Activity() {
    private data class PickedPhoto(val uri: Uri, val title: String)
    private val photos = mutableListOf<PickedPhoto>()
    private val cache = mutableMapOf<Uri, Pair<PhotoStats, SuggestedEdit>>()
    private var selected = 0
    private var loadSequence = 0
    private val worker = Executors.newSingleThreadExecutor()
    private var pendingPreset: Pair<String,String>? = null

    private val bg = Color.rgb(12,19,29)
    private val panel = Color.rgb(24,36,52)
    private val mint = Color.rgb(145,230,210)
    private val white = Color.rgb(238,246,252)
    private val muted = Color.rgb(164,181,199)
    private lateinit var hero: ImageView
    private lateinit var filename: TextView
    private lateinit var counter: TextView
    private lateinit var status: TextView
    private lateinit var metrics: TextView
    private lateinit var settings: TextView

    companion object {
        private const val PICK_PHOTOS=1101
        private const val SAVE_PRESET=1102
        private const val EXPORT_FOLDER=1103
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildUi()
        status.text = "Choose photographs to start. No files leave your device."
    }

    private fun dp(n: Int): Int = (n*resources.displayMetrics.density+.5f).toInt()
    private fun style(radius: Int = 16, fill: Int = panel, border: Int? = null): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill); cornerRadius=dp(radius).toFloat()
            if (border != null) setStroke(dp(1),border)
        }
    }
    private fun label(text: String,size: Float,color: Int=white,bold: Boolean=false): TextView = TextView(this).apply {
        this.text=text; textSize=size; setTextColor(color)
        if(bold) setTypeface(null,android.graphics.Typeface.BOLD)
    }
    private fun button(title: String, primary: Boolean = false, action: ()->Unit): TextView {
        return label(title,14f,if(primary) bg else white,true).apply {
            gravity=Gravity.CENTER
            minHeight=dp(50)
            setPadding(dp(12),dp(10),dp(12),dp(10))
            background=style(13,if(primary) mint else panel,if(primary) null else Color.rgb(62,83,104))
            setOnClickListener { action() }
        }
    }
    private fun LinearLayout.addBlock(view: View, top: Int = 0, bottom: Int = 0) {
        val lp=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.topMargin=dp(top); lp.bottomMargin=dp(bottom)
        addView(view,lp)
    }
    private fun row(vararg views: View): LinearLayout = LinearLayout(this).apply {
        orientation=LinearLayout.HORIZONTAL
        views.forEachIndexed { i, v ->
            val lp=LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f)
            if (i>0) lp.leftMargin=dp(8)
            addView(v,lp)
        }
    }
    private fun buildUi() {
        val scroll=ScrollView(this).apply { setBackgroundColor(bg); isFillViewport=true }
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(24),dp(20),dp(32)) }
        scroll.addView(root); setContentView(scroll)
        root.addBlock(label("DENNIS  /  PHOTO SYSTEMS",11f,mint,true),bottom=8)
        root.addBlock(label("Lightroom Bridge",29f,white,true),bottom=4)
        root.addBlock(label("PHOTO-SPECIFIC XMP  •  OFFLINE ENGINE v0.1",11f,muted),bottom=16)
        root.addBlock(button("SELECT PHOTOS",true) { choosePhotos() },bottom=10)
        root.addBlock(row(
            button("PREVIOUS") { navigate(-1) },
            button("NEXT") { navigate(1) }
        ),bottom=12)
        val heroFrame=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; background=style(18); setPadding(dp(6),dp(6),dp(6),dp(6)) }
        hero=ImageView(this).apply {
            setBackgroundColor(Color.rgb(19,28,40)); scaleType=ImageView.ScaleType.FIT_CENTER
            contentDescription="Selected photo preview"
        }
        heroFrame.addView(hero,LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(270)))
        root.addBlock(heroFrame)
        filename=label("No photograph selected",15f,white,true)
        counter=label("0 / 0",12f,muted)
        root.addBlock(filename,top=14)
        root.addBlock(counter,top=3)
        status=label("",13f,mint)
        root.addBlock(status,top=12)
        root.addBlock(label("IMAGE ASSESSMENT",11f,muted,true),top=23,bottom=8)
        metrics=label("No analysis yet",13f,white)
        metrics.setPadding(dp(14),dp(12),dp(14),dp(12)); metrics.background=style()
        root.addBlock(metrics)
        root.addBlock(label("RECOMMENDED LIGHTROOM SETTINGS",11f,muted,true),top=22,bottom=8)
        settings=label("Select a photo to calculate settings.",13f,white)
        settings.setPadding(dp(14),dp(12),dp(14),dp(12)); settings.background=style()
        root.addBlock(settings)
        root.addBlock(button("SAVE THIS PHOTO'S XMP PRESET",true) { saveSelectedPreset() },top=16)
        root.addBlock(button("EXPORT XMP PRESETS FOR ALL SELECTED PHOTOS") { chooseExportFolder() },top=9)
        root.addBlock(button("LIGHTROOM IMPORT INSTRUCTIONS") { showImportSteps() },top=9)
        root.addBlock(label("Non-destructive workflow: originals are never modified. Preset import into Lightroom is a separate user action.",12f,muted),top=16)
    }

    private fun choosePhotos() {
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type="image/*"; addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent,PICK_PHOTOS)
    }
    private fun navigate(delta: Int) {
        if(photos.isEmpty()) return
        selected=(selected+delta+photos.size)%photos.size
        showCurrent()
    }
    private fun displayName(uri: Uri): String {
        return try {
            contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use { c ->
                if(c.moveToFirst()) c.getString(0) ?: "photo" else "photo"
            } ?: "photo"
        } catch (_:Exception) { "photo" }
    }
    private fun decodeBitmap(uri: Uri): Bitmap {
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "Could not open image" }
            BitmapFactory.decodeStream(stream,null,bounds)
        }
        require(bounds.outWidth>0 && bounds.outHeight>0) { "Image format unsupported (try JPEG or HEIC)" }
        var subsample=1
        while(max(bounds.outWidth/subsample,bounds.outHeight/subsample)>1024) subsample*=2
        val opts=BitmapFactory.Options().apply { inSampleSize=subsample; inPreferredConfig=Bitmap.Config.ARGB_8888 }
        return requireNotNull(contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "Could not open image" }
            BitmapFactory.decodeStream(stream,null,opts)
        }) { "Cannot decode this image" }
    }
    private fun analyzePhoto(uri: Uri): Pair<Bitmap,Pair<PhotoStats,SuggestedEdit>> {
        val bitmap=decodeBitmap(uri)
        val cached= synchronized(cache) { cache[uri] }
        if(cached!=null) return bitmap to cached
        val pixels=IntArray(bitmap.width*bitmap.height)
        bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
        val outcome=PhotoAnalyzer.analyze(pixels,bitmap.width,bitmap.height)
        synchronized(cache) { cache[uri]=outcome }
        return bitmap to outcome
    }
    private fun showCurrent() {
        if(photos.isEmpty()) return
        val photo=photos[selected]; val sequence=++loadSequence
        filename.text=photo.title
        counter.text="${selected+1} / ${photos.size} selected"
        status.text="Analysing image offline…"
        worker.execute {
            try {
                val (bitmap,result)=analyzePhoto(photo.uri)
                runOnUiThread {
                    if(sequence!=loadSequence) { bitmap.recycle(); return@runOnUiThread }
                    hero.setImageBitmap(bitmap)
                    val (stats,edit)=result
                    val notes=stats.notes.joinToString("\n• ",prefix="• ")
                    metrics.text="Luminance  P05 %.2f  •  P50 %.2f  •  P95 %.2f\nClipped bright pixels %.1f%%  •  Dominant green %.1f%%\n\n%s".format(
                        java.util.Locale.US,stats.lumaP05,stats.lumaP50,stats.lumaP95,
                        stats.highlightClippedFraction*100,stats.greenFraction*100,notes)
                    settings.text=edit.settings.entries.joinToString("\n") { (key,value) -> "${friendlyName(key)}   $value" }
                    status.text="Ready — tailored XMP adjustment calculated"
                }
            } catch(t:Throwable) {
                runOnUiThread { if(sequence==loadSequence) {
                    status.text="Unable to analyse: ${t.message ?: "unsupported file"}"
                    metrics.text="Try a standard JPEG or HEIC photo."
                    settings.text="No settings available."
                } }
            }
        }
    }
    private fun friendlyName(key: String):String=when(key) {
        "Exposure2012"->"Exposure"; "Contrast2012"->"Contrast"; "Highlights2012"->"Highlights"
        "Shadows2012"->"Shadows"; "Whites2012"->"Whites"; "Blacks2012"->"Blacks"
        "Clarity2012"->"Clarity"; "SaturationAdjustmentGreen"->"Green saturation"
        "SaturationAdjustmentYellow"->"Yellow saturation"; "SaturationAdjustmentAqua"->"Aqua saturation"
        "HueAdjustmentGreen"->"Green hue"; "SharpenEdgeMasking"->"Sharpen masking"
        "LuminanceSmoothing"->"Luminance noise reduction"; "ColorNoiseReduction"->"Colour noise reduction"
        else->key
    }
    private fun xmpFilename(title:String,position:Int):String {
        val stem=title.substringBeforeLast('.',title).replace(Regex("[^A-Za-z0-9_-]+"),"_").take(55).ifBlank { "image" }
        return "DLB_%03d_%s.xmp".format(java.util.Locale.US,position+1,stem)
    }
    private fun saveSelectedPreset() {
        if(photos.isEmpty()) { toast("Select a photo first"); return }
        val current=photos[selected]
        val result=synchronized(cache) { cache[current.uri] }
        if(result==null) { toast("Analysis is still running"); return }
        val title="Dennis Auto • ${current.title.substringBeforeLast('.',current.title)}"
        pendingPreset=xmpFilename(current.title,selected) to XmpPreset.generate(title,result.second)
        val i=Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type="application/rdf+xml"
            putExtra(Intent.EXTRA_TITLE,pendingPreset!!.first)
        }
        startActivityForResult(i,SAVE_PRESET)
    }
    private fun chooseExportFolder() {
        if(photos.isEmpty()) { toast("Select photos first"); return }
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),EXPORT_FOLDER)
    }
    private fun showImportSteps() {
        AlertDialog.Builder(this).setTitle("Import into Lightroom Mobile")
            .setMessage("1. Save a .xmp preset using this app.\n\n2. Open Lightroom and choose the photo to edit.\n\n3. Tap Presets → Yours → ⋯ → Import Presets.\n\n4. Browse to the saved .xmp file, import and apply it.\n\n5. Review Exposure and White Balance.\n\nLightroom controls the final photo export; this app does not modify the original.")
            .setPositiveButton("DONE",null).show()
    }
    private fun toast(msg:String) { Toast.makeText(this,msg,Toast.LENGTH_LONG).show() }
    @Deprecated("Android's legacy result callback retained for dependency-free v0.1")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(resultCode!=RESULT_OK || data==null) return
        when(requestCode) {
            PICK_PHOTOS -> {
                val selectedUris=mutableListOf<Uri>()
                val clips:ClipData?=data.clipData
                if (clips != null) {
                    for (i in 0 until clips.itemCount) selectedUris += clips.getItemAt(i).uri
                } else {
                    data.data?.let { selectedUris += it }
                }
                if(selectedUris.isEmpty()) return
                val uris=selectedUris.distinct().take(100)
                photos.clear(); synchronized(cache) { cache.clear() }
                for(uri in uris) {
                    try { contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    catch (_:SecurityException) { /* current session permission still works */ }
                    photos += PickedPhoto(uri,displayName(uri))
                }
                selected=0
                showCurrent()
            }
            SAVE_PRESET -> {
                val target=data.data ?: return
                val contents=pendingPreset?.second ?: return
                try {
                    contentResolver.openOutputStream(target,"w")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(contents) }
                        ?: error("Cannot write selected file")
                    status.text="Preset saved. Import it in Lightroom Mobile."
                    toast("XMP saved successfully")
                } catch(t:Exception) { status.text="Export error: ${t.message}" }
                finally { pendingPreset=null }
            }
            EXPORT_FOLDER -> {
                val tree=data.data ?: return
                status.text="Exporting ${photos.size} presets…"
                val copy=photos.toList()
                worker.execute {
                    try {
                        val treeId=DocumentsContract.getTreeDocumentId(tree)
                        val dir=DocumentsContract.buildDocumentUriUsingTree(tree,treeId)
                        var saved=0
                        for((i,p) in copy.withIndex()) {
                            val result=analyzePhoto(p.uri).second
                            val title="Dennis Auto • ${p.title.substringBeforeLast('.',p.title)}"
                            val contents=XmpPreset.generate(title,result.second)
                            val output=DocumentsContract.createDocument(contentResolver,dir,"application/rdf+xml",xmpFilename(p.title,i))
                                ?: error("Could not create preset document")
                            contentResolver.openOutputStream(output,"w")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(contents) }
                                ?: error("Could not write preset")
                            saved++
                        }
                        runOnUiThread { status.text="Export complete: $saved XMP presets saved"; toast("Saved $saved XMP presets") }
                    } catch(t:Throwable) {
                        runOnUiThread { status.text="Export stopped: ${t.message}" }
                    }
                }
            }
        }
    }
    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }
}

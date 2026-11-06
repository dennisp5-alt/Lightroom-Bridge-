package com.dennis.lightroombridge

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.Context
import android.content.ClipboardManager
import android.content.ActivityNotFoundException
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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.dennis.lightroombridge.core.PhotoAnalyzer
import com.dennis.lightroombridge.core.PhotoStats
import com.dennis.lightroombridge.core.SuggestedEdit
import com.dennis.lightroombridge.core.XmpPreset
import com.dennis.lightroombridge.core.EditPresentation
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
    private lateinit var lookTitle: TextView
    private lateinit var lookDetail: TextView
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
        root.addBlock(label("GUIDED EDITING  •  OFFLINE ENGINE v0.1.2",11f,muted),bottom=16)
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
        root.addBlock(label("WHAT THIS EDIT IS FOR",11f,muted,true),top=17,bottom=6)
        lookTitle=label("Choose a photograph",18f,mint,true)
        root.addBlock(lookTitle)
        lookDetail=label("Your adjustment plan will be explained here.",13f,muted)
        root.addBlock(lookDetail,top=5)
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
        root.addBlock(button("COPY SETTINGS — NO PRESET CREATED",true) { copySettings() },top=16)
        root.addBlock(button("OPEN LIGHTROOM") { openLightroom() },top=9)
        root.addBlock(label("For everyday photos, copy the settings and follow the values in Lightroom. This creates no presets.",12f,muted),top=9)
        root.addBlock(button("SAVE NAMED XMP PRESET (OPTIONAL)") { saveSelectedPreset() },top=16)
        root.addBlock(button("ADVANCED: EXPORT ALL XMP PRESETS") { confirmBatchExport() },top=9)
        root.addBlock(button("LIGHTROOM INSTRUCTIONS") { showImportSteps() },top=9)
        root.addBlock(label("XMP imports create entries in your Lightroom presets library. Save them only for looks you want to reuse.",12f,muted),top=16)
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
        lookTitle.text="Analysing selected photo…"
        lookDetail.text=""
        worker.execute {
            try {
                val (bitmap,result)=analyzePhoto(photo.uri)
                runOnUiThread {
                    if(sequence!=loadSequence) { bitmap.recycle(); return@runOnUiThread }
                    hero.setImageBitmap(bitmap)
                    val (stats,edit)=result
                    val identity=EditPresentation.label(stats)
                    lookTitle.text=identity.title
                    lookDetail.text=identity.explanation
                    val notes=stats.notes.joinToString("\n• ",prefix="• ")
                    metrics.text="Luminance  P05 %.2f  •  P50 %.2f  •  P95 %.2f\nClipped bright pixels %.1f%%  •  Dominant green %.1f%%\n\n%s".format(
                        java.util.Locale.US,stats.lumaP05,stats.lumaP50,stats.lumaP95,
                        stats.highlightClippedFraction*100,stats.greenFraction*100,notes)
                    settings.text=EditPresentation.settingsText(edit)
                    status.text="Ready — guided editing recommended (no preset required)"
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
    /** Copies human-readable values, not machine-readable Lightroom settings. */
    private fun copySettings() {
        if (photos.isEmpty()) { toast("Select a photo first"); return }
        val photo=photos[selected]
        val result=synchronized(cache) { cache[photo.uri] }
        if (result==null) { toast("Photo is still being analysed"); return }
        val text=EditPresentation.checklist(result.first,result.second,photo.title)
        val clipboard=getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Dennis Lightroom Edit",text))
        status.text="Slider guide copied — no XMP preset created."
        toast("Slider guide copied. Open Lightroom to adjust the photo.")
    }

    private fun openLightroom() {
        val launch=packageManager.getLaunchIntentForPackage("com.adobe.lrmobile")
        if (launch!=null) {
            try { startActivity(launch) } catch (_: ActivityNotFoundException) {
                toast("Unable to open Lightroom automatically — open it manually.")
            }
        } else toast("Lightroom not found — please open Adobe Lightroom manually.")
    }

    private fun confirmBatchExport() {
        if (photos.isEmpty()) { toast("Select photos first"); return }
        AlertDialog.Builder(this)
            .setTitle("Export ${photos.size} separate presets?")
            .setMessage("Importing all these XMP files creates a separate preset for each photo in Lightroom. This can clutter your library. For ordinary editing, use COPY SETTINGS instead.")
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("EXPORT ANYWAY") { _,_ -> chooseExportFolder() }
            .show()
    }

    /** Some file providers append .xml, .rdf or .bin based on the MIME type.
     *  Retain a true .xmp filename for Lightroom's preset file picker. */
    private fun ensureXmpName(uri: Uri, requested: String): Pair<Uri,String> {
        val initial = displayName(uri)
        if (initial.endsWith(".xmp", ignoreCase = true)) return uri to initial
        return try {
            val renamed = DocumentsContract.renameDocument(contentResolver, uri, requested)
            if (renamed != null) renamed to displayName(renamed)
            else uri to initial
        } catch (_: Exception) {
            uri to initial
        }
    }
    private fun warnAboutXmpName(name: String): String =
        if (name.endsWith(".xmp", ignoreCase = true)) "Saved $name — ready for Lightroom import."
        else "Saved as $name. Rename the file to end in .xmp using My Files before importing."

    private fun saveSelectedPreset() {
        if(photos.isEmpty()) { toast("Select a photo first"); return }
        val current=photos[selected]
        val result=synchronized(cache) { cache[current.uri] }
        if(result==null) { toast("Analysis is still running"); return }
        val suggested=EditPresentation.presetTitle(result.first)
        val nameInput=EditText(this).apply {
            setSingleLine(true)
            setText(suggested)
            setSelection(text.length)
            setPadding(dp(12),dp(10),dp(12),dp(10))
        }
        AlertDialog.Builder(this)
            .setTitle("Name this reusable edit")
            .setMessage("Describe what the preset does. Importing it creates a preset entry in Lightroom. Use COPY SETTINGS for one-off edits instead.")
            .setView(nameInput)
            .setNegativeButton("CANCEL",null)
            .setPositiveButton("SAVE XMP") { _,_ ->
                val userName=nameInput.text.toString().trim().take(120).ifBlank { suggested }
                val filename=EditPresentation.fileName(result.first,current.title)
                pendingPreset=filename to XmpPreset.generate(userName,result.second)
                val intent=Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type="application/octet-stream"
                    putExtra(Intent.EXTRA_TITLE,filename)
                }
                startActivityForResult(intent,SAVE_PRESET)
            }.show()
    }
    private fun chooseExportFolder() {
        if(photos.isEmpty()) { toast("Select photos first"); return }
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),EXPORT_FOLDER)
    }
    private fun showImportSteps() {
        AlertDialog.Builder(this).setTitle("Import into Lightroom Mobile")
            .setMessage("GUIDED EDIT (no presets):\n1. Select a photo in Bridge and tap COPY SETTINGS.\n2. Open the matching photo in Lightroom.\n3. Use the named sliders in Light, Color, Effects and Detail.\n4. Enter target values manually. These are absolute values, not increments.\n\nOPTIONAL REUSABLE PRESET:\nSave a named XMP, then Lightroom → Presets → Yours → menu → Import Presets.\n\nTo use the same successful edit on more photos: Lightroom More → Copy Settings, then Paste Settings on other photos.")
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
                val wanted=pendingPreset?.first ?: return
                try {
                    val (safeTarget,actualName)=ensureXmpName(target,wanted)
                    contentResolver.openOutputStream(safeTarget,"w")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(contents) }
                        ?: error("Cannot write selected file")
                    status.text=warnAboutXmpName(actualName)
                    toast(if(actualName.endsWith(".xmp",true)) "XMP saved successfully" else "Check file extension: $actualName")
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
                        val badNames=mutableListOf<String>()
                        for((i,p) in copy.withIndex()) {
                            val result=analyzePhoto(p.uri).second
                            val title=EditPresentation.presetTitle(result.first)
                            val contents=XmpPreset.generate(title,result.second)
                            val wanted=EditPresentation.fileName(result.first,p.title)
                            val output=DocumentsContract.createDocument(contentResolver,dir,"application/octet-stream",wanted)
                                ?: error("Could not create preset document")
                            val (safeOutput,actualName)=ensureXmpName(output,wanted)
                            contentResolver.openOutputStream(safeOutput,"w")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(contents) }
                                ?: error("Could not write preset")
                            if (!actualName.endsWith(".xmp",true)) badNames += actualName
                            saved++
                        }
                        runOnUiThread {
                            status.text=if(badNames.isEmpty()) "Export complete: $saved XMP presets saved"
                                else "Export complete: $saved files. Some need renaming to .xmp: ${badNames.joinToString(", ")}"
                            toast("Saved $saved presets")
                        }
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

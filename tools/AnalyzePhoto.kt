import com.dennis.lightroombridge.core.PhotoAnalyzer
import com.dennis.lightroombridge.core.XmpPreset
import java.io.File
import javax.imageio.ImageIO

/** Desktop smoke-test of the same pure Kotlin analysis engine used by Android. */
fun main(args: Array<String>) {
    require(args.size >= 2) { "Usage: AnalyzePhoto <input.jpg> <output.xmp> [name]" }
    val input=File(args[0]); val output=File(args[1])
    val image=requireNotNull(ImageIO.read(input)) { "Unsupported image" }
    val pixels=IntArray(image.width * image.height)
    image.getRGB(0,0,image.width,image.height,pixels,0,image.width)
    val (stats,edit)=PhotoAnalyzer.analyze(pixels,image.width,image.height)
    output.parentFile?.mkdirs()
    output.writeText(XmpPreset.generate(args.getOrElse(2) { "Dennis Auto - ${input.nameWithoutExtension}" },edit),Charsets.UTF_8)
    println("SOURCE: ${input.name} ${image.width}x${image.height}; samples=${stats.sampledPixels}")
    edit.settings.forEach { (k,v) -> println("$k=$v") }
    println("OUTPUT: ${output.absolutePath}")
}

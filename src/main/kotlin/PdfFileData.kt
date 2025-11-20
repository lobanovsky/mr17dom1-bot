import java.io.File

data class PdfFileData(
    val fileName: String,
    val bytes: ByteArray
)

fun PdfFileData.toTempFile(): File {
    val tempDir = System.getProperty("java.io.tmpdir")
    val file = File(tempDir, fileName)
    file.writeBytes(bytes)
    return file
}
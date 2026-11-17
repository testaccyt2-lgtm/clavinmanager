package com.sirclavin.clavinmanager

import android.content.Context
import android.net.Uri
import com.github.junrar.Archive
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

/** Unpacks zip, rar, 7z, tar, gz, bz2 and xz archives into a plain folder. */
object Archives {
    val SUPPORTED = listOf(
        "zip", "mcpack", "jar", "rar", "7z", "tar", "gz", "tgz", "bz2", "tbz2", "tbz", "xz", "txz"
    )
    const val SUPPORTED_TEXT = ".zip  .rar  .7z  .tar  .gz  .bz2  .xz"

    fun isSupported(name: String): Boolean {
        val n = name.lowercase()
        return SUPPORTED.any { n.endsWith(".$it") }
    }

    /** Copies [source] to the cache and unpacks it into [out]. */
    fun extract(context: Context, source: Uri, fileName: String, out: File) {
        val tmp = File(context.cacheDir, "pack_src_${System.nanoTime()}")
        try {
            val input = context.contentResolver.openInputStream(source) ?: throw IOException("Couldn't read the file.")
            input.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
            out.mkdirs()
            when (sniff(tmp, fileName)) {
                Kind.ZIP -> unzip(tmp, out)
                Kind.RAR -> unrar(tmp, out)
                Kind.SEVENZ -> un7z(tmp, out)
                Kind.OTHER -> unstream(tmp, fileName, out)
            }
        } finally {
            tmp.delete()
        }
    }

    private enum class Kind { ZIP, RAR, SEVENZ, OTHER }

    private fun sniff(f: File, name: String): Kind {
        val head = ByteArray(8)
        val n = f.inputStream().use { it.read(head) }
        fun b(i: Int) = head[i].toInt() and 0xFF
        if (n >= 4 && b(0) == 0x50 && b(1) == 0x4B) return Kind.ZIP
        if (n >= 4 && b(0) == 0x52 && b(1) == 0x61 && b(2) == 0x72 && b(3) == 0x21) return Kind.RAR
        if (n >= 6 && b(0) == 0x37 && b(1) == 0x7A && b(2) == 0xBC && b(3) == 0xAF) return Kind.SEVENZ
        if (n >= 2 && b(0) == 0x1F && b(1) == 0x8B) return Kind.OTHER
        val lower = name.lowercase()
        return when {
            lower.endsWith(".zip") || lower.endsWith(".mcpack") || lower.endsWith(".jar") -> Kind.ZIP
            lower.endsWith(".rar") -> Kind.RAR
            lower.endsWith(".7z") -> Kind.SEVENZ
            else -> Kind.OTHER
        }
    }

    /** Returns the destination for an entry, refusing anything that would escape [root]. */
    private fun target(root: File, entryName: String): File? {
        val clean = entryName.replace('\\', '/').trimStart('/')
        if (clean.isBlank()) return null
        val f = File(root, clean)
        val rootPath = root.canonicalPath + File.separator
        return if (f.canonicalPath.startsWith(rootPath)) f else null
    }

    private fun save(from: InputStream, to: File) {
        to.parentFile?.mkdirs()
        to.outputStream().use { from.copyTo(it) }
    }

    private fun unzip(src: File, out: File) {
        ZipInputStream(BufferedInputStream(src.inputStream())).use { zin ->
            while (true) {
                val e = zin.nextEntry ?: break
                val t = target(out, e.name) ?: continue
                if (e.isDirectory || e.name.endsWith("/") || e.name.endsWith("\\")) t.mkdirs() else save(zin, t)
            }
        }
    }

    private fun unrar(src: File, out: File) {
        Archive(src).use { rar ->
            for (h in rar.fileHeaders) {
                val t = target(out, h.fileName) ?: continue
                if (h.isDirectory) {
                    t.mkdirs()
                } else {
                    t.parentFile?.mkdirs()
                    t.outputStream().use { rar.extractFile(h, it) }
                }
            }
        }
    }

    private fun un7z(src: File, out: File) {
        SevenZFile(src).use { z ->
            val buf = ByteArray(16 * 1024)
            while (true) {
                val e = z.nextEntry ?: break
                val t = target(out, e.name) ?: continue
                if (e.isDirectory) {
                    t.mkdirs()
                } else {
                    t.parentFile?.mkdirs()
                    t.outputStream().use { o ->
                        while (true) {
                            val r = z.read(buf)
                            if (r < 0) break
                            o.write(buf, 0, r)
                        }
                    }
                }
            }
        }
    }

    /** tar, and gz / bz2 / xz (with or without a tar inside). */
    private fun unstream(src: File, fileName: String, out: File) {
        var stream: InputStream = BufferedInputStream(src.inputStream())
        var inner = fileName
        try {
            val lower = fileName.lowercase()
            when {
                lower.endsWith(".gz") || lower.endsWith(".tgz") -> stream = GzipCompressorInputStream(stream)
                lower.endsWith(".bz2") || lower.endsWith(".tbz2") || lower.endsWith(".tbz") ->
                    stream = BZip2CompressorInputStream(stream)
                lower.endsWith(".xz") || lower.endsWith(".txz") -> stream = XZCompressorInputStream(stream)
            }
            inner = inner.substringBeforeLast('.', inner)
            stream = BufferedInputStream(stream)
            stream.mark(1024)
            val head = ByteArray(512)
            val n = stream.read(head)
            stream.reset()
            val isTar = n >= 262 && String(head, 257, 5, Charsets.ISO_8859_1) == "ustar"
            if (isTar) {
                TarArchiveInputStream(stream).use { tar ->
                    while (true) {
                        val e = tar.nextTarEntry ?: break
                        val t = target(out, e.name) ?: continue
                        if (e.isDirectory) t.mkdirs() else save(tar, t)
                    }
                }
            } else {
                val t = target(out, inner.ifBlank { "extracted_file" })
                    ?: throw IOException("Unsupported archive.")
                save(stream, t)
            }
        } finally {
            stream.close()
        }
    }
}

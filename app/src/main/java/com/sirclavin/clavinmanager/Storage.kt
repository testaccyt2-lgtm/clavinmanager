package com.sirclavin.clavinmanager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/** A folder we can read and write, whether it is reached by plain files or by a folder grant. */
interface Dir {
    val name: String
    fun child(name: String): Dir?
    fun subdirs(): List<Dir>
    fun makeDir(name: String): Dir?
    fun has(name: String): Boolean
    fun remove(name: String): Boolean
    fun write(name: String): OutputStream?
}

class FileDir(private val f: File) : Dir {
    override val name: String get() = f.name

    override fun child(name: String): Dir? =
        f.listFiles()?.firstOrNull { it.isDirectory && it.name.equals(name, ignoreCase = true) }?.let { FileDir(it) }

    override fun subdirs(): List<Dir> =
        f.listFiles()?.filter { it.isDirectory }?.map { FileDir(it) } ?: emptyList()

    override fun makeDir(name: String): Dir? {
        val d = File(f, name)
        return if (d.isDirectory || d.mkdirs()) FileDir(d) else null
    }

    override fun has(name: String): Boolean = File(f, name).exists()

    override fun remove(name: String): Boolean {
        val t = File(f, name)
        return !t.exists() || t.deleteRecursively()
    }

    override fun write(name: String): OutputStream? =
        try {
            FileOutputStream(File(f, name))
        } catch (e: Exception) {
            null
        }
}

class DocDir(private val ctx: Context, private val d: DocumentFile) : Dir {
    override val name: String get() = d.name ?: ""

    override fun child(name: String): Dir? {
        val hit = d.findFile(name)?.takeIf { it.isDirectory }
            ?: d.listFiles().firstOrNull { it.isDirectory && it.name.equals(name, ignoreCase = true) }
        return hit?.let { DocDir(ctx, it) }
    }

    override fun subdirs(): List<Dir> = d.listFiles().filter { it.isDirectory }.map { DocDir(ctx, it) }

    override fun makeDir(name: String): Dir? {
        val made = d.findFile(name)?.takeIf { it.isDirectory } ?: d.createDirectory(name)
        return made?.let { DocDir(ctx, it) }
    }

    override fun has(name: String): Boolean = d.findFile(name) != null

    override fun remove(name: String): Boolean = d.findFile(name)?.delete() ?: true

    override fun write(name: String): OutputStream? {
        d.findFile(name)?.delete()
        val created = d.createFile("application/octet-stream", name) ?: return null
        return ctx.contentResolver.openOutputStream(created.uri, "wt")
    }
}

class StorageProblem(message: String) : Exception(message)

/** Thrown when we have no way into the folder yet (no full access and no folder grant). */
class NeedAccess : Exception()

/** Describes a folder inside some app's Android/data/<pkg> folder. */
data class Spec(
    /** The app whose Android/data folder is opened first. */
    val pkg: String,
    /** Key under which that folder's grant is remembered. */
    val treeKey: String,
    /** Walks from Android/data/<pkg> to the folder we want. */
    val findBase: (Dir) -> Dir?,
    /** Shown when findBase finds nothing. */
    val missing: String,
    /** Folder to create inside the base when it does not exist yet (only when absent). */
    val ensure: String? = null
)

object Roots {
    const val BM_PKG = "com.sandboxol.blockymods"
    val BM_PATH = listOf("files", "Download", "SandboxOL", "BlockManv2", "map_temp")

    private fun dataDir(pkg: String) = File(Environment.getExternalStorageDirectory(), "Android/data/$pkg")

    /** Package folders visible in Android/data by plain file access (empty when the phone hides them). */
    fun listDataPackages(): List<String> = runCatching {
        File(Environment.getExternalStorageDirectory(), "Android/data").list()
            ?.filter { it.contains('.') }?.sorted() ?: emptyList()
    }.getOrDefault(emptyList())

    /** Android/data/<pkg> by plain file access when allowed, otherwise through the saved folder grant. */
    fun pkgRoot(context: Context, pkg: String, treeUri: String?): Dir? {
        val f = dataDir(pkg)
        if (runCatching { f.isDirectory && f.canWrite() }.getOrDefault(false)) return FileDir(f)
        if (treeUri == null) return null
        return runCatching {
            DocumentFile.fromTreeUri(context, Uri.parse(treeUri))?.takeIf { it.exists() }
        }.getOrNull()?.let { DocDir(context, it) }
    }

    fun canOpen(context: Context, spec: Spec, treeUri: String?): Boolean =
        pkgRoot(context, spec.pkg, treeUri) != null

    fun open(context: Context, spec: Spec, treeUri: String?): Dir {
        val root = pkgRoot(context, spec.pkg, treeUri) ?: throw NeedAccess()
        val base = spec.findBase(root) ?: throw StorageProblem(spec.missing)
        val ensure = spec.ensure ?: return base
        return base.child(ensure) ?: base.makeDir(ensure) ?: throw StorageProblem("Couldn't create the $ensure folder.")
    }

    fun descend(root: Dir, path: List<String>): Dir? {
        var cur = root
        for (seg in path) cur = cur.child(seg) ?: return null
        return cur
    }

    /** Searches for emulated/0 below [root] (breadth first, bounded). */
    fun findEmulatedZero(root: Dir): Dir? {
        val queue = ArrayDeque<Pair<Dir, Int>>()
        queue.add(root to 0)
        var visited = 0
        while (queue.isNotEmpty() && visited < 600) {
            val (dir, depth) = queue.removeFirst()
            visited++
            if (dir.name.equals("emulated", ignoreCase = true)) dir.child("0")?.let { return it }
            if (depth >= 8) continue
            for (c in dir.subdirs()) queue.add(c to depth + 1)
        }
        return null
    }
}

/** "All files access" – the fallback for phones whose folder picker refuses Android/data. */
object Access {
    fun granted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 30) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }

    /** True on phones where the system folder picker almost always refuses Android/data. */
    val pickerLikelyBlocked: Boolean get() = Build.VERSION.SDK_INT >= 33
}

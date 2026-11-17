package com.sirclavin.clavinmanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ImportResult {
    data class Success(
        val name: String,
        val where: String,
        val extracted: List<String> = emptyList()
    ) : ImportResult

    data class Failure(val message: String) : ImportResult

    /** No way into the folder yet: ask for a folder grant or All files access. */
    data object NeedAccess : ImportResult
}

private fun cleanName(name: String) = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")

private suspend fun guarded(block: () -> ImportResult): ImportResult = withContext(Dispatchers.IO) {
    try {
        block()
    } catch (e: NeedAccess) {
        ImportResult.NeedAccess
    } catch (e: SecurityException) {
        ImportResult.Failure("Access to the folder was lost. Please allow access again.")
    } catch (e: StorageProblem) {
        ImportResult.Failure(e.message ?: "Something went wrong.")
    } catch (e: Exception) {
        ImportResult.Failure("Something went wrong. (${e.javaClass.simpleName})")
    }
}

/** Shared helpers for the system folder / file pickers. */
object LuaImporter {
    const val TOOL_DIR = "ClavinTool"
    private const val AUTHORITY = "com.android.externalstorage.documents"
    private const val DATA_MARKER = "Android/data/"

    // ---------- picker helpers ----------

    fun initialPickerUri(app: CloneApp): Uri = initialPickerUri(app.pkg)

    fun initialPickerUri(pkg: String?): Uri {
        val id = if (pkg != null) "primary:Android/data/$pkg" else "primary:Android/data"
        return DocumentsContract.buildDocumentUri(AUTHORITY, id)
    }

    /** "com.some.app" from a tree URI that points into Android/data/com.some.app[/...] */
    fun packageFromTree(uri: Uri): String? = try {
        val id = DocumentsContract.getTreeDocumentId(uri)
        val idx = id.indexOf(DATA_MARKER)
        if (idx < 0) null else id.substring(idx + DATA_MARKER.length).substringBefore('/').ifBlank { null }
    } catch (e: Exception) {
        null
    }

    /** Returns an error message, or null when the picked folder is fine. */
    fun validateTree(app: CloneApp, uri: Uri): String? = validatePkg(app.pkg, uri)

    fun validatePkg(expected: String?, uri: Uri): String? {
        val pkg = packageFromTree(uri)
            ?: return "Please pick an app's folder inside Android/data (the one named like com.app.name)."
        if (expected != null) {
            val exact = try {
                DocumentsContract.getTreeDocumentId(uri).substringAfter(DATA_MARKER) == expected
            } catch (e: Exception) {
                false
            }
            if (pkg != expected || !exact) return "Please pick the folder named $expected."
        }
        return null
    }

    fun persist(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Not fatal: the grant still works for this session.
        }
    }

    fun displayName(context: Context, uri: Uri): String? {
        val fromProvider = try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
        return fromProvider ?: uri.lastPathSegment?.substringAfterLast('/')
    }

    // ---------- Lua scripts ----------

    /** emulated/0 inside the clone app, then ClavinTool (created only if missing). */
    fun spec(app: CloneApp, pkg: String): Spec = Spec(
        pkg = pkg,
        treeKey = app.key,
        findBase = { root ->
            if (app == CloneApp.OTHER) Roots.findEmulatedZero(root) else Roots.descend(root, app.inner)
        },
        missing = "Couldn't find the storage folder of ${app.label}. Open the app once and try again.",
        ensure = TOOL_DIR
    )

    suspend fun import(context: Context, spec: Spec, treeUri: String?, source: Uri, fileName: String): ImportResult =
        guarded {
            val dir = Roots.open(context, spec, treeUri)
            val input = context.contentResolver.openInputStream(source)
                ?: throw StorageProblem("Couldn't read the file you picked.")
            val out = dir.write(cleanName(fileName))
                ?: throw StorageProblem("Couldn't create the file inside ClavinTool.")
            input.use { i -> out.use { o -> i.copyTo(o) } }
            ImportResult.Success(fileName, "ClavinTool folder")
        }

    suspend fun delete(context: Context, spec: Spec, treeUri: String?, fileName: String): ImportResult =
        guarded {
            val base = Roots.open(context, spec.copy(ensure = null), treeUri)
            val tool = base.child(TOOL_DIR)
            if (tool != null && !tool.remove(cleanName(fileName))) {
                throw StorageProblem("Couldn't delete $fileName.")
            }
            ImportResult.Success(fileName, "ClavinTool folder")
        }
}

/** Resource packs: unpacked into Blockmango's map_temp folder, replacing what is there. */
object PackImporter {
    fun realSpec(): Spec = Spec(
        pkg = Roots.BM_PKG,
        treeKey = "realbm",
        findBase = { root -> Roots.descend(root, Roots.BM_PATH) },
        missing = "Couldn't find the map_temp folder. Load Sky Pixel in Blockmango first, then try again."
    )

    fun cloneSpec(app: CloneApp, pkg: String): Spec = Spec(
        pkg = pkg,
        treeKey = app.key,
        findBase = { root ->
            val em0 = if (app == CloneApp.OTHER) Roots.findEmulatedZero(root) else Roots.descend(root, app.inner)
            em0?.let { Roots.descend(it, listOf("Android", "data", Roots.BM_PKG) + Roots.BM_PATH) }
        },
        missing = "Couldn't find the map_temp folder inside ${app.label}. Load Sky Pixel in Blockmango there first, then try again."
    )

    suspend fun import(context: Context, spec: Spec, treeUri: String?, source: Uri, fileName: String): ImportResult {
        val staging = File(context.cacheDir, "pack_stage_${System.nanoTime()}")
        return try {
            guarded {
                // Check we can reach map_temp before doing any heavy work.
                val dest = Roots.open(context, spec, treeUri)
                try {
                    Archives.extract(context, source, fileName, staging)
                } catch (e: Exception) {
                    throw StorageProblem("Couldn't unpack that file. It may be damaged, password protected, or an unsupported format.")
                }
                val tops = staging.listFiles()?.toList().orEmpty()
                if (tops.isEmpty()) throw StorageProblem("That archive is empty.")
                tops.forEach { copyInto(it, dest) }
                ImportResult.Success(fileName, "map_temp folder", tops.map { it.name })
            }
        } finally {
            staging.deleteRecursively()
        }
    }

    /** Replaces any existing entry of the same name, then copies the new one in. */
    private fun copyInto(src: File, dir: Dir) {
        dir.remove(src.name)
        if (src.isDirectory) {
            val sub = dir.makeDir(src.name) ?: throw StorageProblem("Couldn't create the folder ${src.name}.")
            src.listFiles()?.forEach { copyInto(it, sub) }
        } else {
            val out = dir.write(src.name) ?: throw StorageProblem("Couldn't write ${src.name}.")
            src.inputStream().use { i -> out.use { o -> i.copyTo(o) } }
        }
    }
}

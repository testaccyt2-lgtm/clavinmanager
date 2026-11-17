package com.sirclavin.clavinmanager

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The clone apps ClavinTool can live in. */
enum class CloneApp(
    val key: String,
    val label: String,
    val pkg: String?,
    /** Folders to walk inside the app's Android/data/<pkg> folder to reach emulated/0. */
    val inner: List<String>
) {
    CLAVINBOX(
        "clavinbox", "Clavinbox", "com.sirclavin.clavinbox",
        listOf("files", "blackbox", "storage", "emulated", "0")
    ),
    MULTI_APP(
        "multiapp", "Multi App Ultra", "com.waxmoon.ma.gp",
        listOf("rootfs", "storage", "emulated", "0")
    ),
    OTHER("other", "Other", null, emptyList());

    companion object {
        fun fromKey(key: String?): CloneApp? = values().firstOrNull { it.key == key }
    }
}

/** One imported script ("lua") or resource pack ("pack"). */
data class HistoryItem(
    val name: String,
    val time: Long,
    val target: String,
    val kind: String = "lua",
    /** Clone app key the script went to (needed to delete it later). */
    val appKey: String = "",
    /** Resource packs: what the archive unpacked to. */
    val extra: String = ""
) {
    val isLua: Boolean get() = kind == "lua"
}

enum class BmTarget(val key: String, val label: String) {
    REAL("real", "Real Blockmango"),
    CLONE("clone", "Cloned Blockmango");

    companion object {
        fun fromKey(key: String?): BmTarget? = values().firstOrNull { it.key == key }
    }
}

data class PickedFile(val uri: Uri, val name: String)

sealed interface Detection {
    data class Switched(val to: CloneApp) : Detection
    data class Suggest(val app: CloneApp) : Detection
}

private fun SharedPreferences.optBool(key: String): Boolean? =
    if (contains(key)) getBoolean(key, false) else null

private fun clean(s: String) = s.replace(Regex("[\t\n\r]"), " ")

/** Everything the app remembers. Backed by SharedPreferences, observable by Compose. */
class AppState(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("clavin_prefs", Context.MODE_PRIVATE)

    var installed by mutableStateOf(sp.optBool("installed"))
        private set
    var cloned by mutableStateOf(sp.optBool("cloned"))
        private set
    var cloneApp by mutableStateOf(CloneApp.fromKey(sp.getString("clone_app", null)))
        private set
    var trees by mutableStateOf(loadTrees())
        private set
    var hintShown by mutableStateOf(sp.getBoolean("hint_shown", false))
        private set
    var autoDetect by mutableStateOf(sp.getBoolean("auto_detect", true))
        private set
    var knownInstalled by mutableStateOf(sp.getStringSet("known_installed", emptySet()) ?: emptySet())
        private set
    var bmTarget by mutableStateOf(BmTarget.fromKey(sp.getString("bm_target", null)))
        private set
    var skypixel by mutableStateOf(sp.optBool("skypixel"))
        private set
    var otherPkg by mutableStateOf(sp.getString("other_pkg", null))
        private set
    var history by mutableStateOf(loadHistory())
        private set
    var totalImports by mutableStateOf(sp.getInt("total_imports", 0))
        private set

    val isSetupComplete: Boolean
        get() = installed == true && cloned == true && cloneApp != null &&
            (cloneApp != CloneApp.OTHER || pkgOf(CloneApp.OTHER) != null)

    /** The clone app is known well enough to open its folder. */
    val cloneKnown: Boolean get() = cloneApp?.let { pkgOf(it) != null } == true

    val packReady: Boolean
        get() = bmTarget != null && skypixel == true &&
            (bmTarget == BmTarget.REAL || cloneKnown)

    private fun loadTrees(): Map<String, String> =
        CloneApp.values().mapNotNull { app ->
            sp.getString("tree_${app.key}", null)?.let { app.key to it }
        }.toMap()

    private fun loadHistory(): List<HistoryItem> =
        (sp.getString("history", "") ?: "").split("\n").mapNotNull { line ->
            val p = line.split("\t")
            if (p.size >= 3) {
                HistoryItem(
                    name = p[0], time = p[1].toLongOrNull() ?: 0L, target = p[2],
                    kind = p.getOrNull(3)?.ifBlank { null } ?: "lua",
                    appKey = p.getOrNull(4) ?: "",
                    extra = p.getOrNull(5) ?: ""
                )
            } else null
        }

    fun treeFor(app: CloneApp?): String? = app?.let { trees[it.key] }

    fun treeByKey(key: String): String? = trees[key]

    fun setTreeByKey(key: String, uri: String?) {
        val m = trees.toMutableMap()
        if (uri == null) m.remove(key) else m[key] = uri
        trees = m
        sp.edit().apply { if (uri == null) remove("tree_$key") else putString("tree_$key", uri) }.apply()
    }

    /** Package folder name of a clone app ("Other" comes from what the user picked or typed). */
    fun pkgOf(app: CloneApp): String? =
        app.pkg ?: otherPkg ?: treeFor(app)?.let { LuaImporter.packageFromTree(Uri.parse(it)) }

    fun setOtherPkg(pkg: String?) {
        otherPkg = pkg
        sp.edit().apply { if (pkg == null) remove("other_pkg") else putString("other_pkg", pkg) }.apply()
    }

    fun luaSpec(app: CloneApp): Spec? = pkgOf(app)?.let { LuaImporter.spec(app, it) }

    /** Where resource packs go, or null when the clone app is not known yet. */
    fun packSpec(target: BmTarget, app: CloneApp?): Spec? = when (target) {
        BmTarget.REAL -> PackImporter.realSpec()
        BmTarget.CLONE -> app?.let { a -> pkgOf(a)?.let { PackImporter.cloneSpec(a, it) } }
    }

    fun packTree(target: BmTarget, app: CloneApp?): String? =
        if (target == BmTarget.REAL) treeByKey("realbm") else treeFor(app)

    fun answerBmTarget(t: BmTarget?) {
        bmTarget = t
        sp.edit().apply { if (t == null) remove("bm_target") else putString("bm_target", t.key) }.apply()
    }

    fun answerSkypixel(v: Boolean?) {
        skypixel = v
        sp.edit().apply { if (v == null) remove("skypixel") else putBoolean("skypixel", v) }.apply()
    }

    /** "Clavinbox", "Multi App Ultra" or "Other · com.some.app". */
    fun targetLabel(app: CloneApp): String {
        if (app != CloneApp.OTHER) return app.label
        val pkg = pkgOf(app)
        return if (pkg != null) "Other · $pkg" else "Other"
    }

    fun targetLabelFor(app: CloneApp): String {
        if (app != CloneApp.OTHER) return app.label
        val pkg = pkgOf(app)
        return if (pkg != null) "Other · $pkg" else "Other"
    }

    fun answerInstalled(v: Boolean?) {
        installed = v
        sp.edit().apply { if (v == null) remove("installed") else putBoolean("installed", v) }.apply()
    }

    fun answerCloned(v: Boolean?) {
        cloned = v
        sp.edit().apply { if (v == null) remove("cloned") else putBoolean("cloned", v) }.apply()
    }

    fun chooseCloneApp(app: CloneApp?) {
        cloneApp = app
        sp.edit().apply { if (app == null) remove("clone_app") else putString("clone_app", app.key) }.apply()
    }

    fun setTree(app: CloneApp, uri: String?) {
        val m = trees.toMutableMap()
        if (uri == null) m.remove(app.key) else m[app.key] = uri
        trees = m
        sp.edit().apply { if (uri == null) remove("tree_${app.key}") else putString("tree_${app.key}", uri) }.apply()
    }

    fun markHintShown() {
        hintShown = true
        sp.edit().putBoolean("hint_shown", true).apply()
    }

    fun updateAutoDetect(v: Boolean) {
        autoDetect = v
        sp.edit().putBoolean("auto_detect", v).apply()
    }

    fun updateKnown(keys: Set<String>) {
        knownInstalled = keys
        sp.edit().putStringSet("known_installed", keys).apply()
    }

    fun addHistory(item: HistoryItem) {
        history = (listOf(item) + history).take(50)
        totalImports += 1
        sp.edit()
            .putString("history", serialize(history))
            .putInt("total_imports", totalImports)
            .apply()
    }

    fun clearHistory() {
        history = emptyList()
        sp.edit().putString("history", "").apply()
    }

    fun removeHistory(item: HistoryItem) {
        history = history.filterNot { it == item }
        sp.edit().putString("history", serialize(history)).apply()
    }

    fun resetAll() {
        sp.edit().clear().apply()
        installed = null
        cloned = null
        cloneApp = null
        trees = emptyMap()
        hintShown = false
        autoDetect = true
        knownInstalled = emptySet()
        history = emptyList()
        totalImports = 0
        bmTarget = null
        skypixel = null
        otherPkg = null
    }

    private fun serialize(items: List<HistoryItem>) =
        items.joinToString("\n") {
            listOf(clean(it.name), it.time.toString(), clean(it.target), it.kind, it.appKey, clean(it.extra))
                .joinToString("\t")
        }
}

/** Notices when the user moves from one clone app to another. */
object Detector {
    private fun isInstalled(pm: PackageManager, pkg: String): Boolean = try {
        pm.getPackageInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun installedNow(context: Context): Set<CloneApp> {
        val pm = context.packageManager
        return listOf(CloneApp.CLAVINBOX, CloneApp.MULTI_APP)
            .filter { isInstalled(pm, it.pkg!!) }
            .toSet()
    }

    fun installedKeys(context: Context): Set<String> = installedNow(context).map { it.key }.toSet()

    fun check(context: Context, state: AppState): Detection? {
        if (!state.autoDetect) return null
        val now = installedNow(context)
        val known = state.knownInstalled
        state.updateKnown(now.map { it.key }.toSet())

        val selected = state.cloneApp ?: return null

        // The app they picked is gone and exactly one other clone app is installed -> switch.
        if (selected != CloneApp.OTHER && selected !in now) {
            val others = now - selected
            if (others.size == 1) {
                val to = others.first()
                state.chooseCloneApp(to)
                return Detection.Switched(to)
            }
            return null
        }

        // A new clone app showed up since last time -> ask.
        val fresh = now.filter { it.key !in known && it != selected }
        if (fresh.size == 1) return Detection.Suggest(fresh.first())
        return null
    }
}

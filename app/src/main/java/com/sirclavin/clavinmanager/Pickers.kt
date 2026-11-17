package com.sirclavin.clavinmanager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** File picker that opens on the Downloads folder. */
class LuaOpenDocument : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent {
        return super.createIntent(context, input).apply {
            putExtra(
                DocumentsContract.EXTRA_INITIAL_URI,
                DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Download")
            )
        }
    }
}

/** Small wrapper so any screen can open the system pickers and the storage-access screen. */
class Pickers(
    val pickLua: ((Uri?) -> Unit) -> Unit,
    val pickTree: (Uri?, (Uri?) -> Unit) -> Unit,
    /** Opens the "All files access" screen (or the storage permission prompt on old phones). */
    val askAllFiles: ((Boolean) -> Unit) -> Unit
)

@Composable
fun rememberPickers(): Pickers {
    val context = LocalContext.current
    var luaCb by remember { mutableStateOf<((Uri?) -> Unit)?>(null) }
    var treeCb by remember { mutableStateOf<((Uri?) -> Unit)?>(null) }
    var accessCb by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val luaLauncher = rememberLauncherForActivityResult(LuaOpenDocument()) { uri ->
        val cb = luaCb
        luaCb = null
        cb?.invoke(uri)
    }
    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val cb = treeCb
        treeCb = null
        cb?.invoke(uri)
    }
    // Android 11+: the user flips a switch on a settings screen and comes back.
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val cb = accessCb
        accessCb = null
        cb?.invoke(Access.granted(context))
    }
    // Android 10 and older: a normal permission prompt.
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        val cb = accessCb
        accessCb = null
        cb?.invoke(ok)
    }

    return remember {
        Pickers(
            pickLua = { cb ->
                luaCb = cb
                luaLauncher.launch(arrayOf("*/*"))
            },
            pickTree = { initial, cb ->
                treeCb = cb
                try {
                    treeLauncher.launch(initial)
                } catch (e: Exception) {
                    treeCb = null
                    cb(null)
                }
            },
            askAllFiles = { cb ->
                accessCb = cb
                if (Build.VERSION.SDK_INT >= 30) {
                    val pkg = Uri.parse("package:${context.packageName}")
                    try {
                        settingsLauncher.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, pkg))
                    } catch (e: Exception) {
                        try {
                            settingsLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                        } catch (e2: Exception) {
                            accessCb = null
                            cb(false)
                        }
                    }
                } else {
                    permLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        )
    }
}

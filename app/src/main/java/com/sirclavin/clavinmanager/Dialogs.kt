package com.sirclavin.clavinmanager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Asks for a way into an app's Android/data folder: the folder picker, or "All files access"
 * on phones where the picker refuses ("can't allow access for privacy reasons").
 */
@Composable
fun AccessDialog(
    spec: Spec,
    state: AppState,
    pickers: Pickers,
    onGained: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val c = Clavin.colors
    var note by remember { mutableStateOf<String?>(null) }
    val fullAccess = Access.granted(context)
    val pickerFirst = !Access.pickerLikelyBlocked

    fun chooseFolder() {
        note = null
        pickers.pickTree(LuaImporter.initialPickerUri(spec.pkg)) { uri ->
            if (uri == null) {
                note = "Your phone closed or blocked the folder picker. Try “All files access” instead."
            } else {
                val problem = LuaImporter.validatePkg(spec.pkg, uri)
                if (problem != null) {
                    note = problem
                } else {
                    LuaImporter.persist(context, uri)
                    state.setTreeByKey(spec.treeKey, uri.toString())
                    onGained()
                }
            }
        }
    }

    fun allFiles() {
        note = null
        pickers.askAllFiles { ok ->
            if (ok) onGained() else note = "All files access wasn't turned on."
        }
    }

    PopDialog(onDismiss = onCancel) {
        IconBubble(Icons.Rounded.Lock)
        Spacer(Modifier.height(16.dp))
        DialogTitle("One-time access")
        Spacer(Modifier.height(10.dp))
        DialogBody(
            when {
                fullAccess ->
                    "ClavinManager has full file access, but your phone still hides ${spec.pkg}. " +
                        "Try choosing the folder yourself."
                pickerFirst ->
                    "Android needs your OK before ClavinManager can save files into ${spec.pkg}. " +
                        "Tap “Use this folder” on the next screen. You'll only do this once."
                else ->
                    "Your phone may refuse the folder picker (“can't allow access for privacy reasons”). " +
                        "If so, allow All files access instead. You'll only do this once."
            }
        )
        val n = note
        if (n != null) {
            Spacer(Modifier.height(10.dp))
            Text(n, color = c.blueText, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(22.dp))
        if (!fullAccess && !pickerFirst) {
            PrimaryButton("Allow all files access") { allFiles() }
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Choose folder instead") { chooseFolder() }
        } else {
            PrimaryButton("Choose folder") { chooseFolder() }
            if (!fullAccess) {
                Spacer(Modifier.height(10.dp))
                SecondaryButton("Use all files access") { allFiles() }
            }
        }
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Not now", onClick = onCancel)
    }
}

/** The three "what cloning app are you using?" choices, with the whole "Other" flow built in. */
@Composable
fun CloneAppOptions(state: AppState, pickers: Pickers, onChosen: (CloneApp) -> Unit) {
    val context = LocalContext.current
    val c = Clavin.colors
    var error by remember { mutableStateOf<String?>(null) }
    var showPackages by remember { mutableStateOf(false) }
    var blocked by remember { mutableStateOf(false) }

    fun choose(app: CloneApp) {
        state.chooseCloneApp(app)
        state.updateKnown(Detector.installedKeys(context))
        onChosen(app)
    }

    OptionRow(Icons.Rounded.Apps, "Clavinbox") { choose(CloneApp.CLAVINBOX) }
    Spacer(Modifier.height(10.dp))
    OptionRow(Icons.Rounded.Extension, "Multi App Ultra") { choose(CloneApp.MULTI_APP) }
    Spacer(Modifier.height(10.dp))
    OptionRow(Icons.Rounded.Folder, "Other", "Pick the app's folder in Android/data") {
        error = null
        if (Access.granted(context) && Roots.listDataPackages().isNotEmpty()) {
            showPackages = true
        } else {
            pickers.pickTree(LuaImporter.initialPickerUri(null as String?)) { uri ->
                if (uri == null) {
                    blocked = true
                } else {
                    val problem = LuaImporter.validatePkg(null, uri)
                    if (problem != null) {
                        error = problem
                    } else {
                        LuaImporter.persist(context, uri)
                        state.setTree(CloneApp.OTHER, uri.toString())
                        state.setOtherPkg(LuaImporter.packageFromTree(uri))
                        choose(CloneApp.OTHER)
                    }
                }
            }
        }
    }
    val e = error
    if (e != null) {
        Spacer(Modifier.height(12.dp))
        Text(e, color = c.blueText, fontSize = 13.sp, textAlign = TextAlign.Center)
    }

    if (blocked) {
        PopDialog(onDismiss = { blocked = false }) {
            IconBubble(Icons.Rounded.Lock)
            Spacer(Modifier.height(16.dp))
            DialogTitle("Folder picker blocked")
            Spacer(Modifier.height(10.dp))
            DialogBody(
                "No problem. Allow All files access and choose your app from a list, " +
                    "or type its package name."
            )
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Allow all files access") {
                pickers.askAllFiles {
                    blocked = false
                    showPackages = true
                }
            }
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Type the package name") {
                blocked = false
                showPackages = true
            }
        }
    }

    if (showPackages) {
        PackagePickerDialog(
            onPick = { pkg ->
                showPackages = false
                state.setOtherPkg(pkg)
                choose(CloneApp.OTHER)
            },
            onDismiss = { showPackages = false }
        )
    }
}

@Composable
fun PackagePickerDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val c = Clavin.colors
    val packages = remember { Roots.listDataPackages() }
    var text by remember { mutableStateOf("") }
    val valid = text.contains('.') && text.none { it.isWhitespace() || it == '/' }

    PopDialog(onDismiss = onDismiss) {
        IconBubble(Icons.Rounded.Folder)
        Spacer(Modifier.height(16.dp))
        DialogTitle("Which app is it?")
        Spacer(Modifier.height(10.dp))
        DialogBody("Choose the folder named like com.whateverapp.name")
        Spacer(Modifier.height(14.dp))
        if (packages.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 190.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .verticalScroll(rememberScrollState())
            ) {
                packages.forEach { p ->
                    Text(
                        p,
                        color = if (p == text) c.blueText else c.ink,
                        fontWeight = if (p == text) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { text = p }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.trim() },
            singleLine = true,
            placeholder = { Text("com.whateverapp.name") },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Brand.Blue,
                unfocusedBorderColor = c.border,
                focusedTextColor = c.ink,
                unfocusedTextColor = c.ink,
                cursorColor = Brand.Blue
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))
        if (valid) PrimaryButton("Use this app") { onPick(text) }
        else SecondaryButton("Cancel", onClick = onDismiss)
    }
}

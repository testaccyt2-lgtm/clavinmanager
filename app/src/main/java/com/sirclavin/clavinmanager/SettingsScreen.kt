package com.sirclavin.clavinmanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class SettingsDialog { Installed, Cloned, App, Target, Skypixel, Reset, FolderAccess, RealAccess }

@Composable
fun SettingsScreen(state: AppState, pickers: Pickers, modifier: Modifier) {
    val context = LocalContext.current
    val c = Clavin.colors
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var message by remember { mutableStateOf<Pair<String, String>?>(null) }

    fun yesNo(v: Boolean?) = when (v) {
        true -> "Yes"
        false -> "No"
        null -> "Not set"
    }

    // Re-reads "All files access" after coming back from the system screen.
    var accessTick by remember { mutableIntStateOf(0) }
    val fullAccess = remember(accessTick) { Access.granted(context) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Appear(0) { Text("Settings", color = c.ink, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(6.dp))
        Appear(0) { Text("Change your answers any time.", color = c.muted, fontSize = 14.sp) }

        Spacer(Modifier.height(20.dp))
        Appear(1) { SectionTitle("Your setup") }
        Spacer(Modifier.height(10.dp))
        Appear(1) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingRow(Icons.Rounded.Extension, "ClavinTool installed", yesNo(state.installed)) { dialog = SettingsDialog.Installed }
                SettingRow(Icons.Rounded.Folder, "ClavinTool cloned", yesNo(state.cloned)) { dialog = SettingsDialog.Cloned }
                SettingRow(
                    Icons.Rounded.Apps, "Clone app",
                    state.cloneApp?.let { state.targetLabel(it) } ?: "Not set"
                ) { dialog = SettingsDialog.App }
                SettingRow(
                    Icons.Rounded.Palette, "Resource packs go to",
                    state.bmTarget?.label ?: "Not set"
                ) { dialog = SettingsDialog.Target }
                SettingRow(Icons.Rounded.Palette, "Sky Pixel loaded", yesNo(state.skypixel)) { dialog = SettingsDialog.Skypixel }
            }
        }

        Spacer(Modifier.height(22.dp))
        Appear(2) { SectionTitle("Folder access") }
        Spacer(Modifier.height(10.dp))
        Appear(2) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val app = state.cloneApp
                SettingRow(
                    Icons.Rounded.Lock,
                    "Clone app folder",
                    when {
                        app == null -> "Choose a clone app first"
                        Roots.canOpen(context, state.luaSpec(app) ?: Spec("", "", { null }, ""), state.treeFor(app)) &&
                            state.luaSpec(app) != null -> "Allowed for ${state.targetLabel(app)}"
                        else -> "Tap to allow ${state.targetLabel(app)}"
                    }
                ) {
                    if (app != null && state.luaSpec(app) != null) dialog = SettingsDialog.FolderAccess
                }
                SettingRow(
                    Icons.Rounded.Lock,
                    "Real Blockmango folder",
                    if (Roots.canOpen(context, PackImporter.realSpec(), state.treeByKey("realbm"))) "Allowed" else "Tap to allow"
                ) { dialog = SettingsDialog.RealAccess }
                SettingRow(
                    Icons.Rounded.Lock,
                    "All files access",
                    if (fullAccess) "On. Used when the folder picker is blocked" else "Off. Turn on if your phone blocks the folder picker"
                ) {
                    pickers.askAllFiles {
                        accessTick++
                        message = if (it) "All files access is on" to "ClavinManager will use it automatically when it needs to."
                        else "Still off" to "All files access wasn't turned on."
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Appear(3) { SectionTitle("Smart features") }
        Spacer(Modifier.height(10.dp))
        Appear(3) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .softCard(20.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBubble(Icons.Rounded.Settings, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Auto-detect clone app", color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Notices when you move to a different app", color = c.muted, fontSize = 12.sp, lineHeight = 16.sp)
                }
                Switch(
                    checked = state.autoDetect,
                    onCheckedChange = { state.updateAutoDetect(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Brand.White,
                        checkedTrackColor = Brand.Blue,
                        uncheckedThumbColor = Brand.White,
                        uncheckedTrackColor = c.muted.copy(alpha = 0.35f),
                        uncheckedBorderColor = c.muted.copy(alpha = 0.0f)
                    )
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        Appear(4) { SectionTitle("Start over") }
        Spacer(Modifier.height(10.dp))
        Appear(4) {
            SettingRow(Icons.Rounded.Delete, "Reset everything", "Forget all answers and history") {
                dialog = SettingsDialog.Reset
            }
        }

        Spacer(Modifier.height(26.dp))
        Appear(5) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                ClavinLogo(40.dp)
                Spacer(Modifier.height(8.dp))
                Text("ClavinManager 1.0.0", color = c.muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(28.dp))
    }

    // ---------------- dialogs ----------------

    when (dialog) {
        SettingsDialog.Installed -> PopDialog(onDismiss = { dialog = null }) {
            IconBubble(Icons.Rounded.Extension)
            Spacer(Modifier.height(16.dp))
            DialogTitle("Do you have ClavinTool installed?")
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Yes") { state.answerInstalled(true); dialog = null }
            Spacer(Modifier.height(10.dp))
            SecondaryButton("No") { state.answerInstalled(false); dialog = null }
        }

        SettingsDialog.Cloned -> PopDialog(onDismiss = { dialog = null }) {
            IconBubble(Icons.Rounded.Folder)
            Spacer(Modifier.height(16.dp))
            DialogTitle("Have you cloned your ClavinTool on the clone app of your choice?")
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Yes") { state.answerCloned(true); dialog = null }
            Spacer(Modifier.height(10.dp))
            SecondaryButton("No") { state.answerCloned(false); dialog = null }
        }

        SettingsDialog.App -> PopDialog(onDismiss = { dialog = null }) {
            DialogTitle("What cloning app are you using?")
            Spacer(Modifier.height(20.dp))
            CloneAppOptions(state, pickers) { dialog = null }
        }

        SettingsDialog.Target -> PopDialog(onDismiss = { dialog = null }) {
            IconBubble(Icons.Rounded.Palette)
            Spacer(Modifier.height(16.dp))
            DialogTitle("Are you looking to import into?")
            Spacer(Modifier.height(20.dp))
            OptionRow(Icons.Rounded.Apps, "Real Blockmango") {
                state.answerBmTarget(BmTarget.REAL)
                dialog = null
            }
            Spacer(Modifier.height(10.dp))
            OptionRow(Icons.Rounded.Extension, "Cloned Blockmango") {
                state.answerBmTarget(BmTarget.CLONE)
                dialog = if (state.cloneKnown) null else SettingsDialog.App
            }
        }

        SettingsDialog.Skypixel -> PopDialog(onDismiss = { dialog = null }) {
            IconBubble(Icons.Rounded.Palette)
            Spacer(Modifier.height(16.dp))
            DialogTitle("Have you loaded Sky Pixel in Blockmango?")
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Yes") { state.answerSkypixel(true); dialog = null }
            Spacer(Modifier.height(10.dp))
            SecondaryButton("No") { state.answerSkypixel(false); dialog = null }
        }

        SettingsDialog.FolderAccess -> {
            val app = state.cloneApp
            val spec = app?.let { state.luaSpec(it) }
            if (spec != null) {
                AccessDialog(spec, state, pickers,
                    onGained = {
                        dialog = null
                        accessTick++
                        message = "Access saved" to "ClavinManager can now reach ${spec.pkg}."
                    },
                    onCancel = { dialog = null })
            } else dialog = null
        }

        SettingsDialog.RealAccess -> AccessDialog(PackImporter.realSpec(), state, pickers,
            onGained = {
                dialog = null
                accessTick++
                message = "Access saved" to "ClavinManager can now reach Real Blockmango."
            },
            onCancel = { dialog = null })

        SettingsDialog.Reset -> PopDialog(onDismiss = { dialog = null }) {
            IconBubble(Icons.Rounded.Delete)
            Spacer(Modifier.height(16.dp))
            DialogTitle("Reset everything?")
            Spacer(Modifier.height(10.dp))
            DialogBody("ClavinManager will forget your answers and import history. Your scripts stay where they are.")
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Yes, reset") { state.resetAll(); dialog = null }
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Cancel") { dialog = null }
        }

        null -> Unit
    }

    val m = message
    if (m != null) {
        PopDialog(onDismiss = { message = null }) {
            IconBubble(Icons.Rounded.Lock)
            Spacer(Modifier.height(16.dp))
            DialogTitle(m.first)
            Spacer(Modifier.height(10.dp))
            DialogBody(m.second)
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Okay") { message = null }
        }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    val c = Clavin.colors
    Row(
        Modifier
            .fillMaxWidth()
            .bouncyClick(onClick)
            .softCard(20.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(icon, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(value, color = c.muted, fontSize = 13.sp)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.muted)
    }
}

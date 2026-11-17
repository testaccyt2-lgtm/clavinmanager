package com.sirclavin.clavinmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryRow(item: HistoryItem, onDelete: (() -> Unit)? = null) {
    val c = Clavin.colors
    val whenText = remember(item.time) {
        SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(item.time))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .softCard(20.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(if (item.isLua) Icons.Rounded.Code else Icons.Rounded.Palette, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.name, color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                "${item.target} · $whenText", color = c.muted, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (onDelete != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(40.dp)
                    .bouncyClick(onDelete)
                    .clip(CircleShape)
                    .background(Brand.Blue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete ${item.name}", tint = c.blueText)
            }
        }
    }
}

@Composable
fun HistoryScreen(state: AppState, pickers: Pickers, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = Clavin.colors

    var confirm by remember { mutableStateOf<HistoryItem?>(null) }
    var needAccess by remember { mutableStateOf<HistoryItem?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Pair<String, String>?>(null) }

    fun appFor(item: HistoryItem): CloneApp? = CloneApp.fromKey(item.appKey) ?: state.cloneApp

    fun runDelete(item: HistoryItem) {
        val app = appFor(item)
        val spec = app?.let { state.luaSpec(it) }
        if (app == null || spec == null) {
            message = "Can't delete yet" to "ClavinManager doesn't know which app this script went to. Choose your clone app in Settings first."
            return
        }
        busy = true
        scope.launch {
            val r = LuaImporter.delete(context, spec, state.treeFor(app), item.name)
            busy = false
            when (r) {
                is ImportResult.Success -> state.removeHistory(item)
                is ImportResult.NeedAccess -> needAccess = item
                is ImportResult.Failure -> message = "Couldn't delete" to r.message
            }
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Appear(0) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("History", color = c.ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                if (state.history.isNotEmpty()) {
                    Text(
                        "Clear list",
                        color = c.blueText, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { state.clearHistory() }
                            .padding(8.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        if (state.history.isEmpty()) {
            Appear(1, Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ClavinLogo(72.dp)
                    Spacer(Modifier.height(18.dp))
                    Text("No imports yet", color = c.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Scripts and resource packs you import will be listed here.", color = c.muted, fontSize = 15.sp)
                    Spacer(Modifier.height(80.dp))
                }
            }
        } else {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Tap the bin to delete a script from your game folder.",
                    color = c.muted, fontSize = 13.sp
                )
                state.history.forEach { item ->
                    HistoryRow(item, onDelete = if (item.isLua) ({ confirm = item }) else null)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    // ---------------- dialogs ----------------

    val ask = confirm
    if (ask != null) {
        PopDialog(onDismiss = { confirm = null }) {
            IconBubble(Icons.Rounded.Delete)
            Spacer(Modifier.height(16.dp))
            DialogTitle("Delete ${ask.name}?")
            Spacer(Modifier.height(10.dp))
            DialogBody("This removes the script from your ClavinTool folder (${ask.target}).")
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Yes, delete") {
                confirm = null
                runDelete(ask)
            }
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Keep it") { confirm = null }
        }
    }

    if (busy) {
        PopDialog(onDismiss = {}, dismissible = false) {
            CircularProgressIndicator(color = Brand.Aqua, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(18.dp))
            DialogTitle("Deleting…")
        }
    }

    val access = needAccess
    if (access != null) {
        val spec = appFor(access)?.let { state.luaSpec(it) }
        if (spec != null) {
            AccessDialog(
                spec = spec,
                state = state,
                pickers = pickers,
                onGained = {
                    needAccess = null
                    runDelete(access)
                },
                onCancel = { needAccess = null }
            )
        } else {
            needAccess = null
        }
    }

    val m = message
    if (m != null) {
        PopDialog(onDismiss = { message = null }) {
            IconBubble(Icons.Rounded.Delete)
            Spacer(Modifier.height(16.dp))
            DialogTitle(m.first)
            Spacer(Modifier.height(10.dp))
            DialogBody(m.second)
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Okay") { message = null }
        }
    }
}

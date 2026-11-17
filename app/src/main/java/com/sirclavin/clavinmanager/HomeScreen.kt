package com.sirclavin.clavinmanager

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    state: AppState,
    modifier: Modifier,
    onOpenLoader: () -> Unit,
    onOpenPacks: () -> Unit,
    onSeeHistory: () -> Unit
) {
    val c = Clavin.colors
    var soon by remember { mutableStateOf<String?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Appear(0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClavinLogo(46.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Welcome back", color = c.muted, fontSize = 13.sp)
                    Row {
                        Text("Clavin", color = c.ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Manager", color = c.blueText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // Hero card
        Appear(1) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(Brand.Blue)
            ) {
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(Brand.White.copy(alpha = 0.08f), radius = size.width * 0.34f, center = Offset(size.width * 0.95f, size.height * 0.05f))
                    drawCircle(Brand.White.copy(alpha = 0.06f), radius = size.width * 0.22f, center = Offset(size.width * 0.05f, size.height * 1.0f))
                    drawCircle(Brand.Aqua, radius = 7.dp.toPx(), center = Offset(size.width - 34.dp.toPx(), 34.dp.toPx()))
                }
                Column(Modifier.padding(22.dp)) {
                    val ready = state.isSetupComplete
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Brand.White.copy(alpha = 0.16f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Brand.Aqua))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (ready) "Ready · ${state.cloneApp?.let { state.targetLabel(it) }}" else "Quick setup needed",
                            color = Brand.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Load Lua scripts\nin one tap",
                        color = Brand.White, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No more copy and paste. ClavinManager puts your scripts exactly where ClavinTool needs them.",
                        color = Brand.White.copy(alpha = 0.88f), fontSize = 14.sp, lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .bouncyClick(onOpenLoader)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brand.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Open Lua Script Loader", color = Brand.Blue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(26.dp))
        Appear(2) { SectionTitle("Your tools") }
        Spacer(Modifier.height(12.dp))

        Appear(3) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToolTile(Icons.Rounded.Code, "Lua Script Loader", "Add scripts to ClavinTool", active = true, Modifier.weight(1f), onOpenLoader)
                    ToolTile(Icons.Rounded.Palette, "Resource Pack Importer", "Add texture packs to Blockmango", active = true, Modifier.weight(1f), onOpenPacks)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ToolTile(Icons.Rounded.Folder, "Script Library", "Keep scripts organised", active = false, Modifier.weight(1f)) { soon = "Script Library" }
                    ToolTile(Icons.Rounded.Backup, "Backup", "Save and restore setups", active = false, Modifier.weight(1f)) { soon = "Backup" }
                }
            }
        }

        Spacer(Modifier.height(26.dp))
        Appear(4) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Recent imports")
                if (state.history.isNotEmpty()) {
                    Text(
                        "See all",
                        color = c.blueText, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSeeHistory() }
                            .padding(6.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Appear(5) {
            if (state.history.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().softCard(22.dp).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Nothing here yet", color = c.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Import your first Lua script and it will show up here.", color = c.muted, fontSize = 14.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.history.take(3).forEach { HistoryRow(it) }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }

    val s = soon
    if (s != null) {
        PopDialog(onDismiss = { soon = null }) {
            IconBubble(Icons.Rounded.Bolt)
            Spacer(Modifier.height(16.dp))
            DialogTitle("$s is coming soon")
            Spacer(Modifier.height(10.dp))
            DialogBody("We're still building this one. Stay tuned!")
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Okay") { soon = null }
        }
    }
}

@Composable
private fun ToolTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val c = Clavin.colors
    Column(
        modifier
            .height(160.dp)
            .bouncyClick(onClick)
            .softCard(24.dp)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            IconBubble(icon, size = 46.dp)
            if (active) Badge("READY", filled = true) else Badge("SOON")
        }
        Spacer(Modifier.weight(1f))
        Text(title, color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, color = c.muted, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

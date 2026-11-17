package com.sirclavin.clavinmanager

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class PackPhase { Loading, Questions, Ready }
private enum class PackQ { Target, CloneApp, Skypixel, Steps }

private fun blockmangoName(state: AppState, target: BmTarget): String =
    if (target == BmTarget.REAL) "Real Blockmango" else "Cloned Blockmango"

@Composable
fun PackScreen(
    state: AppState,
    pickers: Pickers,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val c = Clavin.colors
    var phase by remember { mutableStateOf(if (state.packReady) PackPhase.Ready else PackPhase.Loading) }

    LaunchedEffect(phase) {
        if (phase == PackPhase.Loading) {
            delay(1400)
            phase = PackPhase.Questions
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .bouncyClick(onBack)
                    .clip(CircleShape)
                    .background(c.card),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.ink)
            }
            Spacer(Modifier.width(12.dp))
            Text("Resource Pack Importer", color = c.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (phase == PackPhase.Ready) {
                PackImportPanel(state, pickers, onOpenSettings)
            } else {
                PreferencesBackdrop(loading = phase == PackPhase.Loading)
            }
            if (phase == PackPhase.Questions) {
                PackQuestions(state, pickers, onDone = { phase = PackPhase.Ready }, onExit = onBack)
            }
        }
    }
}

// ------------------------------------------------------------------ questions

@Composable
private fun PackQuestions(state: AppState, pickers: Pickers, onDone: () -> Unit, onExit: () -> Unit) {
    var q by remember {
        mutableStateOf(
            when {
                state.bmTarget == null -> PackQ.Target
                state.bmTarget == BmTarget.CLONE && !state.cloneKnown -> PackQ.CloneApp
                else -> PackQ.Skypixel
            }
        )
    }

    PopDialog(onDismiss = onExit) {
        AnimatedContent(
            targetState = q,
            transitionSpec = {
                (slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(200)) { -it / 3 } + fadeOut(tween(160)))
            },
            label = "packQuestion"
        ) { step ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                when (step) {
                    PackQ.Target -> {
                        IconBubble(Icons.Rounded.Palette)
                        Spacer(Modifier.height(16.dp))
                        DialogTitle("Are you looking to import into?")
                        Spacer(Modifier.height(20.dp))
                        OptionRow(Icons.Rounded.Apps, "Real Blockmango") {
                            state.answerBmTarget(BmTarget.REAL)
                            q = PackQ.Skypixel
                        }
                        Spacer(Modifier.height(10.dp))
                        OptionRow(Icons.Rounded.Extension, "Cloned Blockmango") {
                            state.answerBmTarget(BmTarget.CLONE)
                            q = if (state.cloneKnown) PackQ.Skypixel else PackQ.CloneApp
                        }
                    }

                    PackQ.CloneApp -> {
                        IconBubble(Icons.Rounded.Folder)
                        Spacer(Modifier.height(16.dp))
                        DialogTitle("What cloning app are you using?")
                        Spacer(Modifier.height(20.dp))
                        CloneAppOptions(state, pickers) { q = PackQ.Skypixel }
                    }

                    PackQ.Skypixel -> {
                        val target = state.bmTarget ?: BmTarget.REAL
                        IconBubble(Icons.Rounded.Palette)
                        Spacer(Modifier.height(16.dp))
                        DialogTitle("Have you loaded Sky Pixel in ${blockmangoName(state, target)}?")
                        Spacer(Modifier.height(24.dp))
                        PrimaryButton("Yes") {
                            state.answerSkypixel(true)
                            onDone()
                        }
                        Spacer(Modifier.height(10.dp))
                        SecondaryButton("No") { q = PackQ.Steps }
                    }

                    PackQ.Steps -> {
                        val target = state.bmTarget ?: BmTarget.REAL
                        val first = if (target == BmTarget.CLONE) {
                            val app = state.cloneApp
                            "Open Blockmango inside ${if (app != null) state.targetLabel(app) else "your clone app"}"
                        } else {
                            "Open Real Blockmango"
                        }
                        DialogTitle("Load Sky Pixel first")
                        Spacer(Modifier.height(8.dp))
                        DialogBody("Please proceed after loading Sky Pixel in ${blockmangoName(state, target)}.")
                        Spacer(Modifier.height(18.dp))
                        StepLine(1, first)
                        Spacer(Modifier.height(10.dp))
                        StepLine(2, "Search for a game called Sky Pixel")
                        Spacer(Modifier.height(10.dp))
                        StepLine(3, "After the game loads, quit it")
                        Spacer(Modifier.height(10.dp))
                        StepLine(4, "Come back here")
                        Spacer(Modifier.height(22.dp))
                        PrimaryButton("Got it", onClick = onExit)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepLine(number: Int, text: String) {
    val c = Clavin.colors
    Row(
        Modifier
            .fillMaxWidth()
            .softCard(16.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Brand.Blue),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", color = Brand.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, color = c.ink, fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
    }
}

// ------------------------------------------------------------------ "Select your texture pack"

@Composable
private fun PackImportPanel(state: AppState, pickers: Pickers, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = Clavin.colors

    var pending by remember { mutableStateOf<PickedFile?>(null) }
    var askAccess by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ImportResult?>(null) }
    var notice by remember { mutableStateOf<Pair<String, String>?>(null) }

    val target = state.bmTarget ?: BmTarget.REAL
    val app = state.cloneApp
    val destLabel = if (target == BmTarget.REAL) "Real Blockmango"
    else "Cloned · ${app?.let { state.targetLabel(it) } ?: "choose app"}"

    fun runImport(file: PickedFile) {
        val spec = state.packSpec(target, app)
        if (spec == null) {
            notice = "Pick your clone app" to "ClavinManager doesn't know which clone app to use yet. Choose it in Settings."
            return
        }
        importing = true
        scope.launch {
            val r = PackImporter.import(context, spec, state.packTree(target, app), file.uri, file.name)
            importing = false
            if (r is ImportResult.NeedAccess) {
                pending = file
                askAccess = true
            } else {
                result = r
                if (r is ImportResult.Success) {
                    state.addHistory(
                        HistoryItem(
                            name = file.name,
                            time = System.currentTimeMillis(),
                            target = destLabel,
                            kind = "pack",
                            appKey = if (target == BmTarget.REAL) "real" else (app?.key ?: ""),
                            extra = r.extracted.joinToString(", ")
                        )
                    )
                }
            }
        }
    }

    fun pickFile() {
        pickers.pickLua { uri ->
            if (uri != null) {
                val name = LuaImporter.displayName(context, uri) ?: "pack.zip"
                if (!Archives.isSupported(name)) {
                    notice = "That format isn't supported" to
                        "“$name” can't be unpacked. Supported: ${Archives.SUPPORTED_TEXT}"
                } else {
                    runImport(PickedFile(uri, name))
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Appear(0) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .softCard(20.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBubble(Icons.Rounded.Palette, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Importing into", color = c.muted, fontSize = 12.sp)
                    Text(destLabel, color = c.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Change",
                    color = c.blueText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenSettings() }
                        .padding(8.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Appear(1) {
            Text(
                "Select your texture pack:",
                color = c.ink, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(16.dp))

        Appear(2) {
            val dashColor = Brand.Blue.copy(alpha = 0.45f)
            val floating = rememberInfiniteTransition(label = "float")
            val dy by floating.animateFloat(
                initialValue = -6f,
                targetValue = 6f,
                animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "dy"
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(c.card)
                    .drawBehind {
                        drawRoundRect(
                            color = dashColor,
                            cornerRadius = CornerRadius(28.dp.toPx()),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 16f))
                            )
                        )
                    }
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.graphicsLayer { translationY = dy }) {
                    IconBubble(Icons.Rounded.UploadFile, size = 76.dp)
                }
                Spacer(Modifier.height(18.dp))
                PrimaryButton("Choose texture pack") { pickFile() }
                Spacer(Modifier.height(12.dp))
                Text("Opens your Downloads folder", color = c.muted, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    Archives.SUPPORTED_TEXT, color = c.muted, fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    // ---------------- dialogs ----------------

    if (askAccess) {
        val spec = state.packSpec(target, app)
        if (spec != null) {
            AccessDialog(
                spec = spec,
                state = state,
                pickers = pickers,
                onGained = {
                    askAccess = false
                    pending?.let { runImport(it) }
                    pending = null
                },
                onCancel = {
                    askAccess = false
                    pending = null
                }
            )
        }
    }

    if (importing) {
        PopDialog(onDismiss = {}, dismissible = false) {
            CircularProgressIndicator(color = Brand.Aqua, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(18.dp))
            DialogTitle("Unpacking…")
        }
    }

    val r = result
    if (r is ImportResult.Success) {
        PopDialog(onDismiss = { result = null }) {
            SuccessCheck()
            Spacer(Modifier.height(18.dp))
            DialogTitle(r.name)
            Spacer(Modifier.height(6.dp))
            DialogBody("has been imported successfully")
            if (r.extracted.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                val shown = r.extracted.take(3).joinToString(", ")
                val more = if (r.extracted.size > 3) " +${r.extracted.size - 3} more" else ""
                Text("Replaced: $shown$more", color = c.muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Awesome!") { result = null }
        }
    } else if (r is ImportResult.Failure) {
        PopDialog(onDismiss = { result = null }) {
            IconBubble(Icons.Rounded.Close)
            Spacer(Modifier.height(16.dp))
            DialogTitle("Couldn't import")
            Spacer(Modifier.height(10.dp))
            DialogBody(r.message)
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Try again") {
                result = null
                pickFile()
            }
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Close") { result = null }
        }
    }

    val n = notice
    if (n != null) {
        PopDialog(onDismiss = { notice = null }) {
            IconBubble(Icons.Rounded.Palette)
            Spacer(Modifier.height(16.dp))
            DialogTitle(n.first)
            Spacer(Modifier.height(10.dp))
            DialogBody(n.second)
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Okay") { notice = null }
        }
    }
}

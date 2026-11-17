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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
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

private enum class Phase { Loading, Questions, Ready }
private enum class Q { Installed, Cloned, App, NeedInstall, NeedClone }

private data class Notice(val title: String, val message: String)

@Composable
fun LoaderScreen(
    state: AppState,
    pickers: Pickers,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val c = Clavin.colors
    var phase by remember { mutableStateOf(if (state.isSetupComplete) Phase.Ready else Phase.Loading) }

    LaunchedEffect(phase) {
        if (phase == Phase.Loading) {
            delay(1600)
            phase = Phase.Questions
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
            Text("Lua Script Loader", color = c.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (phase == Phase.Ready) {
                ImportPanel(state, pickers, onOpenSettings)
            } else {
                PreferencesBackdrop(loading = phase == Phase.Loading)
            }
            if (phase == Phase.Questions) {
                QuestionFlow(state, pickers, onDone = { phase = Phase.Ready }, onExit = onBack)
            }
        }
    }
}

// ------------------------------------------------------------------ "SELECT YOUR PREFERENCES"

@Composable
fun PreferencesBackdrop(loading: Boolean) {
    val c = Clavin.colors
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
        ) { ClavinLogo(96.dp) }
        Spacer(Modifier.height(28.dp))
        Text("Select your preferences", color = c.ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        if (loading) LoadingDots() else Text("Just a few quick questions", color = c.muted, fontSize = 15.sp)
    }
}

// ------------------------------------------------------------------ the three questions

@Composable
private fun QuestionFlow(state: AppState, pickers: Pickers, onDone: () -> Unit, onExit: () -> Unit) {
    val context = LocalContext.current
    val c = Clavin.colors
    var q by remember {
        mutableStateOf(
            when {
                state.installed != true -> Q.Installed
                state.cloned != true -> Q.Cloned
                else -> Q.App
            }
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    fun finish(app: CloneApp) {
        state.chooseCloneApp(app)
        state.updateKnown(Detector.installedKeys(context))
        onDone()
    }

    PopDialog(onDismiss = onExit) {
        AnimatedContent(
            targetState = q,
            transitionSpec = {
                (slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(200)) { -it / 3 } + fadeOut(tween(160)))
            },
            label = "question"
        ) { step ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                when (step) {
                    Q.Installed -> {
                        StepDots(1, 3)
                        Spacer(Modifier.height(20.dp))
                        IconBubble(Icons.Rounded.Extension)
                        Spacer(Modifier.height(16.dp))
                        DialogTitle("Do you have ClavinTool installed?")
                        Spacer(Modifier.height(24.dp))
                        PrimaryButton("Yes") {
                            state.answerInstalled(true)
                            q = Q.Cloned
                        }
                        Spacer(Modifier.height(10.dp))
                        SecondaryButton("No") { q = Q.NeedInstall }
                    }

                    Q.Cloned -> {
                        StepDots(2, 3)
                        Spacer(Modifier.height(20.dp))
                        IconBubble(Icons.Rounded.Folder)
                        Spacer(Modifier.height(16.dp))
                        DialogTitle("Have you cloned your ClavinTool on the clone app of your choice?")
                        Spacer(Modifier.height(24.dp))
                        PrimaryButton("Yes") {
                            state.answerCloned(true)
                            q = Q.App
                        }
                        Spacer(Modifier.height(10.dp))
                        SecondaryButton("No") { q = Q.NeedClone }
                    }

                    Q.App -> {
                        StepDots(3, 3)
                        Spacer(Modifier.height(20.dp))
                        DialogTitle("What cloning app are you using?")
                        Spacer(Modifier.height(20.dp))
                        CloneAppOptions(state, pickers) { finish(it) }
                    }

                    Q.NeedInstall -> InfoStep("Please proceed after installing ClavinTool", onExit)
                    Q.NeedClone -> InfoStep("Please proceed after cloning ClavinTool", onExit)
                }
            }
        }
    }
}

@Composable
private fun InfoStep(message: String, onOk: () -> Unit) {
    Spacer(Modifier.height(4.dp))
    IconBubble(Icons.Rounded.Lock)
    Spacer(Modifier.height(16.dp))
    DialogTitle(message)
    Spacer(Modifier.height(24.dp))
    PrimaryButton("Got it", onClick = onOk)
}

// ------------------------------------------------------------------ "Select the Lua file to import"

@Composable
private fun ImportPanel(state: AppState, pickers: Pickers, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = Clavin.colors

    var pending by remember { mutableStateOf<PickedFile?>(null) }
    var askAccess by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ImportResult?>(null) }
    var showTip by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<Notice?>(null) }
    var banner by remember { mutableStateOf<String?>(null) }
    var suggest by remember { mutableStateOf<CloneApp?>(null) }

    val app = state.cloneApp ?: CloneApp.CLAVINBOX

    // Auto-detect: did the user move to a different clone app?
    LaunchedEffect(Unit) {
        when (val d = Detector.check(context, state)) {
            is Detection.Switched -> banner = "We noticed you now use ${d.to.label}, so we switched to it for you."
            is Detection.Suggest -> suggest = d.app
            null -> Unit
        }
    }

    fun runImport(file: PickedFile) {
        val spec = state.luaSpec(app)
        if (spec == null) {
            notice = Notice("Pick your clone app", "ClavinManager doesn't know which app folder to use yet. Choose it in Settings.")
            return
        }
        importing = true
        scope.launch {
            val r = LuaImporter.import(context, spec, state.treeFor(app), file.uri, file.name)
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
                            target = state.targetLabel(app),
                            kind = "lua",
                            appKey = app.key
                        )
                    )
                }
            }
        }
    }

    fun proceed(file: PickedFile) = runImport(file)

    fun pickFile() {
        pickers.pickLua { uri ->
            if (uri != null) {
                val name = LuaImporter.displayName(context, uri) ?: "script.lua"
                if (!name.endsWith(".lua", ignoreCase = true)) {
                    notice = Notice("That's not a .lua file", "“$name” can't be imported. Please choose a file that ends with .lua.")
                } else {
                    proceed(PickedFile(uri, name))
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
        // Where scripts are going
        Appear(0) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .softCard(20.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBubble(Icons.Rounded.Folder, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Saving scripts to", color = c.muted, fontSize = 12.sp)
                    Text(state.targetLabel(app), color = c.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
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

        val b = banner
        if (b != null) {
            Spacer(Modifier.height(12.dp))
            Appear(0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brand.Blue)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Brand.Aqua))
                    Spacer(Modifier.width(12.dp))
                    Text(b, color = Brand.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Rounded.Close, "Dismiss", tint = Brand.White,
                        modifier = Modifier.size(22.dp).clickable { banner = null }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Appear(1) {
            Text(
                "Select the Lua file to import:",
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
                PrimaryButton("Import .lua") { pickFile() }
                Spacer(Modifier.height(12.dp))
                Text("Opens your Downloads folder", color = c.muted, fontSize = 13.sp)
            }
        }

        // The "?" hint, shown once at least one script has been imported.
        if (state.totalImports > 0) {
            Spacer(Modifier.height(20.dp))
            Appear(0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .bouncyClick { showTip = true }
                        .softCard(20.dp)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuestionBadge(40.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Changed your preferences? You can update them anytime in Settings.",
                        color = c.ink, fontSize = 14.sp, lineHeight = 19.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    // ---------------- dialogs ----------------

    if (askAccess) {
        val spec = state.luaSpec(app)
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
            DialogTitle("Importing…")
        }
    }

    val r = result
    if (r is ImportResult.Success) {
        PopDialog(onDismiss = {
            result = null
            if (!state.hintShown) showTip = true
        }) {
            SuccessCheck()
            Spacer(Modifier.height(18.dp))
            DialogTitle(r.name)
            Spacer(Modifier.height(6.dp))
            DialogBody("has been imported successfully")
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Awesome!") {
                result = null
                if (!state.hintShown) showTip = true
            }
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

    if (showTip) {
        PopDialog(onDismiss = {
            showTip = false
            state.markHintShown()
        }) {
            QuestionBadge(64.dp)
            Spacer(Modifier.height(16.dp))
            DialogTitle("Changed your preferences?")
            Spacer(Modifier.height(10.dp))
            DialogBody("You can update your answers anytime in Settings.")
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Open Settings") {
                showTip = false
                state.markHintShown()
                onOpenSettings()
            }
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Got it") {
                showTip = false
                state.markHintShown()
            }
        }
    }

    val n = notice
    if (n != null) {
        PopDialog(onDismiss = { notice = null }) {
            IconBubble(Icons.Rounded.Code)
            Spacer(Modifier.height(16.dp))
            DialogTitle(n.title)
            Spacer(Modifier.height(10.dp))
            DialogBody(n.message)
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Okay") { notice = null }
        }
    }

    val s = suggest
    if (s != null) {
        PopDialog(onDismiss = { suggest = null }) {
            IconBubble(Icons.Rounded.Apps)
            Spacer(Modifier.height(16.dp))
            DialogTitle("Switch to ${s.label}?")
            Spacer(Modifier.height(10.dp))
            DialogBody("We noticed ${s.label} is now installed. Want to save your scripts there instead?")
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Yes, switch") {
                state.chooseCloneApp(s)
                suggest = null
            }
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Keep ${state.targetLabel(app)}") { suggest = null }
        }
    }
}

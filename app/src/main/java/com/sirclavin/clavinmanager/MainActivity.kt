package com.sirclavin.clavinmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class Screen { Main, Loader, Packs }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClavinTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val c = Clavin.colors
    val state = remember { AppState(context) }
    val pickers = rememberPickers()
    var tab by remember { mutableIntStateOf(0) }
    var screen by remember { mutableStateOf(Screen.Main) }

    BackHandler(enabled = screen != Screen.Main) { screen = Screen.Main }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                if (targetState != Screen.Main) {
                    (slideInHorizontally(tween(320)) { it } + fadeIn(tween(320))) togetherWith
                        (slideOutHorizontally(tween(320)) { -it / 4 } + fadeOut(tween(200)))
                } else {
                    (slideInHorizontally(tween(320)) { -it / 4 } + fadeIn(tween(320))) togetherWith
                        (slideOutHorizontally(tween(320)) { it } + fadeOut(tween(200)))
                }
            },
            label = "root"
        ) { current ->
            Box(Modifier.fillMaxSize().background(c.bg)) {
                when (current) {
                    Screen.Loader -> LoaderScreen(
                        state = state,
                        pickers = pickers,
                        onBack = { screen = Screen.Main },
                        onOpenSettings = {
                            tab = 2
                            screen = Screen.Main
                        }
                    )
                    Screen.Packs -> PackScreen(
                        state = state,
                        pickers = pickers,
                        onBack = { screen = Screen.Main },
                        onOpenSettings = {
                            tab = 2
                            screen = Screen.Main
                        }
                    )
                    Screen.Main -> MainTabs(
                        state, pickers, tab,
                        onTab = { tab = it },
                        onOpenLoader = { screen = Screen.Loader },
                        onOpenPacks = { screen = Screen.Packs }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainTabs(
    state: AppState,
    pickers: Pickers,
    tab: Int,
    onTab: (Int) -> Unit,
    onOpenLoader: () -> Unit,
    onOpenPacks: () -> Unit
) {
    val c = Clavin.colors
    Scaffold(
        containerColor = c.bg,
        bottomBar = { ClavinBottomBar(tab, onTab) }
    ) { padding ->
        Crossfade(targetState = tab, animationSpec = tween(220), label = "tabs") { t ->
            when (t) {
                0 -> HomeScreen(state, Modifier.padding(padding), onOpenLoader, onOpenPacks, onSeeHistory = { onTab(1) })
                1 -> HistoryScreen(state, pickers, Modifier.padding(padding))
                else -> SettingsScreen(state, pickers, Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun ClavinBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val c = Clavin.colors
    Column(Modifier.background(c.bg)) {
        HorizontalDivider(color = c.border)
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BarItem(Icons.Rounded.Home, "Home", selected == 0) { onSelect(0) }
            BarItem(Icons.Rounded.History, "History", selected == 1) { onSelect(1) }
            BarItem(Icons.Rounded.Settings, "Settings", selected == 2) { onSelect(2) }
        }
    }
}

@Composable
private fun BarItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Clavin.colors
    val pill by animateColorAsState(
        if (selected) Brand.Blue.copy(alpha = 0.14f) else Brand.Blue.copy(alpha = 0f),
        tween(220), label = "pill"
    )
    val tint by animateColorAsState(if (selected) c.blueText else c.muted, tween(220), label = "tint")
    Row(
        Modifier
            .bouncyClick(onClick)
            .clip(RoundedCornerShape(50))
            .background(pill)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(tween(220)) + fadeIn(tween(220)),
            exit = shrinkHorizontally(tween(180)) + fadeOut(tween(120))
        ) {
            Row {
                Spacer(Modifier.width(8.dp))
                Text(label, color = tint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

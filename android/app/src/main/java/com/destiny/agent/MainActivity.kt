package com.destiny.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.destiny.agent.data.DestinyStore
import com.destiny.agent.ui.DestinyTabBar
import com.destiny.agent.ui.Tab
import com.destiny.agent.ui.screens.CloudScreen
import com.destiny.agent.ui.screens.LoginScreen
import com.destiny.agent.ui.screens.MeScreen
import com.destiny.agent.ui.screens.MemoryScreen
import com.destiny.agent.ui.screens.ProactiveScreen
import com.destiny.agent.ui.screens.TaskDetailScreen
import com.destiny.agent.ui.screens.TalkScreen
import com.destiny.agent.ui.theme.DestinyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Destiny 原生入口
 *
 * 不用 navigation-compose —— 五个 Tab + 一个任务详情栈，手写更轻、更可控。
 * 路由模型：tabName（五个目的地）+ taskId（盖在上面的详情层）。
 */
class MainActivity : ComponentActivity() {

    private lateinit var store: DestinyStore
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        store = DestinyStore(applicationContext)
        store.start(appScope)

        setContent {
            val state by store.state.collectAsState()
            DestinyTheme(dark = state.theme == "dark") {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(store)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(store: DestinyStore) {
    val state by store.state.collectAsState()

    // 登录门控
    if (!state.loggedIn) {
        LoginScreen(onLogin = { method, id -> store.login(method, id) })
        return
    }

    var tabName by rememberSaveable { mutableStateOf(Tab.Talk.name) }
    var taskId by remember { mutableStateOf<String?>(null) }
    val tab = Tab.valueOf(tabName)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            Modifier
                .weight(1f)
                .statusBarsPadding()
        ) {
            val current = taskId
            if (current != null) {
                TaskDetailScreen(
                    store = store,
                    taskId = current,
                    onBack = { taskId = null }
                )
            } else {
                when (tab) {
                    Tab.Talk -> TalkScreen(
                        store = store,
                        onOpenTask = { taskId = it },
                        onOpenProactive = { tabName = Tab.Proactive.name },
                        onOpenMemory = { tabName = Tab.Memory.name },
                        onOpenCloud = { tabName = Tab.Cloud.name }
                    )
                    Tab.Proactive -> ProactiveScreen(
                        store = store,
                        onOpenTask = { taskId = it }
                    )
                    Tab.Memory -> MemoryScreen(store)
                    Tab.Cloud -> CloudScreen(store)
                    Tab.Me -> MeScreen(store)
                }
            }
        }

        if (taskId == null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
            ) {
                DestinyTabBar(
                    current = tab,
                    onSelect = { tabName = it.name },
                    pendingCount = state.pendingCount()
                )
            }
        }
    }
}

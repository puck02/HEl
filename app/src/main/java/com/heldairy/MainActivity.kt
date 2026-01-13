package com.heldairy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heldairy.feature.report.ui.DailyReportRoute
import com.heldairy.feature.settings.ui.SettingsRoute
import com.heldairy.ui.theme.HElDairyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HElDairyApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HElDairyApp() {
    HElDairyTheme {
        val tabs = remember {
            listOf(
                ConciergeTab(
                    title = "问候",
                    description = "欢迎来到生活管家，准备开始今日的关怀对话。",
                    icon = Icons.AutoMirrored.Outlined.Chat
                ),
                ConciergeTab(
                    title = "日报",
                    description = "今日的基础问题会在这里依序出现。",
                    icon = Icons.AutoMirrored.Outlined.EventNote
                ),
                ConciergeTab(
                    title = "洞察",
                    description = "在这里将展示近期趋势与提示。",
                    icon = Icons.Outlined.Assessment
                ),
                ConciergeTab(
                    title = "设置",
                    description = "管理 API Key、偏好与导出工具。",
                    icon = Icons.Outlined.Settings
                )
            )
        }
        var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = tabs[selectedIndex].title) }
                )
            },
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index },
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (selectedIndex) {
                1 -> DailyReportRoute(paddingValues = innerPadding)
                3 -> SettingsRoute(paddingValues = innerPadding)
                else -> TabContent(
                    paddingValues = innerPadding,
                    content = tabs[selectedIndex].description
                )
            }
        }
    }
}

@Composable
private fun TabContent(paddingValues: PaddingValues, content: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "功能逐步上线中，生活管家会很快来到这里。",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewHElDairyApp() {
    HElDairyTheme {
        HElDairyApp()
    }
}

data class ConciergeTab(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

package com.nutriai.mvp.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nutriai.mvp.ui.agent.AgentTabScreen
import com.nutriai.mvp.ui.analytics.AnalyticsTabScreen
import com.nutriai.mvp.ui.preview.NutriPreviewSurface
import com.nutriai.mvp.ui.profile.ProfileTabScreen

enum class MainTab(val label: String) {
    Agent("Агент"),
    Analytics("Аналитика"),
    Profile("Профиль"),
}

@Composable
fun MainScreen(
    selectedTab: MainTab = MainTab.Agent,
    onTabSelected: (MainTab) -> Unit = {},
    modifier: Modifier = Modifier,
    profileContent: @Composable () -> Unit = { ProfileTabScreen() },
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            Icon(
                                when (tab) {
                                    MainTab.Agent -> Icons.Default.SmartToy
                                    MainTab.Analytics -> Icons.Default.ShowChart
                                    MainTab.Profile -> Icons.Default.Person
                                },
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            MainTab.Agent -> AgentTabScreen(Modifier.padding(padding))
            MainTab.Analytics -> AnalyticsTabScreen(Modifier.padding(padding))
            MainTab.Profile -> Box(Modifier.padding(padding)) {
                profileContent()
            }
        }
    }
}

@Preview(name = "S5 Main — Agent", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun MainAgentPreview() {
    NutriPreviewSurface { MainScreen(selectedTab = MainTab.Agent) }
}

@Preview(name = "S5 Main — Analytics", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun MainAnalyticsPreview() {
    NutriPreviewSurface { MainScreen(selectedTab = MainTab.Analytics) }
}

@Preview(name = "S5 Main — Profile", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun MainProfilePreview() {
    NutriPreviewSurface { MainScreen(selectedTab = MainTab.Profile) }
}

@Preview(name = "S5 Main — interactive", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun MainInteractivePreview() {
    var tab by remember { mutableIntStateOf(0) }
    NutriPreviewSurface {
        MainScreen(
            selectedTab = MainTab.entries[tab],
            onTabSelected = { selected -> tab = MainTab.entries.indexOf(selected) },
        )
    }
}

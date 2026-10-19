package com.nutriai.mvp.ui.agent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nutriai.mvp.data.MockData
import com.nutriai.mvp.ui.components.ChatBubble
import com.nutriai.mvp.ui.components.DiaryEntryCardUi
import com.nutriai.mvp.ui.preview.NutriPreviewSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentTabScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("NutriAI") },
                actions = {
                    TextButton(onClick = {}) {
                        Text("Сегодня")
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                },
            )
        },
        bottomBar = { AgentInputBar() },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("Утро, ${MockData.userName}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(MockData.chatMessages) { msg ->
                ChatBubble(text = msg.text, isUser = msg.isUser)
            }
            item {
                DiaryEntryCardUi(entry = MockData.entryCard)
            }
            item { Text("ИИ оценил порцию — подтвердите или измените.", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun AgentInputBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = {}) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Фото")
        }
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("Сообщение…") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        IconButton(onClick = {}) {
            Icon(Icons.Default.Mic, contentDescription = "Голос")
        }
        IconButton(onClick = {}) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
        }
    }
}

@Preview(name = "S5a Agent Tab", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun AgentTabPreview() {
    NutriPreviewSurface { AgentTabScreen() }
}

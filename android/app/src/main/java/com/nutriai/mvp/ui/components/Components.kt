package com.nutriai.mvp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nutriai.mvp.data.DiaryEntryCard
import com.nutriai.mvp.data.Macros
import com.nutriai.mvp.theme.AiBubble
import com.nutriai.mvp.theme.GreenContainer
import com.nutriai.mvp.theme.GreenPrimary
import com.nutriai.mvp.theme.UserBubble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutriTopBar(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onBack: (() -> Unit)? = null,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            }
        },
        actions = {
            if (actionLabel != null) {
                TextButton(onClick = {}) {
                    Text(actionLabel)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
fun ChatBubble(
    text: String,
    isUser: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg = if (isUser) UserBubble else AiBubble
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp,
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = GreenPrimary,
                modifier = Modifier
                    .padding(end = 8.dp, top = 4.dp)
                    .size(20.dp),
            )
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = bg),
            shape = shape,
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun DiaryEntryCardUi(
    entry: DiaryEntryCard,
    modifier: Modifier = Modifier,
    showActions: Boolean = true,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GreenContainer),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Запись в дневнике",
                style = MaterialTheme.typography.labelLarge,
                color = GreenPrimary,
            )
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "${entry.portionGrams} g",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = entry.macros.formattedShort(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (showActions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                        Text("Изменить")
                    }
                    OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                        Text("Подтвердить")
                    }
                }
            }
        }
    }
}

@Composable
fun MacroChipRow(macros: Macros, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MacroChip("Б ${macros.protein}g", Modifier.weight(1f))
        MacroChip("Ж ${macros.fat}g", Modifier.weight(1f))
        MacroChip("У ${macros.carbs}g", Modifier.weight(1f))
    }
}

@Composable
private fun MacroChip(label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun NutriLogoBlock(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = null,
            tint = GreenPrimary,
            modifier = Modifier.size(72.dp),
        )
        Text(
            text = "NutriAI",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = GreenPrimary,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "Дневник питания с ИИ",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

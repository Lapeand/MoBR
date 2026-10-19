package com.nutriai.mvp.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nutriai.mvp.data.MealRecommendation
import com.nutriai.mvp.data.MockData
import com.nutriai.mvp.theme.GreenContainer
import com.nutriai.mvp.theme.GreenPrimary
import com.nutriai.mvp.ui.components.NutriTopBar
import com.nutriai.mvp.ui.preview.NutriPreviewSurface

@Composable
fun ProfileTabScreen(
    onSettingsClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onRecommendationDetailClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            NutriTopBar(title = "Профиль", actionLabel = null)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = GreenContainer),
                    ) {
                        Text(
                            text = MockData.userName.take(1),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleLarge,
                            color = GreenPrimary,
                        )
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text("${MockData.userName}, цель: ${MockData.goalLabel}", fontWeight = FontWeight.SemiBold)
                        Text(
                            "${MockData.weightKg} кг · ${MockData.heightCm} см · ${MockData.gender}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Настройки")
                }
            }
            OutlinedButton(onClick = onEditProfileClick, modifier = Modifier.padding(top = 16.dp)) {
                Text("Изменить профиль")
            }
            Text(
                text = "Рекомендации на сегодня",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
            )
            MockData.recommendations.forEach { rec ->
                RecommendationCard(
                    rec,
                    onDetailClick = onRecommendationDetailClick,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    rec: MealRecommendation,
    onDetailClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GreenContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(rec.mealLabel, style = MaterialTheme.typography.labelLarge, color = GreenPrimary)
            Text(rec.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            Text(rec.description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            Text(
                "~${rec.macros.kcal} kcal · Б${rec.macros.protein} Ж${rec.macros.fat} У${rec.macros.carbs}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            TextButton(onClick = onDetailClick) { Text("Подробнее") }
        }
    }
}

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSave: () -> Unit = onBack,
    modifier: Modifier = Modifier,
) {
    Scaffold(topBar = { NutriTopBar(title = "Профиль", onBack = onBack) }) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
        ) {
            OutlinedTextField(
                value = MockData.weightKg.toString(),
                onValueChange = {},
                label = { Text("Вес, кг") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = MockData.heightCm.toString(),
                onValueChange = {},
                label = { Text("Рост, см") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Цель", style = MaterialTheme.typography.labelLarge)
            FilterChip(selected = true, onClick = {}, label = { Text("Похудение ${MockData.goalLabel}") }, modifier = Modifier.padding(top = 8.dp))
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Сохранить") }
        }
    }
}

@Composable
fun RecommendationsDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rec = MockData.recommendations.first()
    Scaffold(topBar = { NutriTopBar(title = rec.mealLabel, onBack = onBack) }) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
        ) {
            Text(rec.title, style = MaterialTheme.typography.headlineSmall)
            Text(rec.detail, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp))
            Text(rec.macros.formattedShort(), modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(topBar = { NutriTopBar(title = "Настройки", onBack = onBack) }) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = {}) { Text("Политика конфиденциальности") }
            TextButton(onClick = {}) { Text("О приложении NutriAI MVP") }
            OutlinedButton(onClick = onLogout) { Text("Выйти из аккаунта") }
        }
    }
}

@Preview(name = "S5c Profile Tab", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun ProfileTabPreview() {
    NutriPreviewSurface { ProfileTabScreen() }
}

@Preview(name = "S7 Edit Profile", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun EditProfilePreview() {
    NutriPreviewSurface { EditProfileScreen(onBack = {}) }
}

@Preview(name = "S8 Recommendations Detail", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun RecommendationsDetailPreview() {
    NutriPreviewSurface { RecommendationsDetailScreen(onBack = {}) }
}

@Preview(name = "S9 Settings", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun SettingsPreview() {
    NutriPreviewSurface { SettingsScreen(onBack = {}) }
}

package com.nutriai.mvp.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nutriai.mvp.ui.preview.NutriPreviewSurface

enum class OnboardingStep { Goal, Body, Activity }

@Composable
fun OnboardingScreen(
    step: OnboardingStep,
    onNext: () -> Unit = {},
    onSkip: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val progress = when (step) {
        OnboardingStep.Goal -> 0.33f
        OnboardingStep.Body -> 0.66f
        OnboardingStep.Activity -> 1f
    }
    Scaffold { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                )
                TextButton(onClick = onSkip) { Text("Пропустить") }
            }
            Spacer(modifier = Modifier.height(24.dp))
            when (step) {
                OnboardingStep.Goal -> GoalStep()
                OnboardingStep.Body -> BodyStep()
                OnboardingStep.Activity -> ActivityStep()
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text(if (step == OnboardingStep.Activity) "Начать" else "Далее")
            }
        }
    }
}

@Composable
private fun GoalStep() {
    Text("Ваша цель", style = MaterialTheme.typography.headlineSmall)
    Spacer(modifier = Modifier.height(16.dp))
    GoalChip("Похудение", selected = false)
    Spacer(modifier = Modifier.height(8.dp))
    GoalChip("Поддержание формы", selected = true)
    Spacer(modifier = Modifier.height(8.dp))
    GoalChip("Набор массы", selected = false)
}

@Composable
private fun BodyStep() {
    Text("О вас", style = MaterialTheme.typography.headlineSmall)
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(value = "82", onValueChange = {}, label = { Text("Вес, кг") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(value = "178", onValueChange = {}, label = { Text("Рост, см") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoalChip("муж", selected = true, modifier = Modifier.weight(1f))
        GoalChip("жен", selected = false, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ActivityStep() {
    Text("Активность", style = MaterialTheme.typography.headlineSmall)
    Spacer(modifier = Modifier.height(16.dp))
    GoalChip("Мало двигаюсь", selected = false)
    Spacer(modifier = Modifier.height(8.dp))
    GoalChip("1–3 тренировки в неделю", selected = true)
    Spacer(modifier = Modifier.height(8.dp))
    GoalChip("Ежедневный спорт", selected = false)
}

@Composable
private fun GoalChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = {},
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview(name = "S4 Onboarding — цель", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun OnboardingGoalPreview() {
    NutriPreviewSurface { OnboardingScreen(step = OnboardingStep.Goal) }
}

@Preview(name = "S4 Onboarding — тело", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun OnboardingBodyPreview() {
    NutriPreviewSurface { OnboardingScreen(step = OnboardingStep.Body) }
}

@Preview(name = "S4 Onboarding — активность", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun OnboardingActivityPreview() {
    NutriPreviewSurface { OnboardingScreen(step = OnboardingStep.Activity) }
}

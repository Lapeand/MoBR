package com.nutriai.mvp.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nutriai.mvp.data.MockData
import com.nutriai.mvp.theme.GreenPrimary
import com.nutriai.mvp.ui.components.MacroChipRow
import com.nutriai.mvp.ui.components.NutriTopBar
import com.nutriai.mvp.ui.preview.NutriPreviewSurface

@Composable
fun AnalyticsTabScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { NutriTopBar(title = "Аналитика", actionLabel = "Неделя") },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KcalProgressCard()
            WeeklyBarChartCard()
            MacroDonutCard()
            DiaryTableCard()
        }
    }
}

@Composable
private fun KcalProgressCard() {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Сегодня", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${MockData.dailyCurrentKcal} / ${MockData.dailyTargetKcal} kcal",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            LinearProgressIndicator(
                progress = { MockData.dailyCurrentKcal.toFloat() / MockData.dailyTargetKcal },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            MacroChipRow(
                macros = MockData.macroTotals,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun WeeklyBarChartCard() {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Калории за неделю", style = MaterialTheme.typography.titleMedium)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(top = 16.dp),
            ) {
                val max = MockData.weeklyKcal.max().toFloat()
                val barWidth = size.width / (MockData.weeklyKcal.size * 2f)
                MockData.weeklyKcal.forEachIndexed { index, value ->
                    val barHeight = (value / max) * size.height * 0.85f
                    val left = index * barWidth * 2f + barWidth * 0.5f
                    drawRoundRect(
                        color = GreenPrimary,
                        topLeft = androidx.compose.ui.geometry.Offset(left, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8f, 8f),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MockData.weekDays.forEach { day ->
                    Text(day, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun MacroDonutCard() {
    val total = MockData.macroTotals.protein + MockData.macroTotals.fat + MockData.macroTotals.carbs
    val fatColor = MaterialTheme.colorScheme.tertiary
    val carbsColor = MaterialTheme.colorScheme.primaryContainer
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Баланс БЖУ", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Canvas(modifier = Modifier.height(96.dp).weight(1f)) {
                    val stroke = 28f
                    var start = -90f
                    val sweeps = listOf(
                        MockData.macroTotals.protein to GreenPrimary,
                        MockData.macroTotals.fat to fatColor,
                        MockData.macroTotals.carbs to carbsColor,
                    )
                    sweeps.forEach { (value, color) ->
                        val sweep = (value.toFloat() / total) * 360f
                        drawArc(
                            color = color,
                            startAngle = start,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                        )
                        start += sweep
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Б ${MockData.macroTotals.protein}g")
                    Text("Ж ${MockData.macroTotals.fat}g")
                    Text("У ${MockData.macroTotals.carbs}g")
                }
            }
        }
    }
}

@Composable
private fun DiaryTableCard() {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Дневник за день", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
            ) {
                TableCell("Приём", Modifier.weight(1.2f), header = true)
                TableCell("Ккал", Modifier.weight(0.7f), header = true)
                TableCell("Б", Modifier.weight(0.4f), header = true)
                TableCell("Ж", Modifier.weight(0.4f), header = true)
                TableCell("У", Modifier.weight(0.4f), header = true)
            }
            MockData.diaryRows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    TableCell(row.meal, Modifier.weight(1.2f))
                    TableCell("${row.macros.kcal}", Modifier.weight(0.7f))
                    TableCell("${row.macros.protein}", Modifier.weight(0.4f))
                    TableCell("${row.macros.fat}", Modifier.weight(0.4f))
                    TableCell("${row.macros.carbs}", Modifier.weight(0.4f))
                }
            }
        }
    }
}

@Composable
private fun TableCell(text: String, modifier: Modifier = Modifier, header: Boolean = false) {
    Text(
        text = text,
        modifier = modifier,
        style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
    )
}

@Preview(name = "S5b Analytics Tab", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun AnalyticsTabPreview() {
    NutriPreviewSurface { AnalyticsTabScreen() }
}

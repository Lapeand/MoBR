package com.nutriai.mvp.runtime

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.roundToInt

private val macroColors=listOf(Color(0xFF3788C7),Color(0xFFD69524),Color(0xFF369D7A))
private val macroNames=listOf("Белки","Жиры","Углеводы")
private fun shortDate(value: String)=LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd.MM"))

@Composable
internal fun ChartCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium)
            Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
internal fun DailyGoalCard(total: Nutrients, target: Nutrients, hasEntries: Boolean) {
    val primary=MaterialTheme.colorScheme.primary
    val track=MaterialTheme.colorScheme.surfaceVariant
    val over=total.kcal>target.kcal
    val tint=if(over) MaterialTheme.colorScheme.error else primary
    val ratio=if(target.kcal>0) total.kcal/target.kcal else 0.0
    ChartCard("Калории и дневная норма","За выбранный день · по сохранённым записям") {
        Text("${total.kcal.roundToInt()} / ${target.kcal.roundToInt()} ккал",style=MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(112.dp).semantics { contentDescription="Калории: ${(ratio*100).roundToInt()}% дневной нормы" },contentAlignment=Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(7.dp)) {
                    val stroke=Stroke(10.dp.toPx())
                    drawArc(track,0f,360f,false,style=stroke)
                    if(ratio>0) drawArc(tint,-90f,(ratio.coerceIn(0.0,1.0)*360).toFloat(),false,style=stroke)
                }
                Text("${(ratio*100).roundToInt()}%",style=MaterialTheme.typography.headlineSmall)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(if(over) "Сверх нормы" else "Осталось до нормы",style=MaterialTheme.typography.bodyMedium)
                Text("${kotlin.math.abs(target.kcal-total.kcal).roundToInt()} ккал",style=MaterialTheme.typography.titleLarge,color=tint)
                Text(if(hasEntries) "Учитываются записи дневника" else "Записей пока нет",style=MaterialTheme.typography.bodySmall)
            }
        }
        val values=listOf(total.protein,total.fat,total.carbs)
        val goals=listOf(target.protein,target.fat,target.carbs)
        values.forEachIndexed { index,value ->
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(macroNames[index],style=MaterialTheme.typography.bodyMedium)
                    Text("${value.roundToInt()} / ${goals[index].roundToInt()} г",style=MaterialTheme.typography.bodyMedium)
                }
                LinearProgressIndicator(progress={ (value/goals[index].coerceAtLeast(1.0)).toFloat().coerceIn(0f,1f) },
                    modifier=Modifier.fillMaxWidth(),color=macroColors[index])
                if(value>goals[index]) Text("Сверх нормы: ${(value-goals[index]).roundToInt()} г",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
internal fun MacroDonutCard(total: Nutrients) {
    val energies=AnalyticsData.macroEnergy(total)
    val percentages=AnalyticsData.macroPercentages(total)
    val sum=energies.sum()
    val track=MaterialTheme.colorScheme.surfaceVariant
    ChartCard("Распределение БЖУ","Доля энергии: белки и углеводы — 4 ккал/г, жиры — 9 ккал/г") {
        Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
            Box(Modifier.size(164.dp).semantics { contentDescription=if(sum>0) "Круговая диаграмма БЖУ: ${total.label()}" else "Нет данных для диаграммы БЖУ" },contentAlignment=Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(12.dp)) {
                    val stroke=Stroke(22.dp.toPx())
                    drawArc(track,0f,360f,false,style=stroke)
                    var start=-90f
                    energies.forEachIndexed { index,value ->
                        val angle=if(sum>0) (value/sum*360).toFloat() else 0f
                        if(angle>0) drawArc(macroColors[index],start,angle,false,style=stroke)
                        start+=angle
                    }
                }
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(if(sum>0) "${sum.roundToInt()}" else "—",style=MaterialTheme.typography.headlineSmall)
                    Text(if(sum>0) "ккал из БЖУ" else "Нет данных",style=MaterialTheme.typography.labelSmall)
                }
            }
        }
        macroNames.forEachIndexed { index,name ->
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                ColorDot(macroColors[index])
                Text(name,Modifier.weight(1f))
                val grams=listOf(total.protein,total.fat,total.carbs)[index]
                Text("${grams.roundToInt()} г · ${if(sum>0) "${percentages[index]}%" else "—"}")
            }
        }
        if(sum==0.0) Text("Добавьте еду с БЖУ, чтобы увидеть распределение.",style=MaterialTheme.typography.bodySmall)
        else Text("Энергия из БЖУ может отличаться от калорийности на этикетке из-за округления и состава продукта.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ColorDot(color: Color) { Box(Modifier.size(10.dp).background(color,CircleShape)) }

@Composable
internal fun CalorieBreakdownCard(title: String, groups: List<CalorieGroup>, totalKcal: Double, maxRows: Int = Int.MAX_VALUE) {
    val color=MaterialTheme.colorScheme.primary
    ChartCard(title,"За выбранный день · доля от всех записанных калорий") {
        if(groups.isEmpty()) Text("Нет записей для диаграммы",style=MaterialTheme.typography.bodyMedium)
        else {
            groups.take(maxRows).forEach { group ->
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(group.name,style=MaterialTheme.typography.bodyMedium)
                    val share=if(totalKcal>0) group.kcal/totalKcal else 0.0
                    Text("${group.kcal.roundToInt()} ккал · ${(share*100).roundToInt()}%",style=MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(progress={ share.toFloat().coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth(),color=color)
                }
            }
            if(groups.size>maxRows) Text("Показаны первые $maxRows из ${groups.size}. Проценты считаются от всего дня.",style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun WeekSummaryCard(days: List<DayStats>) {
    val summary=AnalyticsData.week(days)
    ChartCard("Итоги за 7 дней","Период заканчивается выбранной датой") {
        Text("${summary.total.kcal.roundToInt()} ккал за период",style=MaterialTheme.typography.titleLarge)
        Text("Дней с записями: ${summary.loggedDays} из ${days.size}")
        Text(if(summary.averageKcal==null) "Среднее появится после первой записи" else "В среднем: ${summary.averageKcal.roundToInt()} ккал за день с записями")
        Text("Всего: ${summary.total.label()}",style=MaterialTheme.typography.bodySmall)
        Text("День без записей не означает, что вы ничего не ели. Он исключён из среднего.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun WeeklyBarsCard(days: List<DayStats>, targetKcal: Double, macros: Boolean = false) {
    var selectedDate by remember(days.lastOrNull()?.date) { mutableStateOf(days.lastOrNull()?.date) }
    val selectedDay=days.firstOrNull { it.date==selectedDate }
    val primary=MaterialTheme.colorScheme.primary
    val error=MaterialTheme.colorScheme.error
    val outline=MaterialTheme.colorScheme.outlineVariant
    val values=days.map { if(macros) AnalyticsData.macroEnergy(it.totals).sum() else it.totals.kcal }
    val rawMax=maxOf(values.maxOrNull() ?: 0.0,if(macros) 0.0 else targetKcal,100.0)
    val axisMax=ceil(rawMax/500.0)*500.0
    ChartCard(if(macros) "БЖУ по дням" else "Калории по дням",
        if(macros) "Энергия из БЖУ · ккал. Нажмите на столбик." else "Ккал за день. Нажмите на столбик, чтобы увидеть значение.") {
        if(!macros) Text("Пунктир — дневная норма ${targetKcal.roundToInt()} ккал по текущему профилю",style=MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth().height(170.dp)) {
            Column(Modifier.width(36.dp).fillMaxHeight(),verticalArrangement=Arrangement.SpaceBetween) {
                listOf(axisMax,axisMax/2,0.0).forEach { Text(it.roundToInt().toString(),fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Canvas(Modifier.fillMaxSize()) {
                    listOf(0f,.5f,1f).forEach { fraction ->
                        val y=(size.height*fraction).coerceIn(1f,size.height-1f)
                        drawLine(outline,Offset(0f,y),Offset(size.width,y),1.dp.toPx())
                    }
                    if(!macros && targetKcal>0) {
                        val y=(size.height*(1-targetKcal/axisMax)).toFloat()
                        drawLine(primary,Offset(0f,y),Offset(size.width,y),2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(),5.dp.toPx())))
                    }
                }
                Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    days.forEachIndexed { index,day ->
                        val isSelected=day.date==selectedDate
                        val description="${if(macros) "БЖУ" else "Калории"} ${shortDate(day.date)}: ${if(day.entryCount==0) "нет записей" else if(macros) "${AnalyticsData.macroEnergy(day.totals).sum().roundToInt()} ккал из БЖУ" else "${day.totals.kcal.roundToInt()} ккал"}"
                        Canvas(Modifier.weight(1f).fillMaxHeight().clickable { selectedDate=day.date }.semantics {
                            contentDescription=description;selected=isSelected
                        }) {
                            val barWidth=size.width*.8f
                            val left=(size.width-barWidth)/2
                            if(day.entryCount==0) drawCircle(outline,3.dp.toPx(),Offset(size.width/2,size.height-5.dp.toPx()),style=Stroke(1.5.dp.toPx()))
                            else if(macros) {
                                var bottom=size.height
                                AnalyticsData.macroEnergy(day.totals).forEachIndexed { m,energy ->
                                    val height=(energy/axisMax*size.height).toFloat()
                                    if(height>0) drawRect(macroColors[m],Offset(left,bottom-height),Size(barWidth,height))
                                    bottom-=height
                                }
                            } else {
                                val height=(values[index]/axisMax*size.height).toFloat()
                                if(height>0) drawRect(if(day.totals.kcal>targetKcal) error else primary,Offset(left,size.height-height),Size(barWidth,height))
                                else drawLine(primary,Offset(left,size.height-1.dp.toPx()),Offset(left+barWidth,size.height-1.dp.toPx()),2.dp.toPx())
                            }
                            if(isSelected) drawLine(primary,Offset(left,size.height),Offset(left+barWidth,size.height),4.dp.toPx())
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(36.dp))
            days.forEach { day -> Text(shortDate(day.date),Modifier.weight(1f),fontSize=10.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center) }
        }
        if(macros) Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            macroNames.forEachIndexed { index,name -> Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                ColorDot(macroColors[index]);Text(name,style=MaterialTheme.typography.labelSmall)
            } }
        }
        selectedDay?.let { day ->
            Text("${shortDate(day.date)} · ${if(day.entryCount==0) "Нет записей" else if(macros) "${AnalyticsData.macroEnergy(day.totals).sum().roundToInt()} ккал из БЖУ" else "${day.totals.kcal.roundToInt()} ккал"}",style=MaterialTheme.typography.titleSmall)
            if(macros && day.entryCount>0) Text("Белки ${day.totals.protein.roundToInt()} г · Жиры ${day.totals.fat.roundToInt()} г · Углеводы ${day.totals.carbs.roundToInt()} г",style=MaterialTheme.typography.bodySmall)
            if(day.entryCount>0) Text("Записей: ${day.entryCount}",style=MaterialTheme.typography.bodySmall)
        }
        Text(if(days.none { it.entryCount>0 }) "За период ещё нет записей. Добавьте еду во вкладке «Агент»." else "Пустой кружок — день без записей.",style=MaterialTheme.typography.bodySmall)
    }
}

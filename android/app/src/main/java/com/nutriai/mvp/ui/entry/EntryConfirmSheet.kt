package com.nutriai.mvp.ui.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nutriai.mvp.ui.preview.NutriPreviewSurface

@Composable
fun EntryConfirmSheet(
    modifier: Modifier = Modifier,
    portionGrams: Int = 300,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp),
                thickness = 4.dp,
            )
            Text("Проверьте запись", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Борщ домашний",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text("Порция", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Remove, contentDescription = "Меньше")
                }
                Text("$portionGrams g", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Add, contentDescription = "Больше")
                }
            }
            Text(
                text = "180 kcal · Б6 · Ж8 · У12",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                Text("Сохранить в дневник")
            }
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Отмена")
            }
        }
    }
}

@Preview(name = "S6 Entry Confirm Sheet", showBackground = true, backgroundColor = 0x99000000, device = "spec:width=411dp,height=891dp")
@Composable
private fun EntryConfirmPreview() {
    NutriPreviewSurface {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                EntryConfirmSheet()
            }
        }
    }
}

@Preview(name = "S6 Entry Confirm — inline", showBackground = true, device = "spec:width=411dp,height=400dp")
@Composable
private fun EntryConfirmInlinePreview() {
    NutriPreviewSurface { EntryConfirmSheet() }
}

package com.nutriai.mvp.ui.preview

import androidx.compose.runtime.Composable
import com.nutriai.mvp.theme.NutriAiTheme

@Composable
fun NutriPreviewSurface(content: @Composable () -> Unit) {
    NutriAiTheme(content = content)
}

package com.nutriai.mvp.ui.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nutriai.mvp.theme.GreenPrimary
import com.nutriai.mvp.ui.components.NutriLogoBlock
import com.nutriai.mvp.ui.preview.NutriPreviewSurface

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NutriLogoBlock()
        Spacer(modifier = Modifier.height(48.dp))
        CircularProgressIndicator(color = GreenPrimary, strokeWidth = 3.dp)
        Text(
            text = "Загрузка…",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Preview(name = "S0 Splash", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun SplashScreenPreview() {
    NutriPreviewSurface { SplashScreen() }
}

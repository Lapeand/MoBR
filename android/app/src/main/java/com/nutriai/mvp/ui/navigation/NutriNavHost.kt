package com.nutriai.mvp.ui.navigation

import androidx.compose.runtime.Composable
import com.nutriai.mvp.runtime.NutriApp

/** Runtime uses one shared ViewModel; design-only screens remain available in the preview gallery. */
@Composable
fun NutriNavHost() { NutriApp() }

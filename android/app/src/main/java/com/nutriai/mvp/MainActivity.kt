package com.nutriai.mvp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nutriai.mvp.theme.NutriAiTheme
import com.nutriai.mvp.ui.navigation.NutriNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutriAiTheme {
                NutriNavHost()
            }
        }
    }
}

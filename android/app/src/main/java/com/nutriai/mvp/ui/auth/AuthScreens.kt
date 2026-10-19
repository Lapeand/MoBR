package com.nutriai.mvp.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nutriai.mvp.ui.components.NutriLogoBlock
import com.nutriai.mvp.ui.components.NutriTopBar
import com.nutriai.mvp.ui.preview.NutriPreviewSurface

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit = {},
    onRegisterClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NutriLogoBlock()
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) {
            Text("Войти")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onRegisterClick, modifier = Modifier.fillMaxWidth()) {
            Text("Создать аккаунт")
        }
    }
}

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { NutriTopBar(title = "Вход", onBack = onBack) },
    ) { padding ->
        AuthForm(
            modifier = Modifier.padding(padding),
            title = "С возвращением",
            subtitle = "Войдите, чтобы продолжить дневник",
            primaryLabel = "Войти",
            onPrimaryClick = onLoginSuccess,
        )
    }
}

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { NutriTopBar(title = "Регистрация", onBack = onBack) },
    ) { padding ->
        AuthForm(
            modifier = Modifier.padding(padding),
            title = "Новый аккаунт",
            subtitle = "Создайте профиль NutriAI",
            primaryLabel = "Зарегистрироваться",
            showName = true,
            onPrimaryClick = onRegisterSuccess,
        )
    }
}

@Composable
private fun AuthForm(
    title: String,
    subtitle: String,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    showName: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
        )
        if (showName) {
            OutlinedTextField(
                value = "Антон",
                onValueChange = {},
                label = { Text("Имя") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = "student@example.com",
            onValueChange = {},
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = "••••••••",
            onValueChange = {},
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onPrimaryClick, modifier = Modifier.fillMaxWidth()) {
            Text(primaryLabel)
        }
    }
}

@Preview(name = "S1 Welcome", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun WelcomePreview() {
    NutriPreviewSurface { WelcomeScreen() }
}

@Preview(name = "S2 Login", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun LoginPreview() {
    NutriPreviewSurface { LoginScreen(onBack = {}, onLoginSuccess = {}) }
}

@Preview(name = "S3 Register", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun RegisterPreview() {
    NutriPreviewSurface { RegisterScreen(onBack = {}, onRegisterSuccess = {}) }
}

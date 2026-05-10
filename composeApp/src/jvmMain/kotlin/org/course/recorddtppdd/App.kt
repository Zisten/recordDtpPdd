package org.course.recorddtppdd

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.course.recorddtppdd.db.DatabaseFactory
import org.course.recorddtppdd.model.Officer
import org.course.recorddtppdd.ui.screens.AuthScreen
import org.course.recorddtppdd.ui.screens.MainScreen
import org.course.recorddtppdd.ui.theme.AppTheme
import org.course.recorddtppdd.viewmodel.AuthViewModel

/**
 * Корневой composable приложения.
 * Управляет состоянием навигации: авторизация → главный экран → выход.
 */
@Composable
fun App() {
    var currentOfficer by remember { mutableStateOf<Officer?>(null) }
    val authVm = remember { AuthViewModel() }

    // Инициализируем БД один раз при старте
    var dbError by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        try {
            DatabaseFactory.init()
        } catch (e: Exception) {
            dbError = "Ошибка подключения к БД: ${e.message}"
        }
    }

    AppTheme {
        when {
            dbError.isNotBlank() -> DbErrorScreen(dbError)
            currentOfficer == null -> AuthScreen(
                vm = authVm,
                onLoginSuccess = { officer ->
                    currentOfficer = officer
                    authVm.reset()
                }
            )
            else -> MainScreen(
                officer = currentOfficer!!,
                onLogout = { currentOfficer = null }
            )
        }
    }
}

@Composable
private fun DbErrorScreen(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.widthIn(max = 500.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Ошибка подключения к базе данных",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    error,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "Проверьте параметры подключения в AppConfig.kt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

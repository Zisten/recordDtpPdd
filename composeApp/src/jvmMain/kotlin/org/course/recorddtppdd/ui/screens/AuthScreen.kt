package org.course.recorddtppdd.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.course.recorddtppdd.db.Repository
import org.course.recorddtppdd.model.Officer

@Composable
fun AuthScreen(onLoginSuccess: (Officer) -> Unit) {
    val state = remember { AuthState() }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.width(400.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Заголовок
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Система учёта ДТП и ПДД",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Вход в систему",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                // Поле логина
                OutlinedTextField(
                    value = state.login,
                    onValueChange = { state.login = it },
                    label = { Text("Логин") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Поле пароля
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { state.password = it },
                    label = { Text("Пароль") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Сообщение об ошибке
                if (state.error.isNotBlank()) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }

                // Кнопка входа
                Button(
                    onClick = {
                        val officer = state.authenticate()
                        if (officer != null) {
                            onLoginSuccess(officer)
                            state.reset()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Войти", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

private class AuthState {
    var login by mutableStateOf("")
    var password by mutableStateOf("")
    var error by mutableStateOf("")
    var isLoading by mutableStateOf(false)

    fun authenticate(): Officer? {
        if (login.isBlank() || password.isBlank()) {
            error = "Введите логин и пароль"
            return null
        }
        isLoading = true
        error = ""
        return try {
            val officer = Repository.findOfficerByCredentials(login, password)
            if (officer == null) {
                error = "Неверный логин или пароль"
            }
            officer
        } catch (e: Exception) {
            error = "Ошибка подключения к БД: ${e.message}"
            null
        } finally {
            isLoading = false
        }
    }

    fun reset() {
        login = ""
        password = ""
        error = ""
    }
}

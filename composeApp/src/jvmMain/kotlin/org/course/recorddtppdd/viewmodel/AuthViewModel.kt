package org.course.recorddtppdd.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.course.recorddtppdd.db.Repository
import org.course.recorddtppdd.model.Officer

class AuthViewModel {
    var login by mutableStateOf("")
    var password by mutableStateOf("")
    var error by mutableStateOf("")
    var isLoading by mutableStateOf(false)

    /**
     * Проверка учётных данных по таблице Officers.
     * Возвращает объект Officer при успехе, null — при ошибке.
     */
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

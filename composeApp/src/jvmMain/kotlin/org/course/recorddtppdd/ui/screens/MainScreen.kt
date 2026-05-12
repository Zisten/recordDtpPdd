package org.course.recorddtppdd.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.course.recorddtppdd.model.Officer

enum class MainSection {
    HOME, ACCIDENT_FORM, VIOLATION_FORM
}

@Composable
fun MainScreen(
    officer: Officer,
    onLogout: () -> Unit
) {
    var currentSection by remember { mutableStateOf(MainSection.HOME) }
    var accidentFormKey by remember { mutableStateOf(0) }
    var violationFormKey by remember { mutableStateOf(0) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Navigation Rail
        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Spacer(Modifier.height(16.dp))

            NavigationRailItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Главная") },
                label = { Text("Главная") },
                selected = currentSection == MainSection.HOME,
                onClick = { currentSection = MainSection.HOME }
            )

            NavigationRailItem(
                icon = { Icon(Icons.Default.Warning, contentDescription = "ДТП") },
                label = { Text("Оформление ДТП") },
                selected = currentSection == MainSection.ACCIDENT_FORM,
                onClick = {
                    accidentFormKey++
                    currentSection = MainSection.ACCIDENT_FORM
                }
            )

            NavigationRailItem(
                icon = { Icon(Icons.Default.Edit, contentDescription = "ПДД") },
                label = { Text("Оформление ПДД") },
                selected = currentSection == MainSection.VIOLATION_FORM,
                onClick = {
                    violationFormKey++
                    currentSection = MainSection.VIOLATION_FORM
                }
            )

            // Logout — прижат к низу
            Spacer(Modifier.weight(1f))
            NavigationRailItem(
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Выход") },
                label = { Text("Выход") },
                selected = false,
                onClick = onLogout
            )
            Spacer(Modifier.height(8.dp))
        }

        // Основной контент
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentSection) {
                MainSection.HOME ->
                    HomeScreen()
                MainSection.ACCIDENT_FORM ->
                    key(accidentFormKey) {
                        AccidentFormScreen(
                            officerId = officer.id,
                            onDone = {
                                currentSection = MainSection.HOME
                            }
                        )
                    }
                MainSection.VIOLATION_FORM ->
                    key(violationFormKey) {
                        ViolationFormScreen(
                            officerId = officer.id,
                            onDone = {
                                currentSection = MainSection.HOME
                            }
                        )
                    }
            }
        }
    }
}
package org.course.recorddtppdd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.course.recorddtppdd.model.AccidentRecord
import org.course.recorddtppdd.model.ViolationRecord
import org.course.recorddtppdd.viewmodel.HomeViewModel
import java.time.format.DateTimeFormatter

private val dtFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

@Composable
fun HomeScreen(vm: HomeViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("ДТП", "Нарушения ПДД", "SQL-отчёты")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // Заголовок + кнопка обновления
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Главная", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { vm.load() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (vm.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }

        if (vm.error.isNotBlank()) {
            Text(vm.error, color = MaterialTheme.colorScheme.error)
            return@Column
        }

        // Карточки статистики
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(
                title = "ДТП сегодня",
                value = vm.stats.accidentsToday.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Нарушений сегодня",
                value = vm.stats.violationsToday.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Вкладки
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { i, title ->
                Tab(selected = i == selectedTab, onClick = { selectedTab = i }, text = { Text(title) })
            }
        }

        Spacer(Modifier.height(8.dp))

        when (selectedTab) {
            0 -> AccidentsTable(vm)
            1 -> ViolationsTable(vm)
            2 -> SqlReportsTable(vm)
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(4.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 36.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text(title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccidentsTable(vm: HomeViewModel) {
    Column {
        // Поиск и сортировка
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = vm.accidentDateFilter,
                onValueChange = { vm.accidentDateFilter = it },
                label = { Text("Поиск по дате (гггг-мм-дд)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = { vm.accidentSortAsc = !vm.accidentSortAsc }) {
                Icon(Icons.Default.SwapVert, contentDescription = "Сортировка")
            }
            Text(if (vm.accidentSortAsc) "↑ по дате" else "↓ по дате", fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))

        val rows = vm.filteredAccidents()
        if (rows.isEmpty()) {
            Text("Нет записей", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        DataTable(
            headers = listOf("ID", "Улица", "Дом", "Дата/время", "Виновный"),
            rows = rows.map { a ->
                listOf(
                    a.id.toString(),
                    a.street,
                    a.house ?: "-",
                    a.dateTime.format(dtFmt),
                    a.guiltySide
                )
            }
        )
    }
}

@Composable
private fun ViolationsTable(vm: HomeViewModel) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = vm.violationDateFilter,
                onValueChange = { vm.violationDateFilter = it },
                label = { Text("Поиск по дате (гггг-мм-дд)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = { vm.violationSortAsc = !vm.violationSortAsc }) {
                Icon(Icons.Default.SwapVert, contentDescription = "Сортировка")
            }
            Text(if (vm.violationSortAsc) "↑ по дате" else "↓ по дате", fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))

        val rows = vm.filteredViolations()
        if (rows.isEmpty()) {
            Text("Нет записей", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        DataTable(
            headers = listOf("ID", "Водитель", "Пункт НПА", "Дата/время"),
            rows = rows.map { v ->
                listOf(
                    v.id.toString(),
                    v.driverFullName,
                    v.npaPoint,
                    v.dateTime.format(dtFmt)
                )
            }
        )
    }
}

@Composable
private fun SqlReportsTable(vm: HomeViewModel) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (vm.sqlReports.isEmpty()) {
            Text("Нет данных SQL-отчётов", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        vm.sqlReports.forEach { report ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(report.title, fontWeight = FontWeight.SemiBold)
                    Text(
                        report.sql,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    report.rows.forEach { row ->
                        Text(row, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/** Простая таблица с заголовками */
@Composable
fun DataTable(headers: List<String>, rows: List<List<String>>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Заголовок таблицы
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            headers.forEach { h ->
                Text(
                    h,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        HorizontalDivider()
        // Строки таблицы
        val scrollState = rememberScrollState()
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            rows.forEachIndexed { idx, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (idx % 2 == 0) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    row.forEach { cell ->
                        Text(cell, modifier = Modifier.weight(1f), fontSize = 13.sp)
                    }
                }
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}

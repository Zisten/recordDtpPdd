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
import org.course.recorddtppdd.db.Repository
import org.course.recorddtppdd.model.AccidentRecord
import org.course.recorddtppdd.model.AnalyticsReport
import org.course.recorddtppdd.model.HomeStats
import org.course.recorddtppdd.model.ViolationRecord
import java.time.format.DateTimeFormatter

private val dtFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

@Composable
fun HomeScreen() {
    val state = remember { HomeState() }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("ДТП", "Нарушения ПДД", "Аналитика (SQL)")

    LaunchedEffect(Unit) {
        state.load()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // Заголовок + кнопка обновления
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Главная", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { state.load() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            return@Column
        }

        if (state.error.isNotBlank()) {
            Text(state.error, color = MaterialTheme.colorScheme.error)
            return@Column
        }

        // Карточки статистики
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(
                title = "ДТП сегодня",
                value = state.stats.accidentsToday.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Нарушений сегодня",
                value = state.stats.violationsToday.toString(),
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
            0 -> AccidentsTable(state)
            1 -> ViolationsTable(state)
            2 -> AnalyticsTab(state)
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
private fun AccidentsTable(state: HomeState) {
    Column {
        // Поиск и сортировка
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.accidentDateFilter,
                onValueChange = { state.accidentDateFilter = it },
                label = { Text("Поиск по дате (гггг-мм-дд)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = { state.accidentSortAsc = !state.accidentSortAsc }) {
                Icon(Icons.Default.SwapVert, contentDescription = "Сортировка")
            }
            Text(if (state.accidentSortAsc) "↑ по дате" else "↓ по дате", fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))

        val rows = state.filteredAccidents()
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
private fun ViolationsTable(state: HomeState) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.violationDateFilter,
                onValueChange = { state.violationDateFilter = it },
                label = { Text("Поиск по дате (гггг-мм-дд)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = { state.violationSortAsc = !state.violationSortAsc }) {
                Icon(Icons.Default.SwapVert, contentDescription = "Сортировка")
            }
            Text(if (state.violationSortAsc) "↑ по дате" else "↓ по дате", fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))

        val rows = state.filteredViolations()
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
private fun AnalyticsTab(state: HomeState) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.analyticsReports.isEmpty()) {
            Text("Нет данных для аналитики.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            // Группируем отчеты по категориям
            state.analyticsReports.groupBy { it.category }.forEach { (category, reports) ->
                Text(
                    text = category,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                HorizontalDivider()

                reports.forEach { report ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(report.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.height(8.dp))
                            if (report.rows.isEmpty()) {
                                Text("Нет данных по этому запросу", fontSize = 13.sp)
                            } else {
                                DataTable(headers = report.headers, rows = report.rows, isNested = true)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun DataTable(headers: List<String>, rows: List<List<String>>, isNested: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
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

        val contentModifier = if (!isNested) {
            Modifier.verticalScroll(rememberScrollState())
        } else Modifier

        Column(modifier = contentModifier) {
            rows.forEachIndexed { idx, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (idx % 2 == 0) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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

private class HomeState {
    var stats by mutableStateOf(HomeStats(0, 0))
    var accidents by mutableStateOf<List<AccidentRecord>>(emptyList())
    var violations by mutableStateOf<List<ViolationRecord>>(emptyList())
    var analyticsReports by mutableStateOf<List<AnalyticsReport>>(emptyList())

    var isLoading by mutableStateOf(false)
    var error by mutableStateOf("")

    var accidentDateFilter by mutableStateOf("")
    var accidentSortAsc by mutableStateOf(false)

    var violationDateFilter by mutableStateOf("")
    var violationSortAsc by mutableStateOf(false)

    fun load() {
        isLoading = true
        error = ""
        try {
            stats = Repository.getHomeStats()
            accidents = Repository.getAllAccidents()
            violations = Repository.getAllViolations()
            analyticsReports = Repository.getAnalyticsReports()
        } catch (e: Exception) {
            error = "Ошибка загрузки данных: ${e.message}"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    fun filteredAccidents(): List<AccidentRecord> {
        var list = accidents
        if (accidentDateFilter.isNotBlank()) {
            list = list.filter { it.dateTime.toLocalDate().toString().contains(accidentDateFilter) }
        }
        return if (accidentSortAsc) list.sortedBy { it.dateTime } else list.sortedByDescending { it.dateTime }
    }

    fun filteredViolations(): List<ViolationRecord> {
        var list = violations
        if (violationDateFilter.isNotBlank()) {
            list = list.filter { it.dateTime.toLocalDate().toString().contains(violationDateFilter) }
        }
        return if (violationSortAsc) list.sortedBy { it.dateTime } else list.sortedByDescending { it.dateTime }
    }
}
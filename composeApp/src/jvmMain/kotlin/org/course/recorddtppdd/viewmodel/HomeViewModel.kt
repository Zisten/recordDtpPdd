package org.course.recorddtppdd.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.course.recorddtppdd.db.Repository
import org.course.recorddtppdd.model.AccidentRecord
import org.course.recorddtppdd.model.HomeStats
import org.course.recorddtppdd.model.ViolationRecord

class HomeViewModel {
    var stats by mutableStateOf(HomeStats(0, 0))
    var accidents by mutableStateOf<List<AccidentRecord>>(emptyList())
    var violations by mutableStateOf<List<ViolationRecord>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf("")

    // Фильтры для ДТП
    var accidentDateFilter by mutableStateOf("")
    var accidentSortAsc by mutableStateOf(false)

    // Фильтры для нарушений
    var violationDateFilter by mutableStateOf("")
    var violationSortAsc by mutableStateOf(false)

    fun load() {
        isLoading = true
        error = ""
        try {
            stats = Repository.getHomeStats()
            accidents = Repository.getAllAccidents()
            violations = Repository.getAllViolations()
        } catch (e: Exception) {
            error = "Ошибка загрузки данных: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    fun filteredAccidents(): List<AccidentRecord> {
        var list = accidents
        if (accidentDateFilter.isNotBlank()) {
            list = list.filter { it.datetime.toLocalDate().toString().contains(accidentDateFilter) }
        }
        return if (accidentSortAsc) list.sortedBy { it.datetime } else list.sortedByDescending { it.datetime }
    }

    fun filteredViolations(): List<ViolationRecord> {
        var list = violations
        if (violationDateFilter.isNotBlank()) {
            list = list.filter { it.datetime.toLocalDate().toString().contains(violationDateFilter) }
        }
        return if (violationSortAsc) list.sortedBy { it.datetime } else list.sortedByDescending { it.datetime }
    }
}

package org.course.recorddtppdd.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.course.recorddtppdd.viewmodel.ViolationFormViewModel

@Composable
fun ViolationFormScreen(
    vm: ViolationFormViewModel,
    officerId: Int,
    onDone: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Оформление нарушения ПДД",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        // Индикатор шагов
        ViolationStepIndicator(current = vm.currentStep, total = vm.totalSteps)
        Spacer(Modifier.height(16.dp))

        // Контент шага
        Box(modifier = Modifier.weight(1f)) {
            when (vm.currentStep) {
                0 -> VStep1PersonalData(vm)
                1 -> VStep2VehicleData(vm)
                2 -> VStep3Violation(vm)
            }
        }

        // Ошибка сохранения
        if (vm.saveError.isNotBlank()) {
            Text(vm.saveError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
        }

        // Навигация
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (vm.currentStep > 0) {
                OutlinedButton(onClick = { vm.prevStep() }) { Text("Назад") }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            if (vm.currentStep < vm.totalSteps - 1) {
                Button(onClick = { vm.nextStep() }) { Text("Далее") }
            } else {
                Button(
                    onClick = { vm.save(officerId, onDone) },
                    enabled = !vm.isSaving
                ) {
                    if (vm.isSaving) CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    else Text("Сохранить")
                }
            }
        }
    }
}

@Composable
private fun ViolationStepIndicator(current: Int, total: Int) {
    val names = listOf("Нарушитель", "Транспорт", "Нарушение")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        names.forEachIndexed { i, name ->
            FilterChip(
                selected = i <= current,
                onClick = {},
                label = { Text("${i + 1}. $name", fontSize = 12.sp) },
                enabled = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Шаг 1: Персональные данные нарушителя + ВУ ──────────────────────────────

@Composable
private fun VStep1PersonalData(vm: ViolationFormViewModel) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VSectionTitle("Персональные данные нарушителя")
        VFormField("ФИО", vm.driverFullName) { vm.driverFullName = it }
        VFormField("Дата рождения (гггг-мм-дд)", vm.driverBirthdate) { vm.driverBirthdate = it }
        VFormField("Адрес проживания", vm.driverAddress) { vm.driverAddress = it }
        VFormField("Телефон", vm.driverPhone) { vm.driverPhone = it }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        VSectionTitle("Водительское удостоверение")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = vm.licSeries, onValueChange = { vm.licSeries = it },
                label = { Text("Серия") }, modifier = Modifier.weight(1f), singleLine = true
            )
            OutlinedTextField(
                value = vm.licNumber, onValueChange = { vm.licNumber = it },
                label = { Text("Номер") }, modifier = Modifier.weight(1f), singleLine = true
            )
            OutlinedTextField(
                value = vm.licCategory, onValueChange = { vm.licCategory = it },
                label = { Text("Категория") }, modifier = Modifier.weight(1f), singleLine = true
            )
        }
        VFormField("Дата выдачи ВУ", vm.licIssueDate) { vm.licIssueDate = it }
    }
}

// ── Шаг 2: Данные ТС с автоподстановкой ────────────────────────────────────

@Composable
private fun VStep2VehicleData(vm: ViolationFormViewModel) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VSectionTitle("Транспортное средство")
        Text(
            "Введите госномер для автоматической подстановки данных из БД:",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = vm.vehiclePlate,
                onValueChange = { vm.vehiclePlate = it },
                label = { Text("Госномер") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = { vm.lookupVehicle() }) {
                Icon(Icons.Default.Search, contentDescription = "Найти ТС")
            }
        }
        VFormField("Марка", vm.vehicleBrand) { vm.vehicleBrand = it } // Исправлено
        VFormField("Модель", vm.vehicleModel) { vm.vehicleModel = it }
        VFormField("Владелец (ФИО)", vm.vehicleOwner) { vm.vehicleOwner = it }
        VFormField("Адрес владельца", vm.vehicleOwnerAddress) { vm.vehicleOwnerAddress = it }
        VFormField("VIN", vm.vehicleVin) { vm.vehicleVin = it }
        VFormField("СТС", vm.vehicleSts) { vm.vehicleSts = it }
    }
}

// ── Шаг 3: Нарушение ─────────────────────────────────────────────────────[...]

@Composable
private fun VStep3Violation(vm: ViolationFormViewModel) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VSectionTitle("Данные нарушения")
        VFormField("Вид нарушения (наименование)", vm.violationTypeName) { vm.violationTypeName = it }
        VFormField("Пункт НПА", vm.npaPoint) { vm.npaPoint = it }
        OutlinedTextField(
            value = vm.description, onValueChange = { vm.description = it },
            label = { Text("Суть нарушения") },
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )
        VFormField("Дата нарушения (гггг-мм-дд)", vm.violationDate) { vm.violationDate = it }
        VFormField("Время нарушения (чч:мм)", vm.violationTime) { vm.violationTime = it }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        VSectionTitle("Свидетель / Потерпевший")
        VFormField("ФИО", vm.witnessName) { vm.witnessName = it }
        VFormField("Адрес", vm.witnessAddress) { vm.witnessAddress = it }
        VFormField("Телефон", vm.witnessPhone) { vm.witnessPhone = it }
    }
}

// ── Вспомогательные ─────────────────────────────────────────────────────[...]

@Composable
private fun VSectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
        color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun VFormField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
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
import org.course.recorddtppdd.db.Repository
import java.time.LocalDate
import java.time.LocalDateTime

private class ViolationFormState {
    var currentStep by mutableStateOf(0)
    val totalSteps = 3

    var driverFullName by mutableStateOf("")
    var driverBirthdate by mutableStateOf("")
    var driverAddress by mutableStateOf("")
    var driverPhone by mutableStateOf("")
    var licSeries by mutableStateOf("")
    var licNumber by mutableStateOf("")
    var licCategory by mutableStateOf("")
    var licIssueDate by mutableStateOf("")

    var vehiclePlate by mutableStateOf("")
    var vehicleBrand by mutableStateOf("")
    var vehicleModel by mutableStateOf("")
    var vehicleOwner by mutableStateOf("")
    var vehicleSts by mutableStateOf("")
    var vehicleVin by mutableStateOf("")
    var vehicleOwnerAddress by mutableStateOf("")

    var violationTypeName by mutableStateOf("")
    var npaPoint by mutableStateOf("")
    var description by mutableStateOf("")
    var witnessName by mutableStateOf("")
    var witnessAddress by mutableStateOf("")
    var witnessPhone by mutableStateOf("")
    var violationDate by mutableStateOf(java.time.LocalDate.now().toString())
    var violationTime by mutableStateOf("12:00")

    var isSaving by mutableStateOf(false)
    var saveError by mutableStateOf("")
    var saveSuccess by mutableStateOf(false)

    fun nextStep() { if (currentStep < totalSteps - 1) currentStep++ }
    fun prevStep() { if (currentStep > 0) currentStep-- }

    fun lookupVehicle() {
        if (vehiclePlate.isBlank()) return
        try {
            val v = Repository.findVehicleByPlate(vehiclePlate)
            if (v != null) {
                vehicleBrand = v.brand
                vehicleModel = v.model
                vehicleVin = v.vin ?: ""
                vehicleSts = v.regCertificate ?: ""
            }
        } catch (_: Exception) {
        }
    }

    fun save(officerId: Int, onSuccess: () -> Unit) {
        isSaving = true
        saveError = ""
        try {
            val dt = LocalDateTime.parse(
                "${violationDate}T${violationTime.padStart(5, '0')}:00"
            )

            val driverId = Repository.getOrCreateDriver(
                fullName = driverFullName,
                birthDate = parseDateOrNull(driverBirthdate),
                registrationAddress = driverAddress.ifBlank { null },
                actualAddress = driverAddress.ifBlank { null },
                phone = driverPhone.ifBlank { null }
            )
            if (licSeries.isNotBlank() && licNumber.isNotBlank()) {
                val issueDate = parseDateOrNull(licIssueDate) ?: LocalDate.now()
                Repository.insertLicense(driverId, licSeries, licNumber, licCategory.ifBlank { null }, issueDate)
            }

            val ownerDriverId = if (vehicleOwner.isNotBlank()) {
                Repository.getOrCreateDriver(vehicleOwner, null, vehicleOwnerAddress.ifBlank { null }, null, null)
            } else null

            val vehicleId = Repository.getOrCreateVehicle(
                ownerId = ownerDriverId,
                brand = vehicleBrand,
                model = vehicleModel,
                numberPlate = vehiclePlate,
                vin = vehicleVin.ifBlank { null },
                regCertificate = vehicleSts.ifBlank { null },
                insuranceName = null,
                insurancePolicy = null,
                insuranceExpiry = null,
                hasHullInsurance = null
            )

            val violationTypeId = Repository.getOrCreateViolationType(
                clause = npaPoint,
                description = description,
                fineAmount = 0
            )

            val witnessInfo = listOfNotNull(witnessName, witnessAddress, witnessPhone)
                .filter { it.isNotBlank() }
                .joinToString(", ")
                .ifBlank { null }

            Repository.insertViolation(
                officerId = officerId,
                driverId = driverId,
                vehicleId = vehicleId,
                typeId = violationTypeId,
                dateTime = dt,
                street = "",
                houseNumber = null,
                witnessVictimInfo = witnessInfo
            )

            saveSuccess = true
            onSuccess()
        } catch (e: Exception) {
            saveError = "Ошибка сохранения: ${e.message}"
            e.printStackTrace()
        } finally {
            isSaving = false
        }
    }

    fun reset() {
        currentStep = 0
        driverFullName = ""
        driverBirthdate = ""
        driverAddress = ""
        driverPhone = ""
        licSeries = ""
        licNumber = ""
        licCategory = ""
        licIssueDate = ""
        vehiclePlate = ""
        vehicleBrand = ""
        vehicleModel = ""
        vehicleOwner = ""
        vehicleSts = ""
        vehicleVin = ""
        vehicleOwnerAddress = ""
        violationTypeName = ""
        npaPoint = ""
        description = ""
        witnessName = ""
        witnessAddress = ""
        witnessPhone = ""
        violationDate = java.time.LocalDate.now().toString()
        violationTime = "12:00"
        saveError = ""
        saveSuccess = false
    }
}

@Composable
fun ViolationFormScreen(
    officerId: Int,
    onDone: () -> Unit
) {
    val state = remember { ViolationFormState() }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Оформление нарушения ПДД",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        // Индикатор шагов
        ViolationStepIndicator(current = state.currentStep, total = state.totalSteps)
        Spacer(Modifier.height(16.dp))

        // Контент шага
        Box(modifier = Modifier.weight(1f)) {
            when (state.currentStep) {
                0 -> VStep1PersonalData(state)
                1 -> VStep2VehicleData(state)
                2 -> VStep3Violation(state)
            }
        }

        // Ошибка сохранения
        if (state.saveError.isNotBlank()) {
            Text(state.saveError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
        }

        // Навигация
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (state.currentStep > 0) {
                OutlinedButton(onClick = { state.prevStep() }) { Text("Назад") }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            if (state.currentStep < state.totalSteps - 1) {
                Button(onClick = { state.nextStep() }) { Text("Далее") }
            } else {
                Button(
                    onClick = { state.save(officerId, onDone) },
                    enabled = !state.isSaving
                ) {
                    if (state.isSaving) CircularProgressIndicator(
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
private fun VStep1PersonalData(state: ViolationFormState) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VSectionTitle("Персональные данные нарушителя")
        VFormField("ФИО", state.driverFullName) { state.driverFullName = it }
        VFormField("Дата рождения (гггг-мм-дд)", state.driverBirthdate) { state.driverBirthdate = it }
        VFormField("Адрес проживания", state.driverAddress) { state.driverAddress = it }
        VFormField("Телефон", state.driverPhone) { state.driverPhone = it }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        VSectionTitle("Водительское удостоверение")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.licSeries, onValueChange = { state.licSeries = it },
                label = { Text("Серия") }, modifier = Modifier.weight(1f), singleLine = true
            )
            OutlinedTextField(
                value = state.licNumber, onValueChange = { state.licNumber = it },
                label = { Text("Номер") }, modifier = Modifier.weight(1f), singleLine = true
            )
            OutlinedTextField(
                value = state.licCategory, onValueChange = { state.licCategory = it },
                label = { Text("Категория") }, modifier = Modifier.weight(1f), singleLine = true
            )
        }
        VFormField("Дата выдачи ВУ", state.licIssueDate) { state.licIssueDate = it }
    }
}

// ── Шаг 2: Данные ТС с автоподстановкой ────────────────────────────────────

@Composable
private fun VStep2VehicleData(state: ViolationFormState) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VSectionTitle("Транспортное средство")
        Text(
            "Введите госномер для автоматической подстановки данных из БД:",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.vehiclePlate,
                onValueChange = { state.vehiclePlate = it },
                label = { Text("Госномер") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = { state.lookupVehicle() }) {
                Icon(Icons.Default.Search, contentDescription = "Найти ТС")
            }
        }
        VFormField("Марка", state.vehicleBrand) { state.vehicleBrand = it }
        VFormField("Модель", state.vehicleModel) { state.vehicleModel = it }
        VFormField("Владелец (ФИО)", state.vehicleOwner) { state.vehicleOwner = it }
        VFormField("Адрес владельца", state.vehicleOwnerAddress) { state.vehicleOwnerAddress = it }
        VFormField("VIN", state.vehicleVin) { state.vehicleVin = it }
        VFormField("СТС", state.vehicleSts) { state.vehicleSts = it }
    }
}

// ── Шаг 3: Нарушение ─────────────────────────────────────────────────────[...]

@Composable
private fun VStep3Violation(state: ViolationFormState) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VSectionTitle("Данные нарушения")
        VFormField("Вид нарушения (наименование)", state.violationTypeName) { state.violationTypeName = it }
        VFormField("Пункт НПА", state.npaPoint) { state.npaPoint = it }
        OutlinedTextField(
            value = state.description, onValueChange = { state.description = it },
            label = { Text("Суть нарушения") },
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )
        VFormField("Дата нарушения (гггг-мм-дд)", state.violationDate) { state.violationDate = it }
        VFormField("Время нарушения (чч:мм)", state.violationTime) { state.violationTime = it }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        VSectionTitle("Свидетель / Потерпевший")
        VFormField("ФИО", state.witnessName) { state.witnessName = it }
        VFormField("Адрес", state.witnessAddress) { state.witnessAddress = it }
        VFormField("Телефон", state.witnessPhone) { state.witnessPhone = it }
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

private fun parseDateOrNull(value: String): LocalDate? =
    value.trim().takeIf { it.isNotBlank() }?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
    }
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
import org.course.recorddtppdd.model.ViolationType
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
    var vehicleVin by mutableStateOf("")
    var vehicleOwnerAddress by mutableStateOf("")

    var violationTypeName by mutableStateOf("")
    var npaPoint by mutableStateOf("")
    var street by mutableStateOf("")
    var houseNumber by mutableStateOf("")
    var witnessName by mutableStateOf("")
    var witnessAddress by mutableStateOf("")
    var witnessPhone by mutableStateOf("")
    var noWitnesses by mutableStateOf(false)
    var violationDate by mutableStateOf(java.time.LocalDate.now().toString())
    var violationTime by mutableStateOf("12:00")

    var isSaving by mutableStateOf(false)
    var saveError by mutableStateOf("")
    var saveSuccess by mutableStateOf(false)

    val availableViolationTypes = mutableStateListOf<ViolationType>()

    init {
        try {
            availableViolationTypes.addAll(Repository.getAllViolationTypes())
        } catch (_: Exception) {}
    }

    fun nextStep() { if (currentStep < totalSteps - 1) currentStep++ }
    fun prevStep() { if (currentStep > 0) currentStep-- }

    fun lookupDriverByLicense() {
        if (licSeries.isBlank() || licNumber.isBlank()) return
        try {
            val lic = Repository.findLicenseBySeriesAndNumber(licSeries, licNumber)
            if (lic != null) {
                val d = Repository.findDriverById(lic.driverId)
                if (d != null) {
                    driverFullName = d.fullName
                    driverBirthdate = d.birthDate?.toString() ?: ""
                    driverAddress = d.actualAddress ?: d.registrationAddress ?: ""
                    driverPhone = d.phone ?: ""
                    licCategory = lic.category ?: ""
                    licIssueDate = lic.issueDate.toString()
                }
            }
        } catch (_: Exception) {}
    }

    fun lookupOwnerByPlate() {
        if (vehiclePlate.isBlank()) return
        try {
            val v = Repository.findVehicleByPlate(vehiclePlate)
            if (v?.ownerId != null) {
                val owner = Repository.findDriverById(v.ownerId)
                if (owner != null) {
                    vehicleOwner = owner.fullName
                    vehicleOwnerAddress = owner.actualAddress ?: owner.registrationAddress ?: ""
                }
            }
        } catch (_: Exception) {}
    }

    fun lookupVehicleByVin() {
        if (vehicleVin.isBlank()) return
        try {
            val v = Repository.findVehicleByVin(vehicleVin)
            if (v != null) {
                vehicleBrand = v.brand
                vehicleModel = v.model
            }
        } catch (_: Exception) {}
    }

    fun save(officerId: Int, onSuccess: () -> Unit) {
        isSaving = true
        saveError = ""
        try {
            val dt = LocalDateTime.parse(
                "${violationDate}T${violationTime.padStart(5, '0')}:00"
            )

            // Водитель (если найден по ВУ - берем его, иначе создаем)
            val dLic = if (licSeries.isNotBlank() && licNumber.isNotBlank()) {
                Repository.findLicenseBySeriesAndNumber(licSeries, licNumber)
            } else null

            val driverId = dLic?.driverId ?: Repository.getOrCreateDriver(
                fullName = driverFullName,
                birthDate = parseDateOrNull(driverBirthdate),
                registrationAddress = driverAddress.ifBlank { null },
                actualAddress = driverAddress.ifBlank { null },
                phone = driverPhone.ifBlank { null }
            )

            if (dLic == null && licSeries.isNotBlank() && licNumber.isNotBlank()) {
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
                regCertificate = null, // СТС убрали
                insuranceName = null,
                insurancePolicy = null,
                insuranceExpiry = null,
                hasHullInsurance = null
            )

            val violationTypeId = Repository.getOrCreateViolationType(
                clause = npaPoint,
                description = violationTypeName,
                fineAmount = 0
            )

            val witnessInfo = if (noWitnesses) null else listOfNotNull(witnessName, witnessAddress, witnessPhone)
                .filter { it.isNotBlank() }
                .joinToString(", ")
                .ifBlank { null }

            Repository.insertViolation(
                officerId = officerId,
                driverId = driverId,
                vehicleId = vehicleId,
                typeId = violationTypeId,
                dateTime = dt,
                street = street,
                houseNumber = houseNumber.ifBlank { null },
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
        vehicleVin = ""
        vehicleOwnerAddress = ""
        violationTypeName = ""
        npaPoint = ""
        street = ""
        houseNumber = ""
        witnessName = ""
        witnessAddress = ""
        witnessPhone = ""
        noWitnesses = false
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

@Composable
private fun VStep1PersonalData(state: ViolationFormState) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VSectionTitle("Водительское удостоверение")
        Text(
            "Введите серию и номер ВУ и нажмите лупу для автозаполнения.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.licSeries, onValueChange = { state.licSeries = it },
                label = { Text("Серия") }, modifier = Modifier.weight(1f), singleLine = true
            )
            OutlinedTextField(
                value = state.licNumber, onValueChange = { state.licNumber = it },
                label = { Text("Номер") }, modifier = Modifier.weight(1f), singleLine = true
            )
            IconButton(onClick = { state.lookupDriverByLicense() }) {
                Icon(Icons.Default.Search, contentDescription = "Найти ВУ")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.licCategory, onValueChange = { state.licCategory = it },
                label = { Text("Категория") }, modifier = Modifier.weight(1f), singleLine = true
            )
            OutlinedTextField(
                value = state.licIssueDate, onValueChange = { state.licIssueDate = it },
                label = { Text("Дата выдачи (гггг-мм-дд)") }, modifier = Modifier.weight(1f), singleLine = true
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        VSectionTitle("Персональные данные нарушителя")
        VFormField("ФИО", state.driverFullName) { state.driverFullName = it }
        VFormField("Дата рождения (гггг-мм-дд)", state.driverBirthdate) { state.driverBirthdate = it }
        VFormField("Адрес проживания", state.driverAddress) { state.driverAddress = it }
        VFormField("Телефон", state.driverPhone) { state.driverPhone = it }
    }
}

@Composable
private fun VStep2VehicleData(state: ViolationFormState) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VSectionTitle("Транспортное средство")
        Text(
            "Госномер → Заполняет владельца. VIN → Заполняет марку и модель.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.vehiclePlate,
                onValueChange = { state.vehiclePlate = it },
                label = { Text("Госномер (Поиск владельца)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = { state.lookupOwnerByPlate() }) {
                Icon(Icons.Default.Search, contentDescription = "Найти владельца")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.vehicleVin,
                onValueChange = { state.vehicleVin = it },
                label = { Text("VIN (Поиск авто)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = { state.lookupVehicleByVin() }) {
                Icon(Icons.Default.Search, contentDescription = "Найти по VIN")
            }
        }
        VFormField("Марка", state.vehicleBrand) { state.vehicleBrand = it }
        VFormField("Модель", state.vehicleModel) { state.vehicleModel = it }
        VFormField("Владелец (ФИО)", state.vehicleOwner) { state.vehicleOwner = it }
        VFormField("Адрес владельца", state.vehicleOwnerAddress) { state.vehicleOwnerAddress = it }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VStep3Violation(state: ViolationFormState) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VSectionTitle("Данные нарушения")

        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = state.npaPoint,
                onValueChange = {},
                readOnly = true,
                label = { Text("Пункт НПА") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded && state.availableViolationTypes.isNotEmpty(),
                onDismissRequest = { expanded = false }
            ) {
                state.availableViolationTypes.forEach { vt ->
                    DropdownMenuItem(
                        text = { Text("${vt.clause} - ${vt.description}") },
                        onClick = {
                            state.npaPoint = vt.clause
                            state.violationTypeName = vt.description
                            expanded = false
                        }
                    )
                }
            }
        }

        VFormField("Вид нарушения (наименование)", state.violationTypeName) { state.violationTypeName = it }

        VFormField("Улица", state.street) { state.street = it }
        VFormField("Номер дома", state.houseNumber) { state.houseNumber = it }

        VFormField("Дата нарушения (гггг-мм-дд)", state.violationDate) { state.violationDate = it }
        VFormField("Время нарушения (чч:мм)", state.violationTime) { state.violationTime = it }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        VSectionTitle("Свидетель / Потерпевший")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.noWitnesses, onCheckedChange = { state.noWitnesses = it })
            Text("Свидетели отсутствуют")
        }
        if (!state.noWitnesses) {
            VFormField("ФИО", state.witnessName) { state.witnessName = it }
            VFormField("Адрес", state.witnessAddress) { state.witnessAddress = it }
            VFormField("Телефон", state.witnessPhone) { state.witnessPhone = it }
        }
    }
}

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
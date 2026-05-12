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

private data class VehicleFormData(
    var make: String = "",
    var model: String = "",
    var vin: String = "",
    var numberPlate: String = "",
    var sts: String = "",
    var ownerName: String = "",
    var ownerAddress: String = ""
)

private data class DriverFormData(
    var fullName: String = "",
    var birthdate: String = "",
    var address: String = "",
    var phone: String = "",
    var licSeries: String = "",
    var licNumber: String = "",
    var licCategory: String = "",
    var licIssueDate: String = "",
    var insCompany: String = "",
    var insPolicy: String = "",
    var insExpiry: String = "",
    var hasHull: Boolean = false
)

// Пункты строго по таблице AccidentTypes
private val ACCIDENT_CIRCUMSTANCES = listOf(
    "1. ТС находился на стоянке, парковке, обочине и т.п в неподвижном состоянии",
    "2. Заезжал на стоянку, парковку, во двор, на второстепенную дорогу",
    "3. Двигался прямо (не маневрировал)",
    "4. Двигался на перекрёстке",
    "5. Заезжал на перекресток с круговым движением",
    "6. Столкнулся с ТС, двигавшимся в том же направлении по той же полосе",
    "7. Столкнулся с ТС, двигавшимся в том же направлении по другой полосе (в другом ряду)",
    "8. Менял полосу (перестраивался в другой ряд)",
    "9. Обгонял",
    "10. Поворачивал направо",
    "11. Поворачивал налево",
    "12. Совершал разворот",
    "13. Двигался задним ходом",
    "14. Выехал на сторону дороги, предназначенную для встречного движения",
    "15. Второе ТС находилось слева от меня",
    "16. Не выполнил требование знака приоритета",
    "17. Совершил наезд (на неподвижное ТС, препятствие, пешехода и т.п.)",
    "18. Остановился (стоял) на запрещающий сигнал светофора",
    "19. Иное"
)

private class AccidentFormState {
    var currentStep by mutableStateOf(0)
    val totalSteps = 6

    var locationStreet by mutableStateOf("")
    var locationBuilding by mutableStateOf("")
    var accidentDate by mutableStateOf(java.time.LocalDate.now().toString())
    var accidentTime by mutableStateOf("12:00")
    var witnessesName by mutableStateOf("")
    var witnessesAddress by mutableStateOf("")
    var noWitnesses by mutableStateOf(false)

    var vehicleA by mutableStateOf(VehicleFormData())
    var vehicleB by mutableStateOf(VehicleFormData())
    var driverA by mutableStateOf(DriverFormData())
    var driverB by mutableStateOf(DriverFormData())

    var damagesA by mutableStateOf("")
    var notesA by mutableStateOf("")
    var damagesB by mutableStateOf("")
    var notesB by mutableStateOf("")

    // Используем множественный выбор для обстоятельств
    val selectedCircumstances = mutableStateListOf<String>()

    var accidentDescription by mutableStateOf("")
    var guiltySide by mutableStateOf("не определён")

    var isSaving by mutableStateOf(false)
    var saveError by mutableStateOf("")
    var saveSuccess by mutableStateOf(false)

    fun nextStep() { if (currentStep < totalSteps - 1) currentStep++ }
    fun prevStep() { if (currentStep > 0) currentStep-- }

    fun toggleCircumstance(item: String) {
        if (selectedCircumstances.contains(item)) selectedCircumstances.remove(item)
        else selectedCircumstances.add(item)
    }

    // --- Автозаполнение А ---
    fun lookupOwnerAByPlate() {
        if (vehicleA.numberPlate.isBlank()) return
        try {
            val v = Repository.findVehicleByPlate(vehicleA.numberPlate)
            if (v?.ownerId != null) {
                val owner = Repository.findDriverById(v.ownerId)
                if (owner != null) {
                    vehicleA = vehicleA.copy(
                        ownerName = owner.fullName,
                        ownerAddress = owner.actualAddress ?: owner.registrationAddress ?: ""
                    )
                }
            }
        } catch (_: Exception) {}
    }

    fun lookupVehicleAByVin() {
        if (vehicleA.vin.isBlank()) return
        try {
            val v = Repository.findVehicleByVin(vehicleA.vin)
            if (v != null) {
                vehicleA = vehicleA.copy(
                    make = v.brand,
                    model = v.model,
                    sts = v.regCertificate ?: ""
                )
                driverA = driverA.copy(
                    insCompany = v.insuranceName ?: "",
                    insPolicy = v.insurancePolicy ?: "",
                    insExpiry = v.insuranceExpiry?.toString() ?: "",
                    hasHull = v.hasHullInsurance ?: false
                )
            }
        } catch (_: Exception) {}
    }

    // --- Автозаполнение B ---
    fun lookupOwnerBByPlate() {
        if (vehicleB.numberPlate.isBlank()) return
        try {
            val v = Repository.findVehicleByPlate(vehicleB.numberPlate)
            if (v?.ownerId != null) {
                val owner = Repository.findDriverById(v.ownerId)
                if (owner != null) {
                    vehicleB = vehicleB.copy(
                        ownerName = owner.fullName,
                        ownerAddress = owner.actualAddress ?: owner.registrationAddress ?: ""
                    )
                }
            }
        } catch (_: Exception) {}
    }

    fun lookupVehicleBByVin() {
        if (vehicleB.vin.isBlank()) return
        try {
            val v = Repository.findVehicleByVin(vehicleB.vin)
            if (v != null) {
                vehicleB = vehicleB.copy(
                    make = v.brand,
                    model = v.model,
                    sts = v.regCertificate ?: ""
                )
                driverB = driverB.copy(
                    insCompany = v.insuranceName ?: "",
                    insPolicy = v.insurancePolicy ?: "",
                    insExpiry = v.insuranceExpiry?.toString() ?: "",
                    hasHull = v.hasHullInsurance ?: false
                )
            }
        } catch (_: Exception) {}
    }

    // --- Автозаполнение ВУ ---
    fun lookupDriverAByLicense() {
        if (driverA.licSeries.isBlank() || driverA.licNumber.isBlank()) return
        try {
            val lic = Repository.findLicenseBySeriesAndNumber(driverA.licSeries, driverA.licNumber)
            if (lic != null) {
                val d = Repository.findDriverById(lic.driverId)
                if (d != null) {
                    driverA = driverA.copy(
                        fullName = d.fullName,
                        birthdate = d.birthDate?.toString() ?: "",
                        address = d.actualAddress ?: d.registrationAddress ?: "",
                        phone = d.phone ?: "",
                        licCategory = lic.category ?: "",
                        licIssueDate = lic.issueDate.toString()
                    )
                }
            }
        } catch (_: Exception) {}
    }

    fun lookupDriverBByLicense() {
        if (driverB.licSeries.isBlank() || driverB.licNumber.isBlank()) return
        try {
            val lic = Repository.findLicenseBySeriesAndNumber(driverB.licSeries, driverB.licNumber)
            if (lic != null) {
                val d = Repository.findDriverById(lic.driverId)
                if (d != null) {
                    driverB = driverB.copy(
                        fullName = d.fullName,
                        birthdate = d.birthDate?.toString() ?: "",
                        address = d.actualAddress ?: d.registrationAddress ?: "",
                        phone = d.phone ?: "",
                        licCategory = lic.category ?: "",
                        licIssueDate = lic.issueDate.toString()
                    )
                }
            }
        } catch (_: Exception) {}
    }

    fun save(officerId: Int, onSuccess: () -> Unit) {
        isSaving = true
        saveError = ""
        try {
            val dt = java.time.LocalDateTime.parse(
                "${accidentDate}T${accidentTime.padStart(5, '0')}:00"
            )

            // Преобразуем выбранные строки в JSON массив
            val circumstances = buildCircumstancesJson(selectedCircumstances.toList())

            val witnessesInfo = listOf(witnessesName, witnessesAddress)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .joinToString(", ")
                .ifBlank { null }
            val explanation = buildString {
                if (accidentDescription.isNotBlank()) append(accidentDescription.trim())
                if (guiltySide.isNotBlank()) {
                    if (isNotEmpty()) append("\n")
                    append("Виновный: $guiltySide")
                }
            }.ifBlank { null }

            // typeId оставляем null, так как обстоятельства хранятся в JSON
            val accidentId = Repository.insertAccident(
                officerId = officerId,
                typeId = null,
                dateTime = dt,
                street = locationStreet,
                house = locationBuilding.ifBlank { null },
                witnessesInfo = if (noWitnesses) null else witnessesInfo,
                circumstancesJson = circumstances,
                explanation = explanation
            )

            // Сначала ищем водителя A по номеру прав, если нет — создаем
            val dALic = if (driverA.licSeries.isNotBlank() && driverA.licNumber.isNotBlank()) {
                Repository.findLicenseBySeriesAndNumber(driverA.licSeries, driverA.licNumber)
            } else null

            val driverAId = dALic?.driverId ?: Repository.getOrCreateDriver(
                fullName = driverA.fullName,
                birthDate = parseDateOrNull(driverA.birthdate),
                registrationAddress = driverA.address.ifBlank { null },
                actualAddress = driverA.address.ifBlank { null },
                phone = driverA.phone.ifBlank { null }
            )

            // Владелец A для ТС
            val ownerAId = if (vehicleA.ownerName.isNotBlank()) {
                Repository.getOrCreateDriver(vehicleA.ownerName, null, vehicleA.ownerAddress.ifBlank { null }, null, null)
            } else null

            val vehicleAId = Repository.getOrCreateVehicle(
                ownerId = ownerAId,
                brand = vehicleA.make,
                model = vehicleA.model,
                numberPlate = vehicleA.numberPlate,
                vin = vehicleA.vin.ifBlank { null },
                regCertificate = vehicleA.sts.ifBlank { null },
                insuranceName = driverA.insCompany.ifBlank { null },
                insurancePolicy = driverA.insPolicy.ifBlank { null },
                insuranceExpiry = parseDateOrNull(driverA.insExpiry),
                hasHullInsurance = driverA.hasHull
            )

            if (dALic == null && driverA.licSeries.isNotBlank() && driverA.licNumber.isNotBlank()) {
                val issueDate = parseDateOrNull(driverA.licIssueDate) ?: LocalDate.now()
                Repository.insertLicense(
                    driverId = driverAId,
                    series = driverA.licSeries,
                    number = driverA.licNumber,
                    category = driverA.licCategory.ifBlank { null },
                    issueDate = issueDate
                )
            }

            Repository.insertParticipant(
                accidentId = accidentId,
                driverId = driverAId,
                vehicleId = vehicleAId,
                role = 'A',
                impactSpot = null,
                damageDetails = damagesA.ifBlank { null },
                remarks = notesA.ifBlank { null }
            )

            // Водитель B
            val dBLic = if (driverB.licSeries.isNotBlank() && driverB.licNumber.isNotBlank()) {
                Repository.findLicenseBySeriesAndNumber(driverB.licSeries, driverB.licNumber)
            } else null

            val driverBId = dBLic?.driverId ?: Repository.getOrCreateDriver(
                fullName = driverB.fullName,
                birthDate = parseDateOrNull(driverB.birthdate),
                registrationAddress = driverB.address.ifBlank { null },
                actualAddress = driverB.address.ifBlank { null },
                phone = driverB.phone.ifBlank { null }
            )

            val ownerBId = if (vehicleB.ownerName.isNotBlank()) {
                Repository.getOrCreateDriver(vehicleB.ownerName, null, vehicleB.ownerAddress.ifBlank { null }, null, null)
            } else null

            val vehicleBId = Repository.getOrCreateVehicle(
                ownerId = ownerBId,
                brand = vehicleB.make,
                model = vehicleB.model,
                numberPlate = vehicleB.numberPlate,
                vin = vehicleB.vin.ifBlank { null },
                regCertificate = vehicleB.sts.ifBlank { null },
                insuranceName = driverB.insCompany.ifBlank { null },
                insurancePolicy = driverB.insPolicy.ifBlank { null },
                insuranceExpiry = parseDateOrNull(driverB.insExpiry),
                hasHullInsurance = driverB.hasHull
            )
            if (dBLic == null && driverB.licSeries.isNotBlank() && driverB.licNumber.isNotBlank()) {
                val issueDate = parseDateOrNull(driverB.licIssueDate) ?: LocalDate.now()
                Repository.insertLicense(
                    driverId = driverBId,
                    series = driverB.licSeries,
                    number = driverB.licNumber,
                    category = driverB.licCategory.ifBlank { null },
                    issueDate = issueDate
                )
            }

            Repository.insertParticipant(
                accidentId = accidentId,
                driverId = driverBId,
                vehicleId = vehicleBId,
                role = 'B',
                impactSpot = null,
                damageDetails = damagesB.ifBlank { null },
                remarks = notesB.ifBlank { null }
            )

            saveSuccess = true
            onSuccess()
        } catch (e: Exception) {
            saveError = "Ошибка сохранения: ${e.message}"
        } finally {
            isSaving = false
        }
    }

    fun reset() {
        currentStep = 0
        locationStreet = ""
        locationBuilding = ""
        accidentDate = java.time.LocalDate.now().toString()
        accidentTime = "12:00"
        witnessesName = ""
        witnessesAddress = ""
        noWitnesses = false
        vehicleA = VehicleFormData()
        vehicleB = VehicleFormData()
        driverA = DriverFormData()
        driverB = DriverFormData()
        damagesA = ""
        notesA = ""
        damagesB = ""
        notesB = ""
        selectedCircumstances.clear()
        accidentDescription = ""
        guiltySide = "не определён"
        saveError = ""
        saveSuccess = false
    }
}

private val GUILTY_OPTIONS = listOf("A", "B", "не определён")

@Composable
fun AccidentFormScreen(
    officerId: Int,
    onDone: () -> Unit
) {
    val state = remember { AccidentFormState() }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Оформление ДТП",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        // Индикатор шагов
        StepIndicator(current = state.currentStep, total = state.totalSteps)
        Spacer(Modifier.height(16.dp))

        // Контент шага
        Box(modifier = Modifier.weight(1f)) {
            when (state.currentStep) {
                0 -> Step1GeneralInfo(state)
                1 -> Step2VehicleData(state)
                2 -> Step3DriverData(state)
                3 -> Step4Damages(state)
                4 -> Step5Circumstances(state)
                5 -> Step6Completion(state)
            }
        }

        // Сообщение об ошибке
        if (state.saveError.isNotBlank()) {
            Text(state.saveError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
        }

        // Навигационные кнопки
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
private fun StepIndicator(current: Int, total: Int) {
    val stepNames = listOf(
        "Общее", "Данные ТС", "Водители", "Повреждения", "Обстоятельства", "Завершение"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stepNames.forEachIndexed { i, name ->
            val active = i == current
            val done = i < current
            FilterChip(
                selected = active || done,
                onClick = {},
                label = { Text("${i + 1}. $name", fontSize = 11.sp) },
                enabled = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Step1GeneralInfo(state: AccidentFormState) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Место ДТП")
        FormField("Улица", state.locationStreet) { state.locationStreet = it }
        FormField("Номер дома / км", state.locationBuilding) { state.locationBuilding = it }

        SectionTitle("Дата и время")
        FormField("Дата (гггг-мм-дд)", state.accidentDate) { state.accidentDate = it }
        FormField("Время (чч:мм)", state.accidentTime) { state.accidentTime = it }

        SectionTitle("Свидетели")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.noWitnesses, onCheckedChange = { state.noWitnesses = it })
            Text("Свидетели отсутствуют")
        }
        if (!state.noWitnesses) {
            FormField("ФИО свидетеля", state.witnessesName) { state.witnessesName = it }
            FormField("Адрес свидетеля", state.witnessesAddress) { state.witnessesAddress = it }
        }
    }
}

@Composable
private fun Step2VehicleData(state: AccidentFormState) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Госномер → Заполняет владельца. VIN → Заполняет марку, модель и страховку.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SectionTitle("ТС участника A")
        VehicleFields(
            data = state.vehicleA,
            onLookupPlate = { state.lookupOwnerAByPlate() },
            onLookupVin = { state.lookupVehicleAByVin() },
            onUpdate = { state.vehicleA = it }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionTitle("ТС участника B")
        VehicleFields(
            data = state.vehicleB,
            onLookupPlate = { state.lookupOwnerBByPlate() },
            onLookupVin = { state.lookupVehicleBByVin() },
            onUpdate = { state.vehicleB = it }
        )
    }
}

@Composable
private fun VehicleFields(
    data: VehicleFormData,
    onLookupPlate: () -> Unit,
    onLookupVin: () -> Unit,
    onUpdate: (VehicleFormData) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = data.numberPlate,
            onValueChange = { onUpdate(data.copy(numberPlate = it)) },
            label = { Text("Госномер (Поиск владельца)") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        IconButton(onClick = onLookupPlate) {
            Icon(Icons.Default.Search, contentDescription = "Найти по Госномеру")
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = data.vin,
            onValueChange = { onUpdate(data.copy(vin = it)) },
            label = { Text("VIN (Поиск авто и страховки)") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        IconButton(onClick = onLookupVin) {
            Icon(Icons.Default.Search, contentDescription = "Найти по VIN")
        }
    }

    FormField("Марка", data.make) { onUpdate(data.copy(make = it)) }
    FormField("Модель", data.model) { onUpdate(data.copy(model = it)) }
    FormField("СТС (серия, номер)", data.sts) { onUpdate(data.copy(sts = it)) }
    FormField("Собственник (ФИО)", data.ownerName) { onUpdate(data.copy(ownerName = it)) }
    FormField("Адрес собственника", data.ownerAddress) { onUpdate(data.copy(ownerAddress = it)) }
}

@Composable
private fun Step3DriverData(state: AccidentFormState) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Введите серию и номер ВУ и нажмите лупу для автозаполнения водителя.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SectionTitle("Водитель A")
        DriverFields(state.driverA, onLookup = { state.lookupDriverAByLicense() }) { state.driverA = it }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        SectionTitle("Водитель B")
        DriverFields(state.driverB, onLookup = { state.lookupDriverBByLicense() }) { state.driverB = it }
    }
}

@Composable
private fun DriverFields(data: DriverFormData, onLookup: () -> Unit, onUpdate: (DriverFormData) -> Unit) {
    Text("Водительское удостоверение", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = data.licSeries, onValueChange = { onUpdate(data.copy(licSeries = it)) },
            label = { Text("Серия") }, modifier = Modifier.weight(1f), singleLine = true
        )
        OutlinedTextField(
            value = data.licNumber, onValueChange = { onUpdate(data.copy(licNumber = it)) },
            label = { Text("Номер") }, modifier = Modifier.weight(1f), singleLine = true
        )
        IconButton(onClick = onLookup) {
            Icon(Icons.Default.Search, contentDescription = "Найти ВУ")
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = data.licCategory, onValueChange = { onUpdate(data.copy(licCategory = it)) },
            label = { Text("Категория") }, modifier = Modifier.weight(1f), singleLine = true
        )
        OutlinedTextField(
            value = data.licIssueDate, onValueChange = { onUpdate(data.copy(licIssueDate = it)) },
            label = { Text("Дата выдачи (гггг-мм-дд)") }, modifier = Modifier.weight(1f), singleLine = true
        )
    }

    FormField("ФИО", data.fullName) { onUpdate(data.copy(fullName = it)) }
    FormField("Дата рождения (гггг-мм-дд)", data.birthdate) { onUpdate(data.copy(birthdate = it)) }
    FormField("Адрес", data.address) { onUpdate(data.copy(address = it)) }
    FormField("Телефон", data.phone) { onUpdate(data.copy(phone = it)) }

    Text("Страховой полис", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
    FormField("Наименование страховой", data.insCompany) { onUpdate(data.copy(insCompany = it)) }
    FormField("Номер полиса", data.insPolicy) { onUpdate(data.copy(insPolicy = it)) }
    FormField("Дата окончания полиса", data.insExpiry) { onUpdate(data.copy(insExpiry = it)) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = data.hasHull, onCheckedChange = { onUpdate(data.copy(hasHull = it)) })
        Text("КАСКО")
    }
}

@Composable
private fun Step4Damages(state: AccidentFormState) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Повреждения ТС A")
        OutlinedTextField(
            value = state.damagesA, onValueChange = { state.damagesA = it },
            label = { Text("Перечень повреждений") },
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )
        OutlinedTextField(
            value = state.notesA, onValueChange = { state.notesA = it },
            label = { Text("Замечания") },
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SectionTitle("Повреждения ТС B")
        OutlinedTextField(
            value = state.damagesB, onValueChange = { state.damagesB = it },
            label = { Text("Перечень повреждений") },
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )
        OutlinedTextField(
            value = state.notesB, onValueChange = { state.notesB = it },
            label = { Text("Замечания") },
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )
    }
}

@Composable
private fun Step5Circumstances(state: AccidentFormState) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle("Обстоятельства ДТП")
        Text(
            "Отметьте все подходящие обстоятельства:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        ACCIDENT_CIRCUMSTANCES.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = state.selectedCircumstances.contains(item),
                    onCheckedChange = { state.toggleCircumstance(item) }
                )
                Text(item, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun Step6Completion(state: AccidentFormState) {
    val scroll = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Завершение оформления")
        OutlinedTextField(
            value = state.accidentDescription,
            onValueChange = { state.accidentDescription = it },
            label = { Text("Текстовое описание ДТП") },
            modifier = Modifier.fillMaxWidth().height(150.dp)
        )
        SectionTitle("Определение виновного")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GUILTY_OPTIONS.forEach { opt ->
                FilterChip(
                    selected = state.guiltySide == opt,
                    onClick = { state.guiltySide = opt },
                    label = { Text(opt) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Сводка:", fontWeight = FontWeight.SemiBold)
                Text("Место: ${state.locationStreet} ${state.locationBuilding}", fontSize = 13.sp)
                Text("Дата/время: ${state.accidentDate} ${state.accidentTime}", fontSize = 13.sp)
                Text("Виновный: ${state.guiltySide}", fontSize = 13.sp)
                Text("Обстоятельств: ${state.selectedCircumstances.size}", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun FormField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

private fun buildCircumstancesJson(items: List<String>): String =
    "[${items.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }}]"

private fun parseDateOrNull(value: String): LocalDate? =
    value.trim().takeIf { it.isNotBlank() }?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
    }
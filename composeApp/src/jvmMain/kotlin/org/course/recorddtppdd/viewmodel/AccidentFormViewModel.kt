package org.course.recorddtppdd.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.course.recorddtppdd.db.Repository
import org.course.recorddtppdd.model.AccidentType
import java.time.LocalDate
import java.time.LocalDateTime

/** Данные транспортного средства (для каждого участника) */
data class VehicleFormData(
    var make: String = "",
    var model: String = "",
    var vin: String = "",
    var numberPlate: String = "",
    var sts: String = "",
    var ownerName: String = "",
    var ownerAddress: String = ""
)

/** Данные водителя (для каждого участника) */
data class DriverFormData(
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

class AccidentFormViewModel {
    var currentStep by mutableStateOf(0)
    val totalSteps = 6

    // Шаг 1: Общая информация
    var locationStreet by mutableStateOf("")
    var locationBuilding by mutableStateOf("")
    var accidentDate by mutableStateOf(LocalDate.now().toString())
    var accidentTime by mutableStateOf("12:00")
    var witnessesName by mutableStateOf("")
    var witnessesAddress by mutableStateOf("")
    var noWitnesses by mutableStateOf(false)

    // Шаг 2-3: Данные ТС и водителей
    var vehicleA by mutableStateOf(VehicleFormData())
    var vehicleB by mutableStateOf(VehicleFormData())
    var driverA by mutableStateOf(DriverFormData())
    var driverB by mutableStateOf(DriverFormData())

    // Шаг 4: Повреждения
    var damagesA by mutableStateOf("")
    var notesA by mutableStateOf("")
    var damagesB by mutableStateOf("")
    var notesB by mutableStateOf("")

    // Шаг 5: Тип ДТП
    var accidentTypes by mutableStateOf<List<AccidentType>>(emptyList())
    var selectedAccidentTypeId by mutableStateOf<Int?>(null)

    // Шаг 6: Завершение
    var accidentDescription by mutableStateOf("")
    var guiltySide by mutableStateOf("не определён")

    var isSaving by mutableStateOf(false)
    var validationError by mutableStateOf("")
    var saveError by mutableStateOf("")
    var saveSuccess by mutableStateOf(false)

    init {
        loadAccidentTypes()
    }

    fun loadAccidentTypes() {
        runCatching { Repository.getAllAccidentTypes() }
            .onSuccess { accidentTypes = it }
            .onFailure { saveError = "Не удалось загрузить типы ДТП: ${it.message}" }
    }

    fun prevStep() {
        validationError = ""
        if (currentStep > 0) currentStep--
    }

    fun canGoNext(): Boolean = validateStep(currentStep) == null

    fun canSave(): Boolean = validateAllForSave() == null && !isSaving

    fun tryNextStep() {
        val error = validateStep(currentStep)
        if (error != null) {
            validationError = error
            return
        }
        validationError = ""
        if (currentStep < totalSteps - 1) currentStep++
    }

    /** Сохраняет запись ДТП в БД */
    fun save(officerId: Int, onSuccess: () -> Unit) {
        val validation = validateAllForSave()
        if (validation != null) {
            validationError = validation
            return
        }

        isSaving = true
        saveError = ""
        validationError = ""

        try {
            val dt = parseDateTimeOrNull() ?: throw IllegalArgumentException("Некорректные дата/время ДТП")
            val selectedType = accidentTypes.firstOrNull { it.id == selectedAccidentTypeId }
            val circumstancesJson = selectedType?.let { buildCircumstancesJson(listOf(it.name)) }

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

            val accidentId = Repository.insertAccident(
                officerId = officerId,
                typeId = selectedAccidentTypeId,
                dateTime = dt,
                street = locationStreet.trim(),
                house = locationBuilding.ifBlank { null },
                witnessesInfo = if (noWitnesses) null else witnessesInfo,
                circumstancesJson = circumstancesJson,
                explanation = explanation
            )

            val driverAId = Repository.getOrCreateDriver(
                fullName = driverA.fullName.trim(),
                birthDate = parseDateOrNull(driverA.birthdate),
                registrationAddress = driverA.address.ifBlank { null },
                actualAddress = driverA.address.ifBlank { null },
                phone = driverA.phone.ifBlank { null }
            )
            val vehicleAId = Repository.getOrCreateVehicle(
                ownerId = null,
                brand = vehicleA.make.trim(),
                model = vehicleA.model.trim(),
                numberPlate = vehicleA.numberPlate.trim(),
                vin = vehicleA.vin.ifBlank { null },
                regCertificate = vehicleA.sts.ifBlank { null },
                insuranceName = driverA.insCompany.ifBlank { null },
                insurancePolicy = driverA.insPolicy.ifBlank { null },
                insuranceExpiry = parseDateOrNull(driverA.insExpiry),
                hasHullInsurance = driverA.hasHull
            )
            if (driverA.licSeries.isNotBlank() && driverA.licNumber.isNotBlank()) {
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

            val driverBId = Repository.getOrCreateDriver(
                fullName = driverB.fullName.trim(),
                birthDate = parseDateOrNull(driverB.birthdate),
                registrationAddress = driverB.address.ifBlank { null },
                actualAddress = driverB.address.ifBlank { null },
                phone = driverB.phone.ifBlank { null }
            )
            val vehicleBId = Repository.getOrCreateVehicle(
                ownerId = null,
                brand = vehicleB.make.trim(),
                model = vehicleB.model.trim(),
                numberPlate = vehicleB.numberPlate.trim(),
                vin = vehicleB.vin.ifBlank { null },
                regCertificate = vehicleB.sts.ifBlank { null },
                insuranceName = driverB.insCompany.ifBlank { null },
                insurancePolicy = driverB.insPolicy.ifBlank { null },
                insuranceExpiry = parseDateOrNull(driverB.insExpiry),
                hasHullInsurance = driverB.hasHull
            )
            if (driverB.licSeries.isNotBlank() && driverB.licNumber.isNotBlank()) {
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
        accidentDate = LocalDate.now().toString()
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
        selectedAccidentTypeId = null
        accidentDescription = ""
        guiltySide = "не определён"
        validationError = ""
        saveError = ""
        saveSuccess = false
        loadAccidentTypes()
    }

    private fun validateAllForSave(): String? {
        for (step in 0 until totalSteps) {
            val stepError = validateStep(step)
            if (stepError != null) return stepError
        }
        return null
    }

    private fun validateStep(step: Int): String? {
        return when (step) {
            0 -> {
                when {
                    locationStreet.isBlank() -> "Укажите улицу ДТП (обязательное поле)."
                    parseDateTimeOrNull() == null -> "Укажите корректные дату и время ДТП."
                    else -> null
                }
            }

            1 -> {
                when {
                    vehicleA.make.isBlank() || vehicleA.model.isBlank() || vehicleA.numberPlate.isBlank() ->
                        "Для ТС A заполните марку, модель и госномер."

                    vehicleB.make.isBlank() || vehicleB.model.isBlank() || vehicleB.numberPlate.isBlank() ->
                        "Для ТС B заполните марку, модель и госномер."

                    else -> null
                }
            }

            2 -> {
                when {
                    !hasRequiredName(driverA.fullName) -> "Для водителя A укажите минимум фамилию и имя."
                    !hasRequiredName(driverB.fullName) -> "Для водителя B укажите минимум фамилию и имя."
                    else -> null
                }
            }

            4 -> if (selectedAccidentTypeId == null) "Выберите тип/обстоятельство ДТП из справочника."
            else null

            else -> null
        }
    }

    private fun hasRequiredName(value: String): Boolean {
        return value.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size >= 2
    }

    private fun parseDateTimeOrNull(): LocalDateTime? {
        val time = accidentTime.trim().padStart(5, '0')
        return runCatching { LocalDateTime.parse("${accidentDate}T${time}:00") }.getOrNull()
    }
}

/** Кодирует список строк в JSON-массив */
fun buildCircumstancesJson(items: List<String>): String =
    "[${items.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }}]"

private fun parseDateOrNull(value: String): LocalDate? =
    value.trim().takeIf { it.isNotBlank() }?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
    }

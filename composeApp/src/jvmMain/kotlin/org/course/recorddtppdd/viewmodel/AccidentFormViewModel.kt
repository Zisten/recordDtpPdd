package org.course.recorddtppdd.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.course.recorddtppdd.db.Repository
import java.time.LocalDate

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
    // ВУ
    var licSeries: String = "",
    var licNumber: String = "",
    var licCategory: String = "",
    var licIssueDate: String = "",
    // Страховка
    var insCompany: String = "",
    var insPolicy: String = "",
    var insExpiry: String = "",
    var hasHull: Boolean = false
)

val ACCIDENT_CIRCUMSTANCES = listOf(
    "Превышение скорости",
    "Проезд на запрещающий сигнал светофора",
    "Несоблюдение дистанции",
    "Нарушение правил обгона",
    "Нарушение правил поворота/разворота",
    "Нарушение правил проезда перекрёстка",
    "Выезд на встречную полосу движения",
    "Нарушение правил остановки/стоянки",
    "Управление ТС в состоянии опьянения",
    "Неисправность транспортного средства",
    "Неудовлетворительное состояние дороги",
    "Плохая видимость (туман, снегопад)",
    "Нарушение правил проезда ж/д переезда",
    "Нарушение правил перестроения",
    "Иное"
)

class AccidentFormViewModel {
    var currentStep by mutableStateOf(0)
    val totalSteps = 7

    // Шаг 1: Общая информация
    var locationStreet by mutableStateOf("")
    var locationBuilding by mutableStateOf("")
    var accidentDate by mutableStateOf(java.time.LocalDate.now().toString())
    var accidentTime by mutableStateOf("12:00")
    var witnessesName by mutableStateOf("")
    var witnessesAddress by mutableStateOf("")
    var noWitnesses by mutableStateOf(false)

    // Шаг 2: Выбор ТС (A или B — какое заполняем первым)
    var currentSide by mutableStateOf("A")  // редактируемая сторона в шаге 3/4/5

    // Шаг 3 & 4: Данные ТС и водителей
    var vehicleA by mutableStateOf(VehicleFormData())
    var vehicleB by mutableStateOf(VehicleFormData())
    var driverA by mutableStateOf(DriverFormData())
    var driverB by mutableStateOf(DriverFormData())

    // Шаг 5: Повреждения
    var damagesA by mutableStateOf("")
    var notesA by mutableStateOf("")
    var damagesB by mutableStateOf("")
    var notesB by mutableStateOf("")

    // Шаг 6: Обстоятельства
    val selectedCircumstances = mutableStateListOf<String>()

    // Шаг 7: Завершение
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

    /** Сохраняет запись ДТП в БД */
    fun save(officerId: Int, onSuccess: () -> Unit) {
        isSaving = true
        saveError = ""
        try {
            val dt = java.time.LocalDateTime.parse(
                "${accidentDate}T${accidentTime.padStart(5, '0')}:00"
            )
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

            // Участник A
            val driverAId = Repository.getOrCreateDriver(
                fullName = driverA.fullName,
                birthDate = parseDateOrNull(driverA.birthdate),
                registrationAddress = driverA.address.ifBlank { null },
                actualAddress = driverA.address.ifBlank { null },
                phone = driverA.phone.ifBlank { null }
            )
            val vehicleAId = Repository.getOrCreateVehicle(
                ownerId = null,
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

            // Участник B
            val driverBId = Repository.getOrCreateDriver(
                fullName = driverB.fullName,
                birthDate = parseDateOrNull(driverB.birthdate),
                registrationAddress = driverB.address.ifBlank { null },
                actualAddress = driverB.address.ifBlank { null },
                phone = driverB.phone.ifBlank { null }
            )
            val vehicleBId = Repository.getOrCreateVehicle(
                ownerId = null,
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
        accidentDate = java.time.LocalDate.now().toString()
        accidentTime = "12:00"
        witnessesName = ""
        witnessesAddress = ""
        noWitnesses = false
        currentSide = "A"
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

/** Кодирует список строк в JSON-массив */
fun buildCircumstancesJson(items: List<String>): String =
    "[${items.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }}]"

private fun parseDateOrNull(value: String): LocalDate? =
    value.trim().takeIf { it.isNotBlank() }?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
    }

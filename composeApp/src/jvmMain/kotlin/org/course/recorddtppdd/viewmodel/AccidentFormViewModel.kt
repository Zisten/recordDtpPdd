package org.course.recorddtppdd.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.course.recorddtppdd.db.Repository

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

            val accidentId = Repository.insertAccident(
                locationStreet = locationStreet,
                locationBuilding = locationBuilding,
                datetime = dt,
                witnessesName = if (noWitnesses) "" else witnessesName,
                witnessesAddress = if (noWitnesses) "" else witnessesAddress,
                description = accidentDescription,
                circumstances = circumstances,
                guiltySide = guiltySide,
                officerId = officerId
            )

            // Участник A
            val driverAId = Repository.getOrCreateDriver(
                driverA.fullName, driverA.birthdate, driverA.address, driverA.phone
            )
            val vehicleAId = Repository.getOrCreateVehicle(
                vehicleA.make, vehicleA.model, vehicleA.vin,
                vehicleA.numberPlate, vehicleA.sts, vehicleA.ownerName, vehicleA.ownerAddress
            )
            val licenseAId = if (driverA.licSeries.isNotBlank()) {
                Repository.insertLicense(driverAId, driverA.licSeries, driverA.licNumber,
                    driverA.licCategory, driverA.licIssueDate)
            } else null

            Repository.insertParticipant(
                accidentId = accidentId, side = "A",
                driverId = driverAId, vehicleId = vehicleAId,
                damages = damagesA, notes = notesA, licenseId = licenseAId,
                insuranceCompany = driverA.insCompany, insurancePolicy = driverA.insPolicy,
                insuranceExpiry = driverA.insExpiry, hasHull = driverA.hasHull
            )

            // Участник B
            val driverBId = Repository.getOrCreateDriver(
                driverB.fullName, driverB.birthdate, driverB.address, driverB.phone
            )
            val vehicleBId = Repository.getOrCreateVehicle(
                vehicleB.make, vehicleB.model, vehicleB.vin,
                vehicleB.numberPlate, vehicleB.sts, vehicleB.ownerName, vehicleB.ownerAddress
            )
            val licenseBId = if (driverB.licSeries.isNotBlank()) {
                Repository.insertLicense(driverBId, driverB.licSeries, driverB.licNumber,
                    driverB.licCategory, driverB.licIssueDate)
            } else null

            Repository.insertParticipant(
                accidentId = accidentId, side = "B",
                driverId = driverBId, vehicleId = vehicleBId,
                damages = damagesB, notes = notesB, licenseId = licenseBId,
                insuranceCompany = driverB.insCompany, insurancePolicy = driverB.insPolicy,
                insuranceExpiry = driverB.insExpiry, hasHull = driverB.hasHull
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

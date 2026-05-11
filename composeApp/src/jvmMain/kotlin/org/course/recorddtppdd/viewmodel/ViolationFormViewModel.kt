package org.course.recorddtppdd.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.course.recorddtppdd.db.Repository
import org.course.recorddtppdd.model.ViolationType
import java.time.LocalDate
import java.time.LocalDateTime

class ViolationFormViewModel {
    var currentStep by mutableStateOf(0)
    val totalSteps = 3

    // Шаг 1: Данные нарушителя + ВУ
    var driverFullName by mutableStateOf("")
    var driverBirthdate by mutableStateOf("")
    var driverAddress by mutableStateOf("")
    var driverPhone by mutableStateOf("")
    var licSeries by mutableStateOf("")
    var licNumber by mutableStateOf("")
    var licCategory by mutableStateOf("")
    var licIssueDate by mutableStateOf("")

    // Шаг 2: Данные ТС (с автоподстановкой)
    var vehiclePlate by mutableStateOf("")
    var vehicleBrand by mutableStateOf("") // Переименовано с vehicleMake
    var vehicleModel by mutableStateOf("")
    var vehicleOwner by mutableStateOf("")
    var vehicleSts by mutableStateOf("")
    var vehicleVin by mutableStateOf("")
    var vehicleOwnerAddress by mutableStateOf("")

    // Шаг 3: Нарушение
    var violationTypeName by mutableStateOf("")
    var npaPoint by mutableStateOf("")
    var violationStreet by mutableStateOf("")
    var violationHouse by mutableStateOf("")
    var description by mutableStateOf("")
    var witnessName by mutableStateOf("")
    var witnessAddress by mutableStateOf("")
    var witnessPhone by mutableStateOf("")
    var violationDate by mutableStateOf(java.time.LocalDate.now().toString())
    var violationTime by mutableStateOf("12:00")

    var isSaving by mutableStateOf(false)
    var saveError by mutableStateOf("")
    var saveSuccess by mutableStateOf(false)
    val violationTypes = mutableStateListOf<ViolationType>()

    init {
        loadViolationTypes()
    }

    fun nextStep(): Boolean {
        val validationError = validateStep(currentStep)
        if (validationError != null) {
            saveError = validationError
            return false
        }
        saveError = ""
        if (currentStep < totalSteps - 1) currentStep++
        return true
    }
    fun prevStep() { if (currentStep > 0) currentStep-- }

    fun loadViolationTypes() {
        try {
            val types = Repository.getAllViolationTypes()
            violationTypes.clear()
            violationTypes.addAll(types)
        } catch (_: Exception) {
            violationTypes.clear()
        }
    }

    fun selectViolationClause(clause: String) {
        npaPoint = clause
        violationTypes.firstOrNull { it.clause == clause }?.let { type ->
            violationTypeName = type.description
        }
    }

    /**
     * Автоподстановка данных ТС по госномеру из БД.
     */
    fun lookupVehicle() {
        if (vehiclePlate.isBlank()) return
        try {
            val v = Repository.findVehicleByPlate(vehiclePlate)
            if (v != null) {
                vehicleBrand = v.brand // Исправлено: используется v.brand
                vehicleModel = v.model
                vehicleVin = v.vin ?: ""
                vehicleSts = v.regCertificate ?: ""
                // Логика для поиска владельца по ownerId (упрощено)
            }
        } catch (_: Exception) { /* игнорируем ошибки поиска */ }
    }

    fun save(officerId: Int, onSuccess: () -> Unit) {
        isSaving = true
        saveError = ""
        try {
            val finalValidationError = validateAllRequiredFields()
            if (finalValidationError != null) {
                saveError = finalValidationError
                return
            }
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
                brand = vehicleBrand, // Исправлено: используется vehicleBrand
                model = vehicleModel,
                numberPlate = vehiclePlate,
                vin = vehicleVin.ifBlank { null },
                regCertificate = vehicleSts.ifBlank { null },
                insuranceName = null,
                insurancePolicy = null,
                insuranceExpiry = null,
                hasHullInsurance = null
            )

            val violationTypeId = violationTypes
                .firstOrNull { it.clause == npaPoint }
                ?.id
                ?: throw IllegalStateException("Выберите пункт НПА из списка ViolationsTypes")

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
                street = violationStreet.trim(),
                houseNumber = violationHouse.trim().ifBlank { null },
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
        vehicleBrand = "" // Исправлено
        vehicleModel = ""
        vehicleOwner = ""
        vehicleSts = ""
        vehicleVin = ""
        vehicleOwnerAddress = ""
        violationTypeName = ""
        npaPoint = ""
        violationStreet = ""
        violationHouse = ""
        description = ""
        witnessName = ""
        witnessAddress = ""
        witnessPhone = ""
        violationDate = java.time.LocalDate.now().toString()
        violationTime = "12:00"
        saveError = ""
        saveSuccess = false
        loadViolationTypes()
    }

    private fun validateStep(step: Int): String? = when (step) {
        0 -> if (!isValidFullName(driverFullName)) {
            "Заполните обязательное поле: ФИО нарушителя (минимум фамилия и имя)"
        } else null
        1 -> {
            val missing = mutableListOf<String>()
            if (vehicleBrand.isBlank()) missing += "марка ТС"
            if (vehicleModel.isBlank()) missing += "модель ТС"
            if (vehiclePlate.isBlank()) missing += "госномер ТС"
            missing.takeIf { it.isNotEmpty() }?.let {
                "Заполните обязательные поля: ${it.joinToString(", ")}"
            }
        }
        2 -> when {
            npaPoint.isBlank() -> "Выберите пункт НПА из списка"
            violationTypeName.isBlank() -> "Не удалось определить вид нарушения по выбранному НПА"
            violationStreet.isBlank() -> "Заполните обязательное поле: улица нарушения"
            violationDate.isBlank() -> "Заполните обязательное поле: дата нарушения"
            violationTime.isBlank() -> "Заполните обязательное поле: время нарушения"
            else -> null
        }
        else -> null
    }

    private fun validateAllRequiredFields(): String? {
        val validationSteps = listOf(0, 1, 2)
        return validationSteps.firstNotNullOfOrNull { step -> validateStep(step) }
    }

    private fun isValidFullName(value: String): Boolean =
        value.trim().split(Regex("\\s+")).size >= 2
}

private fun parseDateOrNull(value: String): LocalDate? =
    value.trim().takeIf { it.isNotBlank() }?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
    }

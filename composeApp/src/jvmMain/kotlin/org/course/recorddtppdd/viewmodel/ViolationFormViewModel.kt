package org.course.recorddtppdd.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.course.recorddtppdd.db.Repository
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

            val violationTypeId = Repository.getOrCreateViolationType(
                clause = npaPoint,
                description = description,
                fineAmount = 0 // Предполагаем 0 как штраф по умолчанию
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
                street = "", // Улица отсутствует в форме, используется пустая строка
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
        vehicleBrand = "" // Исправлено
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

private fun parseDateOrNull(value: String): LocalDate? =
    value.trim().takeIf { it.isNotBlank() }?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
    }
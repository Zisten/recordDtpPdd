package org.course.recorddtppdd.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.course.recorddtppdd.db.Repository
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
    var vehicleMake by mutableStateOf("")
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
                vehicleMake = v.make
                vehicleModel = v.model
                vehicleOwner = v.ownerName
                vehicleSts = v.sts
                vehicleVin = v.vin
                vehicleOwnerAddress = v.ownerAddress
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
                driverFullName, driverBirthdate, driverAddress, driverPhone
            )
            val licenseId = if (licSeries.isNotBlank()) {
                Repository.insertLicense(driverId, licSeries, licNumber, licCategory, licIssueDate)
            } else null

            val vehicleId = if (vehiclePlate.isNotBlank()) {
                Repository.getOrCreateVehicle(
                    vehicleMake, vehicleModel, vehicleVin,
                    vehiclePlate, vehicleSts, vehicleOwner, vehicleOwnerAddress
                )
            } else null

            val violationTypeId = if (violationTypeName.isNotBlank()) {
                Repository.getOrCreateViolationType(violationTypeName)
            } else null

            Repository.insertViolation(
                driverId = driverId,
                vehicleId = vehicleId,
                licenseId = licenseId,
                violationTypeId = violationTypeId,
                npaPoint = npaPoint,
                description = description,
                witnessName = witnessName,
                witnessAddress = witnessAddress,
                witnessPhone = witnessPhone,
                datetime = dt,
                officerId = officerId
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
        driverFullName = ""
        driverBirthdate = ""
        driverAddress = ""
        driverPhone = ""
        licSeries = ""
        licNumber = ""
        licCategory = ""
        licIssueDate = ""
        vehiclePlate = ""
        vehicleMake = ""
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

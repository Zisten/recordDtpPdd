package org.course.recorddtppdd.db

import org.course.recorddtppdd.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/**
 * Репозиторий — все операции с базой данных через Exposed DSL.
 * Каждый метод оборачивается в transaction { }.
 */
object Repository {

    // ── Officers ─────────────────────────────────────────────────────────────

    fun findOfficerByCredentials(login: String, password: String): Officer? = transaction {
        Officers.selectAll()
            .where { (Officers.login eq login) and (Officers.password eq password) }
            .map { toOfficer(it) }
            .firstOrNull()
    }

    private fun toOfficer(row: ResultRow) = Officer(
        id = row[Officers.id],
        login = row[Officers.login],
        password = row[Officers.password]
    )

    // ── Drivers ──────────────────────────────────────────────────────────────

    fun getAllDrivers(): List<Driver> = transaction {
        Drivers.selectAll().map { toDriver(it) }
    }

    fun findDriverByNameAndPhone(name: String, phone: String): Driver? = transaction {
        Drivers.selectAll()
            .where { (Drivers.fullName eq name) and (Drivers.phone eq phone) }
            .map { toDriver(it) }
            .firstOrNull()
    }

    fun insertDriver(fullName: String, birthdate: String, address: String, phone: String): Int = transaction {
        Drivers.insertAndGetId {
            it[Drivers.fullName] = fullName
            it[Drivers.birthdate] = birthdate
            it[Drivers.address] = address
            it[Drivers.phone] = phone
        }.value
    }

    fun getOrCreateDriver(fullName: String, birthdate: String, address: String, phone: String): Int {
        return findDriverByNameAndPhone(fullName, phone)?.id
            ?: insertDriver(fullName, birthdate, address, phone)
    }

    private fun toDriver(row: ResultRow) = Driver(
        id = row[Drivers.id],
        fullName = row[Drivers.fullName],
        birthdate = row[Drivers.birthdate],
        address = row[Drivers.address],
        phone = row[Drivers.phone]
    )

    // ── Licenses ─────────────────────────────────────────────────────────────

    fun insertLicense(driverId: Int, series: String, number: String, category: String, issueDate: String): Int =
        transaction {
            Licenses.insertAndGetId {
                it[Licenses.driverId] = driverId
                it[Licenses.series] = series
                it[Licenses.number] = number
                it[Licenses.category] = category
                it[Licenses.issueDate] = issueDate
            }.value
        }

    fun findLicenseByDriverId(driverId: Int): License? = transaction {
        Licenses.selectAll()
            .where { Licenses.driverId eq driverId }
            .map { toLicense(it) }
            .lastOrNull()
    }

    private fun toLicense(row: ResultRow) = License(
        id = row[Licenses.id],
        driverId = row[Licenses.driverId],
        series = row[Licenses.series],
        number = row[Licenses.number],
        category = row[Licenses.category],
        issueDate = row[Licenses.issueDate]
    )

    // ── Vehicles ─────────────────────────────────────────────────────────────

    fun getAllVehicles(): List<Vehicle> = transaction {
        Vehicles.selectAll().map { toVehicle(it) }
    }

    fun findVehicleByPlate(plate: String): Vehicle? = transaction {
        Vehicles.selectAll()
            .where { Vehicles.numberPlate eq plate }
            .map { toVehicle(it) }
            .firstOrNull()
    }

    fun insertVehicle(
        make: String, model: String, vin: String, numberPlate: String,
        sts: String, ownerName: String, ownerAddress: String
    ): Int = transaction {
        Vehicles.insertAndGetId {
            it[Vehicles.make] = make
            it[Vehicles.model] = model
            it[Vehicles.vin] = vin
            it[Vehicles.numberPlate] = numberPlate
            it[Vehicles.sts] = sts
            it[Vehicles.ownerName] = ownerName
            it[Vehicles.ownerAddress] = ownerAddress
        }.value
    }

    fun getOrCreateVehicle(
        make: String, model: String, vin: String, numberPlate: String,
        sts: String, ownerName: String, ownerAddress: String
    ): Int {
        return findVehicleByPlate(numberPlate)?.id
            ?: insertVehicle(make, model, vin, numberPlate, sts, ownerName, ownerAddress)
    }

    private fun toVehicle(row: ResultRow) = Vehicle(
        id = row[Vehicles.id],
        make = row[Vehicles.make],
        model = row[Vehicles.model],
        vin = row[Vehicles.vin],
        numberPlate = row[Vehicles.numberPlate],
        sts = row[Vehicles.sts],
        ownerName = row[Vehicles.ownerName],
        ownerAddress = row[Vehicles.ownerAddress]
    )

    // ── AccidentsRecords ─────────────────────────────────────────────────────

    fun getAllAccidents(): List<AccidentRecord> = transaction {
        AccidentsRecords.selectAll()
            .orderBy(AccidentsRecords.datetime, SortOrder.DESC)
            .map { toAccident(it) }
    }

    fun insertAccident(
        locationStreet: String, locationBuilding: String,
        datetime: LocalDateTime, witnessesName: String, witnessesAddress: String,
        description: String, circumstances: String, guiltySide: String, officerId: Int
    ): Int = transaction {
        AccidentsRecords.insertAndGetId {
            it[AccidentsRecords.locationStreet] = locationStreet
            it[AccidentsRecords.locationBuilding] = locationBuilding
            it[AccidentsRecords.datetime] = datetime
            it[AccidentsRecords.witnessesName] = witnessesName
            it[AccidentsRecords.witnessesAddress] = witnessesAddress
            it[AccidentsRecords.description] = description
            it[AccidentsRecords.circumstances] = circumstances
            it[AccidentsRecords.guiltySide] = guiltySide
            it[AccidentsRecords.officerId] = officerId
            it[AccidentsRecords.createdAt] = LocalDateTime.now()
        }.value
    }

    private fun toAccident(row: ResultRow) = AccidentRecord(
        id = row[AccidentsRecords.id],
        locationStreet = row[AccidentsRecords.locationStreet],
        locationBuilding = row[AccidentsRecords.locationBuilding],
        datetime = row[AccidentsRecords.datetime],
        witnessesName = row[AccidentsRecords.witnessesName],
        witnessesAddress = row[AccidentsRecords.witnessesAddress],
        description = row[AccidentsRecords.description],
        circumstances = row[AccidentsRecords.circumstances],
        guiltySide = row[AccidentsRecords.guiltySide],
        officerId = row[AccidentsRecords.officerId],
        createdAt = row[AccidentsRecords.createdAt]
    )

    // ── AccidentParticipants ────────────────────────────────────────────────

    fun insertParticipant(
        accidentId: Int, side: String, driverId: Int, vehicleId: Int,
        damages: String, notes: String, licenseId: Int?,
        insuranceCompany: String, insurancePolicy: String,
        insuranceExpiry: String, hasHull: Boolean
    ) = transaction {
        AccidentParticipants.insert {
            it[AccidentParticipants.accidentId] = accidentId
            it[AccidentParticipants.side] = side
            it[AccidentParticipants.driverId] = driverId
            it[AccidentParticipants.vehicleId] = vehicleId
            it[AccidentParticipants.damages] = damages
            it[AccidentParticipants.notes] = notes
            it[AccidentParticipants.licenseId] = licenseId
            it[AccidentParticipants.insuranceCompany] = insuranceCompany
            it[AccidentParticipants.insurancePolicy] = insurancePolicy
            it[AccidentParticipants.insuranceExpiry] = insuranceExpiry
            it[AccidentParticipants.hasHull] = hasHull
        }
    }

    // ── ViolationsTypes ─────────────────────────────────────────────────────

    fun getAllViolationTypes(): List<ViolationType> = transaction {
        ViolationsTypes.selectAll().map { toViolationType(it) }
    }

    fun getOrCreateViolationType(name: String): Int = transaction {
        ViolationsTypes.selectAll()
            .where { ViolationsTypes.name eq name }
            .map { it[ViolationsTypes.id] }
            .firstOrNull()
            ?: ViolationsTypes.insertAndGetId {
                it[ViolationsTypes.name] = name
            }.value
    }

    private fun toViolationType(row: ResultRow) = ViolationType(
        id = row[ViolationsTypes.id],
        name = row[ViolationsTypes.name],
        description = row[ViolationsTypes.description]
    )

    // ── ViolationRecords ────────────────────────────────────────────────────

    fun getAllViolations(): List<ViolationRecord> = transaction {
        ViolationRecords.selectAll()
            .orderBy(ViolationRecords.datetime, SortOrder.DESC)
            .map { toViolation(it) }
    }

    fun insertViolation(
        driverId: Int, vehicleId: Int?, licenseId: Int?, violationTypeId: Int?,
        npaPoint: String, description: String, witnessName: String,
        witnessAddress: String, witnessPhone: String,
        datetime: LocalDateTime, officerId: Int
    ): Int = transaction {
        ViolationRecords.insertAndGetId {
            it[ViolationRecords.driverId] = driverId
            it[ViolationRecords.vehicleId] = vehicleId
            it[ViolationRecords.licenseId] = licenseId
            it[ViolationRecords.violationTypeId] = violationTypeId
            it[ViolationRecords.npaPoint] = npaPoint
            it[ViolationRecords.description] = description
            it[ViolationRecords.witnessName] = witnessName
            it[ViolationRecords.witnessAddress] = witnessAddress
            it[ViolationRecords.witnessPhone] = witnessPhone
            it[ViolationRecords.datetime] = datetime
            it[ViolationRecords.officerId] = officerId
            it[ViolationRecords.createdAt] = LocalDateTime.now()
        }.value
    }

    private fun toViolation(row: ResultRow) = ViolationRecord(
        id = row[ViolationRecords.id],
        driverId = row[ViolationRecords.driverId],
        vehicleId = row[ViolationRecords.vehicleId],
        licenseId = row[ViolationRecords.licenseId],
        violationTypeId = row[ViolationRecords.violationTypeId],
        npaPoint = row[ViolationRecords.npaPoint],
        description = row[ViolationRecords.description],
        witnessName = row[ViolationRecords.witnessName],
        witnessAddress = row[ViolationRecords.witnessAddress],
        witnessPhone = row[ViolationRecords.witnessPhone],
        datetime = row[ViolationRecords.datetime],
        officerId = row[ViolationRecords.officerId],
        createdAt = row[ViolationRecords.createdAt]
    )

    // ── Stats ───────────────────────────────────────────────────────────────

    fun getHomeStats(): HomeStats {
        val today = java.time.LocalDate.now()
        val accidents = getAllAccidents().count { it.datetime.toLocalDate() == today }
        val violations = getAllViolations().count { it.datetime.toLocalDate() == today }
        return HomeStats(accidents, violations)
    }
}

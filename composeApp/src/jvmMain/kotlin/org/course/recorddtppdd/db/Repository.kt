package org.course.recorddtppdd.db

import org.course.recorddtppdd.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.regex.Pattern

object Repository {

    // ── Officers ───────────────────────────────────────────────────────────

    fun findOfficerByCredentials(login: String, password: String): Officer? = transaction {
        Officers
            .selectAll()
            .where { (Officers.login eq login) and (Officers.password eq password) }
            .map { toOfficer(it) }
            .firstOrNull()
    }

    private fun toOfficer(row: ResultRow) = Officer(
        id = row[Officers.id],
        login = row[Officers.login],
        password = row[Officers.password]
    )

    // ── Drivers ───────────────────────────────────────────────────────────

    fun getAllDrivers(): List<Driver> = transaction {
        Drivers.selectAll().map { toDriver(it) }
    }

    fun findDriverById(id: Int): Driver? = transaction {
        Drivers.selectAll()
            .where { Drivers.id eq id }
            .map { toDriver(it) }
            .firstOrNull()
    }

    fun findDriverByNameAndPhone(fullName: String, phone: String?): Driver? = transaction {
        val (ln, fn, mn) = splitFullName(fullName)

        val base = (Drivers.lastName eq ln) and (Drivers.firstName eq fn)

        // Более гибкий поиск: если телефон передан, ищем с ним, если нет — только по ФИО
        val phoneExpr: Op<Boolean> = if (phone.isNullOrBlank()) Op.TRUE else Drivers.phone eq phone

        val middleExpr: Op<Boolean> = if (mn == null) {
            Drivers.middleName.isNull() or (Drivers.middleName eq "")
        } else {
            Drivers.middleName eq mn
        }

        Drivers
            .selectAll()
            .where { base and middleExpr and phoneExpr }
            .map { toDriver(it) }
            .firstOrNull()
    }

    fun insertDriver(
        fullName: String,
        birthDate: LocalDate?,
        registrationAddress: String?,
        actualAddress: String?,
        phone: String?
    ): Int = transaction {
        val (ln, fn, mn) = splitFullName(fullName)

        Drivers.insert {
            it[lastName] = ln
            it[firstName] = fn
            it[middleName] = mn
            it[Drivers.birthDate] = birthDate
            it[Drivers.registrationAddress] = registrationAddress
            it[Drivers.actualAddress] = actualAddress
            it[Drivers.phone] = phone
        }[Drivers.id]
    }

    fun getOrCreateDriver(
        fullName: String,
        birthDate: LocalDate?,
        registrationAddress: String?,
        actualAddress: String?,
        phone: String?
    ): Int {
        return findDriverByNameAndPhone(fullName, phone)?.id
            ?: insertDriver(fullName, birthDate, registrationAddress, actualAddress, phone)
    }

    private fun toDriver(row: ResultRow): Driver {
        val fullName = buildFullName(row[Drivers.lastName], row[Drivers.firstName], row[Drivers.middleName])
        return Driver(
            id = row[Drivers.id],
            fullName = fullName,
            birthDate = row[Drivers.birthDate],
            registrationAddress = row[Drivers.registrationAddress],
            actualAddress = row[Drivers.actualAddress],
            phone = row[Drivers.phone]
        )
    }

    // ── Licenses ───────────────────────────────────────────────────────────

    fun insertLicense(
        driverId: Int,
        series: String,
        number: String,
        category: String?,
        issueDate: LocalDate
    ): Int = transaction {
        Licenses.insert {
            it[Licenses.driverId] = driverId
            it[Licenses.series] = series
            it[Licenses.number] = number
            it[Licenses.category] = category
            it[Licenses.issueDate] = issueDate
        }[Licenses.id]
    }

    fun findLicenseByDriverId(driverId: Int): License? = transaction {
        Licenses.selectAll()
            .where { Licenses.driverId eq driverId }
            .map { toLicense(it) }
            .lastOrNull()
    }

    fun findLicenseBySeriesAndNumber(series: String, number: String): License? = transaction {
        Licenses.selectAll()
            .where { (Licenses.series eq series) and (Licenses.number eq number) }
            .map { toLicense(it) }
            .firstOrNull()
    }

    private fun toLicense(row: ResultRow) = License(
        id = row[Licenses.id],
        driverId = row[Licenses.driverId],
        series = row[Licenses.series],
        number = row[Licenses.number],
        category = row[Licenses.category],
        issueDate = row[Licenses.issueDate]
    )

    // ── Vehicles ───────────────────────────────────────────────────────────

    fun getAllVehicles(): List<Vehicle> = transaction {
        Vehicles.selectAll().map { toVehicle(it) }
    }

    fun findVehicleByPlate(plate: String): Vehicle? = transaction {
        Vehicles.selectAll()
            .where { Vehicles.numberPlate eq plate }
            .map { toVehicle(it) }
            .firstOrNull()
    }

    // НОВЫЙ МЕТОД: Поиск по VIN
    fun findVehicleByVin(vin: String): Vehicle? = transaction {
        Vehicles.selectAll()
            .where { Vehicles.vin eq vin }
            .map { toVehicle(it) }
            .firstOrNull()
    }

    fun insertVehicle(
        ownerId: Int?,
        brand: String,
        model: String,
        numberPlate: String,
        vin: String?,
        regCertificate: String?,
        insuranceName: String?,
        insurancePolicy: String?,
        insuranceExpiry: LocalDate?,
        hasHullInsurance: Boolean?
    ): Int = transaction {
        Vehicles.insert {
            it[Vehicles.ownerId] = ownerId
            it[Vehicles.brand] = brand
            it[Vehicles.model] = model
            it[Vehicles.numberPlate] = numberPlate
            it[Vehicles.vin] = vin
            it[Vehicles.regCertificate] = regCertificate
            it[Vehicles.insuranceName] = insuranceName
            it[Vehicles.insurancePolicy] = insurancePolicy
            it[Vehicles.insuranceExpiry] = insuranceExpiry
            it[Vehicles.hasHullInsurance] = hasHullInsurance
        }[Vehicles.id]
    }

    fun getOrCreateVehicle(
        ownerId: Int?,
        brand: String,
        model: String,
        numberPlate: String,
        vin: String?,
        regCertificate: String?,
        insuranceName: String?,
        insurancePolicy: String?,
        insuranceExpiry: LocalDate?,
        hasHullInsurance: Boolean?
    ): Int {
        return findVehicleByPlate(numberPlate)?.id
            ?: insertVehicle(
                ownerId = ownerId,
                brand = brand,
                model = model,
                numberPlate = numberPlate,
                vin = vin,
                regCertificate = regCertificate,
                insuranceName = insuranceName,
                insurancePolicy = insurancePolicy,
                insuranceExpiry = insuranceExpiry,
                hasHullInsurance = hasHullInsurance
            )
    }

    private fun toVehicle(row: ResultRow) = Vehicle(
        id = row[Vehicles.id],
        ownerId = row[Vehicles.ownerId],
        brand = row[Vehicles.brand],
        model = row[Vehicles.model],
        numberPlate = row[Vehicles.numberPlate],
        vin = row[Vehicles.vin],
        regCertificate = row[Vehicles.regCertificate],
        insuranceName = row[Vehicles.insuranceName],
        insurancePolicy = row[Vehicles.insurancePolicy],
        insuranceExpiry = row[Vehicles.insuranceExpiry],
        hasHullInsurance = row[Vehicles.hasHullInsurance]
    )

    // ── AccidentsRecords ─────────────────────────────────────────────────────

    fun getAllAccidents(): List<AccidentRecord> = transaction {
        AccidentsRecords.selectAll()
            .orderBy(AccidentsRecords.dateTime, SortOrder.DESC)
            .map { toAccident(it) }
    }

    fun insertAccident(
        officerId: Int,
        typeId: Int?,
        dateTime: LocalDateTime,
        street: String,
        house: String?,
        witnessesInfo: String?,
        circumstancesJson: String?,
        explanation: String?
    ): Int = transaction {
        AccidentsRecords.insert {
            it[AccidentsRecords.officerId] = officerId
            it[AccidentsRecords.dateTime] = dateTime
            it[AccidentsRecords.street] = street
            it[AccidentsRecords.house] = house
            it[AccidentsRecords.witnessesInfo] = witnessesInfo
            it[AccidentsRecords.circumstancesJson] = circumstancesJson
            it[AccidentsRecords.explanation] = explanation
        }[AccidentsRecords.id]
    }

    private fun toAccident(row: ResultRow): AccidentRecord {
        val explanation = row[AccidentsRecords.explanation]
        val pattern = Pattern.compile("Виновный: (.*)", Pattern.CASE_INSENSITIVE)
        val matcher = explanation?.let { pattern.matcher(it) }
        val guiltySide = if (matcher?.find() == true) matcher.group(1) else "не определён"

        return AccidentRecord(
            id = row[AccidentsRecords.id],
            officerId = row[AccidentsRecords.officerId],
            dateTime = row[AccidentsRecords.dateTime],
            street = row[AccidentsRecords.street],
            house = row[AccidentsRecords.house],
            witnessesInfo = row[AccidentsRecords.witnessesInfo],
            circumstancesJson = row[AccidentsRecords.circumstancesJson],
            explanation = explanation,
            guiltySide = guiltySide
        )
    }

    // ── AccidentParticipants ────────────────────────────────────────────────

    fun insertParticipant(
        accidentId: Int,
        driverId: Int,
        vehicleId: Int,
        role: Char,
        impactSpot: String?,
        damageDetails: String?,
        remarks: String?
    ) = transaction {
        AccidentParticipants.insert {
            it[AccidentParticipants.accidentId] = accidentId
            it[AccidentParticipants.driverId] = driverId
            it[AccidentParticipants.vehicleId] = vehicleId
            it[AccidentParticipants.role] = role
            it[AccidentParticipants.impactSpot] = impactSpot
            it[AccidentParticipants.damageDetails] = damageDetails
            it[AccidentParticipants.remarks] = remarks
        }
    }

    // ── ViolationsTypes ─────────────────────────────────────────────────────

    fun getAllViolationTypes(): List<ViolationType> = transaction {
        ViolationsTypes.selectAll().map { toViolationType(it) }
    }

    fun getOrCreateViolationType(clause: String, description: String, fineAmount: Int): Int = transaction {
        ViolationsTypes
            .selectAll()
            .where { ViolationsTypes.clause eq clause }
            .map { it[ViolationsTypes.id] }
            .firstOrNull()
            ?: ViolationsTypes.insert {
                it[ViolationsTypes.clause] = clause
                it[ViolationsTypes.description] = description
                it[ViolationsTypes.fineAmount] = fineAmount
            }[ViolationsTypes.id]
    }

    private fun toViolationType(row: ResultRow) = ViolationType(
        id = row[ViolationsTypes.id],
        clause = row[ViolationsTypes.clause],
        description = row[ViolationsTypes.description],
        fineAmount = row[ViolationsTypes.fineAmount]
    )

    // ── ViolationRecords ────────────────────────────────────────────────────

    fun getAllViolations(): List<ViolationRecord> = transaction {
        (ViolationRecords innerJoin Drivers innerJoin ViolationsTypes)
            .selectAll()
            .orderBy(ViolationRecords.dateTime, SortOrder.DESC)
            .map { toViolation(it) }
    }

    fun insertViolation(
        officerId: Int,
        driverId: Int,
        vehicleId: Int,
        typeId: Int,
        dateTime: LocalDateTime,
        street: String,
        houseNumber: String?,
        witnessVictimInfo: String?
    ): Int = transaction {
        ViolationRecords.insert {
            it[ViolationRecords.officerId] = officerId
            it[ViolationRecords.driverId] = driverId
            it[ViolationRecords.vehicleId] = vehicleId
            it[ViolationRecords.typeId] = typeId
            it[ViolationRecords.dateTime] = dateTime
            it[ViolationRecords.street] = street
            it[ViolationRecords.houseNumber] = houseNumber
            it[ViolationRecords.witnessVictimInfo] = witnessVictimInfo
        }[ViolationRecords.id]
    }

    private fun toViolation(row: ResultRow) = ViolationRecord(
        id = row[ViolationRecords.id],
        officerId = row[ViolationRecords.officerId],
        driverId = row[ViolationRecords.driverId],
        vehicleId = row[ViolationRecords.vehicleId],
        typeId = row[ViolationRecords.typeId],
        dateTime = row[ViolationRecords.dateTime],
        street = row[ViolationRecords.street],
        houseNumber = row[ViolationRecords.houseNumber],
        witnessVictimInfo = row[ViolationRecords.witnessVictimInfo],
        npaPoint = row[ViolationsTypes.clause],
        driverFullName = buildFullName(row[Drivers.lastName], row[Drivers.firstName], row[Drivers.middleName])
    )

    // ── Stats ────────────────────────────────────────────────────────────

    fun getHomeStats(): HomeStats {
        val today = java.time.LocalDate.now()
        val accidents = getAllAccidents().count { it.dateTime.toLocalDate() == today }
        val violations = getAllViolations().count { it.dateTime.toLocalDate() == today }
        return HomeStats(accidents, violations)
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun splitFullName(fullName: String): Triple<String, String, String?> {
        val parts = fullName.trim().split(Regex("\\s+"))
        val ln = parts.getOrElse(0) { "" }
        val fn = parts.getOrElse(1) { "" }
        val mn = parts.getOrNull(2)
        return Triple(ln, fn, mn)
    }

    private fun buildFullName(last: String, first: String, middle: String?): String {
        return listOfNotNull(last, first, middle?.takeIf { it.isNotBlank() }).joinToString(" ")
    }
}
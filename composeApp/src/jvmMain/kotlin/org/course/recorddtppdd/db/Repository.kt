package org.course.recorddtppdd.db

import org.course.recorddtppdd.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.regex.Pattern

/**
 * Репозиторий — операции с БД через Exposed DSL.
 * Привязан к вашей реальной схеме MySQL.
 */
object Repository {

    // ── Officers ───────────────────────────────────────────────────────────[...]

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

    // ── Drivers ───────────────────────────────────────────────────────────[...]

    fun getAllDrivers(): List<Driver> = transaction {
        Drivers.selectAll().map { toDriver(it) }
    }

    /** Очень простой поиск (ФИО + телефон). */
    fun findDriverByNameAndPhone(fullName: String, phone: String?): Driver? = transaction {
        val (ln, fn, mn) = splitFullName(fullName)

        val base = (Drivers.lastName eq ln) and (Drivers.firstName eq fn) and (Drivers.phone eq phone)

        // В некоторых версиях Exposed нет extension isNull(), поэтому используем eq null.
        val middleExpr: Op<Boolean> = if (mn == null) {
            Drivers.middleName eq null
        } else {
            Drivers.middleName eq mn
        }

        Drivers
            .selectAll()
            .where { base and middleExpr }
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

    // ── Licenses ───────────────────────────────────────────────────────────[...]

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

    private fun toLicense(row: ResultRow) = License(
        id = row[Licenses.id],
        driverId = row[Licenses.driverId],
        series = row[Licenses.series],
        number = row[Licenses.number],
        category = row[Licenses.category],
        issueDate = row[Licenses.issueDate]
    )

    // ── Vehicles ───────────────────────────────────────────────────────────[...]

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
            it[AccidentsRecords.typeId] = typeId
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
            typeId = row[AccidentsRecords.typeId],
            dateTime = row[AccidentsRecords.dateTime],
            street = row[AccidentsRecords.street],
            house = row[AccidentsRecords.house],
            witnessesInfo = row[AccidentsRecords.witnessesInfo],
            circumstancesJson = row[AccidentsRecords.circumstancesJson],
            explanation = explanation,
            guiltySide = guiltySide
        )
    }

    // ── AccidentTypes ────────────────────────────────────────────────────────

    fun getAllAccidentTypes(): List<AccidentType> = transaction {
        AccidentTypes.selectAll()
            .orderBy(AccidentTypes.id, SortOrder.ASC)
            .map { row ->
                AccidentType(
                    id = row[AccidentTypes.id],
                    name = row[AccidentTypes.name],
                    description = row[AccidentTypes.description]
                )
            }
    }

    fun findAccidentTypeIdByName(name: String): Int? = transaction {
        AccidentTypes.selectAll()
            .where { AccidentTypes.name eq name }
            .map { it[AccidentTypes.id] }
            .firstOrNull()
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

    // ── SQL Reports (подзапросы / JOIN / оконные / VIEW / триггеры / proc/fn / CTE)

    fun getSqlReportQueries(): List<SqlReportQuery> = transaction {
        val queries = listOf(
            // Подзапросы (2)
            "Подзапрос 1: ДТП с числом участников" to """
                SELECT ar.accident_id, ar.date_time,
                       (SELECT COUNT(*) FROM AccidentParticipants ap WHERE ap.accident_id = ar.accident_id) AS participants_count
                FROM AccidentsRecords ar
                ORDER BY ar.date_time DESC
                LIMIT 10
            """.trimIndent(),
            "Подзапрос 2: водители с нарушениями > среднего" to """
                SELECT d.driver_id, d.last_name, d.first_name, agg.cnt
                FROM (
                    SELECT vr.driver_id, COUNT(*) AS cnt
                    FROM ViolationRecords vr
                    GROUP BY vr.driver_id
                ) agg
                JOIN Drivers d ON d.driver_id = agg.driver_id
                WHERE agg.cnt > (SELECT AVG(x.cnt) FROM (SELECT COUNT(*) AS cnt FROM ViolationRecords GROUP BY driver_id) x)
                ORDER BY agg.cnt DESC
                LIMIT 10
            """.trimIndent(),

            // JOIN (3)
            "JOIN 1: нарушения + водитель + офицер + тип" to """
                SELECT vr.record_id, vr.date_time, vt.clause, d.last_name, d.first_name, o.login
                FROM ViolationRecords vr
                JOIN Drivers d ON d.driver_id = vr.driver_id
                JOIN Officers o ON o.officer_id = vr.officer_id
                JOIN ViolationsTypes vt ON vt.type_id = vr.type_id
                ORDER BY vr.date_time DESC
                LIMIT 10
            """.trimIndent(),
            "JOIN 2: участники ДТП" to """
                SELECT ap.accident_id, ap.role, d.last_name, d.first_name, v.number_plate
                FROM AccidentParticipants ap
                JOIN Drivers d ON d.driver_id = ap.driver_id
                JOIN Vehicles v ON v.vehicle_id = ap.vehicle_id
                ORDER BY ap.accident_id DESC
                LIMIT 10
            """.trimIndent(),
            "JOIN 3: ДТП + офицер + тип ДТП" to """
                SELECT ar.accident_id, ar.date_time, o.login, at.name
                FROM AccidentsRecords ar
                JOIN Officers o ON o.officer_id = ar.officer_id
                LEFT JOIN AccidentTypes at ON at.type_id = ar.type_id
                ORDER BY ar.date_time DESC
                LIMIT 10
            """.trimIndent(),

            // Оконные функции (4)
            "Оконная 1: ROW_NUMBER по ДТП" to """
                SELECT accident_id, date_time,
                       ROW_NUMBER() OVER (ORDER BY date_time DESC) AS rn
                FROM AccidentsRecords
                LIMIT 10
            """.trimIndent(),
            "Оконная 2: RANK офицеров по ДТП" to """
                SELECT officer_id, total_accidents,
                       RANK() OVER (ORDER BY total_accidents DESC) AS rnk
                FROM (
                    SELECT officer_id, COUNT(*) AS total_accidents
                    FROM AccidentsRecords
                    GROUP BY officer_id
                ) s
                LIMIT 10
            """.trimIndent(),
            "Оконная 3: накопительный итог нарушений" to """
                SELECT day, cnt,
                       SUM(cnt) OVER (ORDER BY day) AS cumulative_cnt
                FROM (
                    SELECT DATE(date_time) AS day, COUNT(*) AS cnt
                    FROM ViolationRecords
                    GROUP BY DATE(date_time)
                ) s
                ORDER BY day DESC
                LIMIT 10
            """.trimIndent(),
            "Оконная 4: LAG по ДТП" to """
                SELECT day, cnt,
                       LAG(cnt, 1, 0) OVER (ORDER BY day) AS prev_cnt
                FROM (
                    SELECT DATE(date_time) AS day, COUNT(*) AS cnt
                    FROM AccidentsRecords
                    GROUP BY DATE(date_time)
                ) s
                ORDER BY day DESC
                LIMIT 10
            """.trimIndent(),

            // VIEW (2)
            "VIEW 1: v_accidents_summary" to """
                SELECT * FROM v_accidents_summary
                ORDER BY date_time DESC
                LIMIT 10
            """.trimIndent(),
            "VIEW 2: v_violations_summary" to """
                SELECT * FROM v_violations_summary
                ORDER BY date_time DESC
                LIMIT 10
            """.trimIndent(),

            // Триггеры (3)
            "Триггер 1: наличие trg_after_accident_insert" to """
                SELECT TRIGGER_NAME, EVENT_MANIPULATION, ACTION_TIMING
                FROM information_schema.TRIGGERS
                WHERE TRIGGER_SCHEMA = DATABASE() AND TRIGGER_NAME = 'trg_after_accident_insert'
            """.trimIndent(),
            "Триггер 2: наличие trg_after_violation_insert" to """
                SELECT TRIGGER_NAME, EVENT_MANIPULATION, ACTION_TIMING
                FROM information_schema.TRIGGERS
                WHERE TRIGGER_SCHEMA = DATABASE() AND TRIGGER_NAME = 'trg_after_violation_insert'
            """.trimIndent(),
            "Триггер 3: наличие trg_before_violation_insert" to """
                SELECT TRIGGER_NAME, EVENT_MANIPULATION, ACTION_TIMING
                FROM information_schema.TRIGGERS
                WHERE TRIGGER_SCHEMA = DATABASE() AND TRIGGER_NAME = 'trg_before_violation_insert'
            """.trimIndent(),

            // Хранимые процедуры / функции (3)
            "Процедура 1: sp_daily_stats" to "CALL sp_daily_stats(CURDATE())",
            "Процедура 2: sp_accident_participants" to "CALL sp_accident_participants(1)",
            "Функция 3: fn_driver_violation_count" to """
                SELECT driver_id, fn_driver_violation_count(driver_id) AS violations_total
                FROM Drivers
                ORDER BY violations_total DESC
                LIMIT 10
            """.trimIndent(),

            // CTE (3)
            "CTE 1: ранжирование офицеров по ДТП" to """
                WITH officer_accident_stats AS (
                    SELECT officer_id, COUNT(*) AS total_accidents
                    FROM AccidentsRecords
                    GROUP BY officer_id
                )
                SELECT officer_id, total_accidents
                FROM officer_accident_stats
                ORDER BY total_accidents DESC
                LIMIT 10
            """.trimIndent(),
            "CTE 2: рецидивисты по нарушениям" to """
                WITH driver_violation_counts AS (
                    SELECT driver_id, COUNT(*) AS violation_count
                    FROM ViolationRecords
                    GROUP BY driver_id
                )
                SELECT d.driver_id, d.last_name, d.first_name, dvc.violation_count
                FROM driver_violation_counts dvc
                JOIN Drivers d ON d.driver_id = dvc.driver_id
                WHERE dvc.violation_count > 1
                ORDER BY dvc.violation_count DESC
                LIMIT 10
            """.trimIndent(),
            "CTE 3: 7-дневная скользящая сумма ДТП" to """
                WITH daily_accidents AS (
                    SELECT DATE(date_time) AS day, COUNT(*) AS cnt
                    FROM AccidentsRecords
                    GROUP BY DATE(date_time)
                )
                SELECT day, cnt,
                       SUM(cnt) OVER (ORDER BY day ROWS BETWEEN 6 PRECEDING AND CURRENT ROW) AS rolling_7d_sum
                FROM daily_accidents
                ORDER BY day DESC
                LIMIT 10
            """.trimIndent()
        )

        queries.map { (title, sql) ->
            SqlReportQuery(
                title = title,
                sql = sql,
                rows = execSqlReportQuery(sql)
            )
        }
    }

    private fun execSqlReportQuery(sql: String): List<String> {
        val rows = mutableListOf<String>()
        try {
            TransactionManager.current().exec(sql) { rs ->
                val metadata = rs.metaData
                val columns = metadata.columnCount
                var rowCount = 0
                while (rs.next() && rowCount < 20) {
                    val row = (1..columns).joinToString(" | ") { i ->
                        "${metadata.getColumnLabel(i)}=${rs.getString(i)}"
                    }
                    rows += row
                    rowCount++
                }
            }
            if (rows.isEmpty()) rows += "Нет данных"
        } catch (e: Exception) {
            rows += "Ошибка: ${e.message}"
        }
        return rows
    }

    // ── Stats ────────────────────────────────────────────────────────────[...]

    fun getHomeStats(): HomeStats {
        val today = java.time.LocalDate.now()
        val accidents = getAllAccidents().count { it.dateTime.toLocalDate() == today }
        val violations = getAllViolations().count { it.dateTime.toLocalDate() == today }
        return HomeStats(accidents, violations)
    }

    // ── Helpers ───────────────────────────────────────────────────────────[...]

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

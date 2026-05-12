package org.course.recorddtppdd.db

import org.course.recorddtppdd.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Date
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.regex.Pattern

/**
 * Репозиторий — операции с БД через Exposed DSL + raw SQL для продвинутых отчетов.
 */
object Repository {

    private var sqlArtifactsEnsured = false

    // ── Officers ───────────────────────────────────────────────────────────────

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

    // ── Drivers ────────────────────────────────────────────────────────────────

    fun getAllDrivers(): List<Driver> = transaction {
        Drivers.selectAll().map { toDriver(it) }
    }

    fun findDriverByNameAndPhone(fullName: String, phone: String?): Driver? = transaction {
        val (ln, fn, mn) = splitFullName(fullName)
        val base = (Drivers.lastName eq ln) and (Drivers.firstName eq fn) and (Drivers.phone eq phone)

        val middleExpr: Op<Boolean> = if (mn == null) {
            Drivers.middleName eq null
        } else {
            Drivers.middleName eq mn
        }

        Drivers.selectAll()
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

    // ── Licenses ───────────────────────────────────────────────────────────────

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

    // ── Vehicles ───────────────────────────────────────────────────────────────

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

    // ── AccidentTypes ──────────────────────────────────────────────────────────

    fun getAllAccidentTypes(): List<AccidentType> = transaction {
        AccidentTypes.selectAll()
            .orderBy(AccidentTypes.id, SortOrder.ASC)
            .map {
                AccidentType(
                    id = it[AccidentTypes.id],
                    name = it[AccidentTypes.name],
                    description = it[AccidentTypes.description]
                )
            }
    }

    // ── AccidentsRecords ───────────────────────────────────────────────────────

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

    // ── AccidentParticipants ──────────────────────────────────────────────────

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

    // ── ViolationsTypes ───────────────────────────────────────────────────────

    fun getAllViolationTypes(): List<ViolationType> = transaction {
        ViolationsTypes.selectAll()
            .orderBy(ViolationsTypes.clause, SortOrder.ASC)
            .map { toViolationType(it) }
    }

    fun findViolationTypeById(id: Int): ViolationType? = transaction {
        ViolationsTypes.selectAll().where { ViolationsTypes.id eq id }
            .map { toViolationType(it) }
            .firstOrNull()
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

    // ── ViolationRecords ──────────────────────────────────────────────────────

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

    // ── Stats + SQL analytics ────────────────────────────────────────────────

    fun getHomeStats(): HomeStats {
        val today = LocalDate.now()
        val accidents = getAllAccidents().count { it.dateTime.toLocalDate() == today }
        val violations = getAllViolations().count { it.dateTime.toLocalDate() == today }
        return HomeStats(accidents, violations)
    }

    fun ensureSqlArtifacts() {
        if (sqlArtifactsEnsured) return
        transaction {
            val statements = listOf(
                """
                CREATE TABLE IF NOT EXISTS AuditLog (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    entity VARCHAR(64) NOT NULL,
                    entity_id INT NOT NULL,
                    action VARCHAR(32) NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """.trimIndent(),
                """
                CREATE VIEW IF NOT EXISTS v_accidents_summary AS
                SELECT
                    a.accident_id,
                    a.date_time,
                    a.street,
                    a.house,
                    o.login AS officer_login,
                    (SELECT COUNT(*) FROM AccidentParticipants ap WHERE ap.accident_id = a.accident_id) AS participants_count
                FROM AccidentsRecords a
                JOIN Officers o ON o.officer_id = a.officer_id
                """.trimIndent(),
                """
                CREATE VIEW IF NOT EXISTS v_violations_summary AS
                SELECT
                    vr.record_id,
                    vr.date_time,
                    vr.street,
                    vr.house_number,
                    CONCAT(d.last_name, ' ', d.first_name) AS driver_name,
                    vt.clause,
                    vt.description,
                    o.login AS officer_login
                FROM ViolationRecords vr
                JOIN Drivers d ON d.driver_id = vr.driver_id
                JOIN ViolationsTypes vt ON vt.type_id = vr.type_id
                JOIN Officers o ON o.officer_id = vr.officer_id
                """.trimIndent(),
                "DROP TRIGGER IF EXISTS trg_accident_before_insert",
                """
                CREATE TRIGGER trg_accident_before_insert
                BEFORE INSERT ON AccidentsRecords
                FOR EACH ROW
                BEGIN
                    SET NEW.street = TRIM(NEW.street);
                END
                """.trimIndent(),
                "DROP TRIGGER IF EXISTS trg_accident_after_insert",
                """
                CREATE TRIGGER trg_accident_after_insert
                AFTER INSERT ON AccidentsRecords
                FOR EACH ROW
                BEGIN
                    INSERT INTO AuditLog(entity, entity_id, action)
                    VALUES ('AccidentsRecords', NEW.accident_id, 'INSERT');
                END
                """.trimIndent(),
                "DROP TRIGGER IF EXISTS trg_violation_after_insert",
                """
                CREATE TRIGGER trg_violation_after_insert
                AFTER INSERT ON ViolationRecords
                FOR EACH ROW
                BEGIN
                    INSERT INTO AuditLog(entity, entity_id, action)
                    VALUES ('ViolationRecords', NEW.record_id, 'INSERT');
                END
                """.trimIndent(),
                "DROP PROCEDURE IF EXISTS sp_daily_stats",
                """
                CREATE PROCEDURE sp_daily_stats(IN p_date DATE)
                BEGIN
                    SELECT
                        (SELECT COUNT(*) FROM AccidentsRecords a WHERE DATE(a.date_time) = p_date) AS accidents_count,
                        (SELECT COUNT(*) FROM ViolationRecords v WHERE DATE(v.date_time) = p_date) AS violations_count;
                END
                """.trimIndent(),
                "DROP PROCEDURE IF EXISTS sp_accident_participants",
                """
                CREATE PROCEDURE sp_accident_participants(IN p_accident_id INT)
                BEGIN
                    SELECT ap.role, d.last_name, d.first_name, v.number_plate
                    FROM AccidentParticipants ap
                    JOIN Drivers d ON d.driver_id = ap.driver_id
                    JOIN Vehicles v ON v.vehicle_id = ap.vehicle_id
                    WHERE ap.accident_id = p_accident_id;
                END
                """.trimIndent(),
                "DROP FUNCTION IF EXISTS fn_driver_violation_count",
                """
                CREATE FUNCTION fn_driver_violation_count(p_driver_id INT)
                RETURNS INT
                DETERMINISTIC
                BEGIN
                    DECLARE v_count INT;
                    SELECT COUNT(*) INTO v_count
                    FROM ViolationRecords
                    WHERE driver_id = p_driver_id;
                    RETURN v_count;
                END
                """.trimIndent()
            )

            statements.forEach { sql ->
                runCatching { execRaw(sql) }
            }
        }
        sqlArtifactsEnsured = true
    }

    fun getAnalyticsRows(): List<AnalyticsRow> {
        ensureSqlArtifacts()
        return transaction {
            val result = mutableListOf<AnalyticsRow>()

            execQuery(
                """
                WITH ranked AS (
                    SELECT officer_login, participants_count, date_time
                    FROM v_accidents_summary
                )
                SELECT
                    officer_login,
                    participants_count,
                    ROW_NUMBER() OVER (ORDER BY participants_count DESC, date_time DESC) AS rn
                FROM ranked
                ORDER BY rn
                LIMIT 5
                """.trimIndent()
            ) { rs ->
                result += AnalyticsRow(
                    category = "CTE/Window: ДТП по участникам",
                    value1 = rs.getString("officer_login") ?: "-",
                    value2 = (rs.getInt("participants_count")).toString(),
                    value3 = "#${rs.getInt("rn")}"
                )
            }

            execQuery(
                """
                WITH daily AS (
                    SELECT DATE(date_time) AS d, COUNT(*) AS c
                    FROM AccidentsRecords
                    GROUP BY DATE(date_time)
                )
                SELECT
                    d,
                    c,
                    SUM(c) OVER (ORDER BY d) AS running_total,
                    RANK() OVER (ORDER BY c DESC) AS rk
                FROM daily
                ORDER BY d DESC
                LIMIT 5
                """.trimIndent()
            ) { rs ->
                result += AnalyticsRow(
                    category = "CTE/Window: динамика ДТП",
                    value1 = rs.getString("d") ?: "-",
                    value2 = rs.getInt("c").toString(),
                    value3 = "Σ${rs.getInt("running_total")} / R${rs.getInt("rk")}"
                )
            }

            execQuery(
                """
                WITH per_day AS (
                    SELECT DATE(date_time) AS d, COUNT(*) AS c
                    FROM ViolationRecords
                    GROUP BY DATE(date_time)
                )
                SELECT
                    d,
                    c,
                    LAG(c, 1, 0) OVER (ORDER BY d) AS prev_c
                FROM per_day
                ORDER BY d DESC
                LIMIT 5
                """.trimIndent()
            ) { rs ->
                result += AnalyticsRow(
                    category = "CTE/Window: динамика ПДД",
                    value1 = rs.getString("d") ?: "-",
                    value2 = rs.getInt("c").toString(),
                    value3 = "prev=${rs.getInt("prev_c")}"
                )
            }

            execCallable(
                sql = "{ call sp_daily_stats(?) }",
                binder = { it.setDate(1, Date.valueOf(LocalDate.now())) }
            ) { rs ->
                result += AnalyticsRow(
                    category = "Procedure: stats",
                    value1 = "today",
                    value2 = "ДТП=${rs.getInt("accidents_count")}",
                    value3 = "ПДД=${rs.getInt("violations_count")}"
                )
            }

            execQuery(
                """
                SELECT d.driver_id, CONCAT(d.last_name, ' ', d.first_name) AS driver_name,
                       fn_driver_violation_count(d.driver_id) AS v_count
                FROM Drivers d
                WHERE d.driver_id IN (
                    SELECT vr.driver_id
                    FROM ViolationRecords vr
                    GROUP BY vr.driver_id
                    HAVING COUNT(*) > 0
                )
                ORDER BY v_count DESC
                LIMIT 5
                """.trimIndent()
            ) { rs ->
                result += AnalyticsRow(
                    category = "Function/Subquery: водитель",
                    value1 = rs.getString("driver_name") ?: "-",
                    value2 = rs.getInt("v_count").toString(),
                    value3 = "id=${rs.getInt("driver_id")}"
                )
            }

            if (result.isEmpty()) {
                result += AnalyticsRow("SQL", "Нет данных", "-", "-")
            }
            result
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun splitFullName(fullName: String): Triple<String, String, String?> {
        val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val ln = parts.getOrElse(0) { "" }
        val fn = parts.getOrElse(1) { "" }
        val mn = parts.getOrNull(2)
        return Triple(ln, fn, mn)
    }

    private fun buildFullName(last: String, first: String, middle: String?): String {
        return listOfNotNull(last, first, middle?.takeIf { it.isNotBlank() }).joinToString(" ")
    }

    private fun execRaw(sql: String) {
        val conn = TransactionManager.current().connection.jdbcConnection
        conn.createStatement().use { st -> st.execute(sql) }
    }

    private fun execQuery(sql: String, row: (java.sql.ResultSet) -> Unit) {
        val conn = TransactionManager.current().connection.jdbcConnection
        conn.prepareStatement(sql).use { ps ->
            ps.executeQuery().use { rs ->
                while (rs.next()) row(rs)
            }
        }
    }

    private fun execCallable(
        sql: String,
        binder: (java.sql.CallableStatement) -> Unit,
        row: (java.sql.ResultSet) -> Unit
    ) {
        val conn = TransactionManager.current().connection.jdbcConnection
        conn.prepareCall(sql).use { cs ->
            binder(cs)
            if (cs.execute()) {
                cs.resultSet?.use { rs ->
                    while (rs.next()) row(rs)
                }
            }
        }
    }
}

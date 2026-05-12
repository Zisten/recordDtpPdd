package org.course.recorddtppdd.db

import org.course.recorddtppdd.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.regex.Pattern
import org.jetbrains.exposed.sql.statements.StatementType
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

    // ... остальной код Repository.kt (оставьте без изменений) ...

    // ── SQL Analytics ─────────────────────────────────────────────────────

    // ── SQL Analytics ─────────────────────────────────────────────────────

    fun getAnalyticsReports(): List<AnalyticsReport> = transaction {
        val reports = mutableListOf<AnalyticsReport>()

        fun execQuery(cat: String, name: String, cols: List<String>, sql: String) {
            val rows = mutableListOf<List<String>>()
            TransactionManager.current().exec(
                stmt = sql,
                args = emptyList(),
                explicitStatementType = StatementType.SELECT
            ) { rs ->
                while (rs.next()) {
                    val row = mutableListOf<String>()
                    for (i in 1..cols.size) {
                        row.add(rs.getString(i) ?: "-")
                    }
                    rows.add(row)
                }
            }
            reports.add(AnalyticsReport(cat, name, cols, rows))
        }

        // --- Категория: Финансовый контроль ---
        execQuery(
            "Финансовый контроль", "Водители с превышением среднего показателя штрафов",
            listOf("Фамилия", "Имя", "Сумма штрафов (руб.)"),
            """
            SELECT d.last_name, d.first_name, SUM(vt.fine_amount) as total
            FROM Drivers d
            JOIN ViolationRecords vr ON d.driver_id = vr.driver_id
            JOIN ViolationsTypes vt ON vr.type_id = vt.type_id
            GROUP BY d.driver_id
            HAVING SUM(vt.fine_amount) > (
                SELECT AVG(driver_total) FROM (
                    SELECT SUM(vt2.fine_amount) as driver_total
                    FROM ViolationRecords vr2
                    JOIN ViolationsTypes vt2 ON vr2.type_id = vt2.type_id
                    GROUP BY vr2.driver_id
                ) as avg_tbl
            )
            """
        )

        execQuery(
            "Финансовый контроль", "Классификация выписанных штрафов по размеру",
            listOf("Категория тяжести", "Количество постановлений"),
            """
            WITH FineCategories AS (
                SELECT CASE 
                    WHEN vt.fine_amount < 1000 THEN 'Мелкий'
                    WHEN vt.fine_amount <= 5000 THEN 'Средний'
                    ELSE 'Крупный' END as category
                FROM ViolationRecords vr JOIN ViolationsTypes vt ON vr.type_id = vt.type_id
            )
            SELECT category, COUNT(*) as count FROM FineCategories GROUP BY category
            """
        )

        execQuery(
            "Финансовый контроль", "Рейтинг злостных нарушителей (по сумме штрафов)",
            listOf("Водитель", "Общая сумма штрафов", "Ранг"),
            """
            SELECT d.last_name, SUM(vt.fine_amount) as total_fine,
                   DENSE_RANK() OVER (ORDER BY SUM(vt.fine_amount) DESC) as rnk
            FROM Drivers d
            JOIN ViolationRecords vr ON d.driver_id = vr.driver_id
            JOIN ViolationsTypes vt ON vr.type_id = vt.type_id
            GROUP BY d.driver_id, d.last_name
            """
        )

        execQuery(
            "Финансовый контроль", "Динамика начисления штрафов (накопительный итог)",
            listOf("Дата", "Сумма за день", "Накопительный итог"),
            """
            WITH DailySums AS (
                SELECT DATE(vr.date_time) as dt, SUM(vt.fine_amount) as daily_sum
                FROM ViolationRecords vr JOIN ViolationsTypes vt ON vr.type_id = vt.type_id
                GROUP BY DATE(vr.date_time)
            )
            SELECT dt, daily_sum,
                   SUM(daily_sum) OVER (ORDER BY dt) as cumulative_sum
            FROM DailySums
            """
        )

        // --- Категория: Статистика ДТП ---
        execQuery(
            "Статистика ДТП", "Анализ аварийности по месяцам",
            listOf("Месяц", "Зафиксировано ДТП"),
            """
            WITH MonthlyStats AS (
                SELECT DATE_FORMAT(date_time, '%Y-%m') as month, COUNT(*) as cnt
                FROM AccidentsRecords
                GROUP BY month
            )
            SELECT * FROM MonthlyStats ORDER BY month DESC
            """
        )

        execQuery(
            "Статистика ДТП", "Регулярные участники ДТП (Транспортные средства)",
            listOf("Марка", "Модель", "Госномер"),
            """
            SELECT brand, model, number_plate FROM Vehicles 
            WHERE vehicle_id IN (
                SELECT vehicle_id FROM AccidentParticipants GROUP BY vehicle_id HAVING COUNT(*) > 1
            )
            """
        )

        execQuery(
            "Статистика ДТП", "Журнал участников недавних ДТП",
            listOf("Дата ДТП", "Улица", "Водитель", "Роль в ДТП"),
            """
            SELECT DATE(a.date_time), a.street, d.last_name, p.role
            FROM AccidentParticipants p
            JOIN AccidentsRecords a ON p.accident_id = a.accident_id
            JOIN Drivers d ON p.driver_id = d.driver_id
            ORDER BY a.date_time DESC LIMIT 20
            """
        )

        execQuery(
            "Статистика ДТП", "Расширенная сводка по ДТП (Представление)",
            listOf("ID ДТП", "Дата", "Улица", "Оформляющий инспектор", "Участников"),
            "SELECT * FROM View_AccidentFullDetails ORDER BY date_time DESC LIMIT 20"
        )

        // --- Категория: Анализ нарушений ПДД ---
        execQuery(
            "Анализ нарушений ПДД", "Рейтинг наиболее частых нарушений",
            listOf("Пункт НПА", "Краткое описание", "Зафиксировано раз"),
            """
            WITH ViolationCounts AS (
                SELECT type_id, COUNT(*) as cnt FROM ViolationRecords GROUP BY type_id
            )
            SELECT vt.clause, SUBSTRING(vt.description, 1, 30), vc.cnt
            FROM ViolationCounts vc JOIN ViolationsTypes vt ON vc.type_id = vt.type_id
            ORDER BY vc.cnt DESC LIMIT 3
            """
        )

        execQuery(
            "Анализ нарушений ПДД", "Детализированный реестр последних нарушений",
            listOf("Дата", "Пункт НПА", "Фамилия нарушителя", "Госномер ТС"),
            """
            SELECT DATE(vr.date_time), vt.clause, d.last_name, v.number_plate
            FROM ViolationRecords vr
            JOIN ViolationsTypes vt ON vr.type_id = vt.type_id
            JOIN Drivers d ON vr.driver_id = d.driver_id
            JOIN Vehicles v ON vr.vehicle_id = v.vehicle_id
            ORDER BY vr.date_time DESC
            LIMIT 20
            """
        )

        execQuery(
            "Анализ нарушений ПДД", "Интервалы между нарушениями (рецидивы)",
            listOf("Водитель", "Дата нарушения", "Дней с предыдущего"),
            """
            SELECT d.last_name, DATE(vr.date_time),
                   COALESCE(DATEDIFF(vr.date_time, LAG(vr.date_time) OVER (PARTITION BY vr.driver_id ORDER BY vr.date_time)), 0) as days_since_last
            FROM ViolationRecords vr JOIN Drivers d ON vr.driver_id = d.driver_id
            """
        )

        // --- Категория: Показатели сотрудников ---
        execQuery(
            "Показатели сотрудников", "Сводка по оформленным материалам (ДТП)",
            listOf("Инспектор (Фамилия)", "Оформлено ДТП"),
            """
            SELECT o.last_name, COUNT(a.accident_id) as accidents_count
            FROM Officers o
            LEFT JOIN AccidentsRecords a ON o.officer_id = a.officer_id
            GROUP BY o.officer_id, o.last_name
            """
        )

        // --- Категория: Профилирование водителей ---
        execQuery(
            "Профилирование водителей", "Профили риска водителей (Сводный отчет)",
            listOf("ID", "ФИО Водителя", "Всего нарушений", "Сумма долга"),
            "SELECT * FROM View_DriverRiskProfile ORDER BY total_fines DESC LIMIT 20"
        )

        execQuery(
            "Профилирование водителей", "Хронология нарушений по водителям",
            listOf("Водитель", "Дата нарушения", "Порядковый номер нарушения"),
            """
            SELECT d.last_name, DATE(vr.date_time),
                   ROW_NUMBER() OVER (PARTITION BY vr.driver_id ORDER BY vr.date_time) as incident_num
            FROM ViolationRecords vr JOIN Drivers d ON vr.driver_id = d.driver_id
            """
        )

        reports
    }
}

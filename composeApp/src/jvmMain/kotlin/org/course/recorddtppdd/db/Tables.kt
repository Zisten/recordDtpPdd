package org.course.recorddtppdd.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * Таблица сотрудников (Officers) — для авторизации.
 */
object Officers : Table("Officers") {
    val id = integer("id").autoIncrement()
    val login = varchar("login", 100)
    val password = varchar("password", 255)
    override val primaryKey = PrimaryKey(id)
}

/**
 * Водители (Drivers).
 */
object Drivers : Table("Drivers") {
    val id = integer("id").autoIncrement()
    val fullName = varchar("full_name", 255)
    val birthdate = varchar("birthdate", 20)
    val address = varchar("address", 500)
    val phone = varchar("phone", 50)
    override val primaryKey = PrimaryKey(id)
}

/**
 * Водительские удостоверения (Licenses).
 */
object Licenses : Table("Licenses") {
    val id = integer("id").autoIncrement()
    val driverId = integer("driver_id").references(Drivers.id)
    val series = varchar("series", 20)
    val number = varchar("number", 20)
    val category = varchar("category", 50)
    val issueDate = varchar("issue_date", 20)
    override val primaryKey = PrimaryKey(id)
}

/**
 * Транспортные средства (Vehicles).
 */
object Vehicles : Table("Vehicles") {
    val id = integer("id").autoIncrement()
    val make = varchar("make", 100)
    val model = varchar("model", 100)
    val vin = varchar("vin", 50)
    val numberPlate = varchar("number_plate", 30)
    val sts = varchar("sts", 50)
    val ownerName = varchar("owner_name", 255)
    val ownerAddress = varchar("owner_address", 500)
    override val primaryKey = PrimaryKey(id)
}

/**
 * Протоколы о ДТП (AccidentsRecords).
 */
object AccidentsRecords : Table("AccidentsRecords") {
    val id = integer("id").autoIncrement()
    val locationStreet = varchar("location_street", 255)
    val locationBuilding = varchar("location_building", 50)
    val datetime = datetime("datetime")
    val witnessesName = varchar("witnesses_name", 255).default("")
    val witnessesAddress = varchar("witnesses_address", 500).default("")
    val description = text("description")
    /** JSON-массив обстоятельств ДТП, хранится как текст */
    val circumstances = text("circumstances").default("[]")
    val guiltySide = varchar("guilty_side", 20).default("не определён")
    val officerId = integer("officer_id").references(Officers.id)
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}

/**
 * Участники ДТП (AccidentParticipants).
 */
object AccidentParticipants : Table("AccidentParticipants") {
    val id = integer("id").autoIncrement()
    val accidentId = integer("accident_id").references(AccidentsRecords.id)
    val side = varchar("side", 5)           // "A" или "B"
    val driverId = integer("driver_id").references(Drivers.id)
    val vehicleId = integer("vehicle_id").references(Vehicles.id)
    val damages = text("damages").default("")
    val notes = text("notes").default("")
    val licenseId = integer("license_id").references(Licenses.id).nullable()
    val insuranceCompany = varchar("insurance_company", 255).default("")
    val insurancePolicy = varchar("insurance_policy", 100).default("")
    val insuranceExpiry = varchar("insurance_expiry", 20).default("")
    val hasHull = bool("has_hull").default(false)
    override val primaryKey = PrimaryKey(id)
}

/**
 * Виды нарушений ПДД (ViolationsTypes).
 */
object ViolationsTypes : Table("ViolationsTypes") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val description = text("description").default("")
    override val primaryKey = PrimaryKey(id)
}

/**
 * Протоколы о нарушениях ПДД (ViolationRecords).
 */
object ViolationRecords : Table("ViolationRecords") {
    val id = integer("id").autoIncrement()
    val driverId = integer("driver_id").references(Drivers.id)
    val vehicleId = integer("vehicle_id").references(Vehicles.id).nullable()
    val licenseId = integer("license_id").references(Licenses.id).nullable()
    val violationTypeId = integer("violation_type_id").references(ViolationsTypes.id).nullable()
    val npaPoint = varchar("npa_point", 100).default("")
    val description = text("description").default("")
    val witnessName = varchar("witness_name", 255).default("")
    val witnessAddress = varchar("witness_address", 500).default("")
    val witnessPhone = varchar("witness_phone", 50).default("")
    val datetime = datetime("datetime")
    val officerId = integer("officer_id").references(Officers.id)
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}

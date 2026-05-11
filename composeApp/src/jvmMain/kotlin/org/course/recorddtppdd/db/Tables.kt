package org.course.recorddtppdd.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * Таблицы Exposed, приведённые к РЕАЛЬНОЙ схеме MySQL из вашего SQL.
 *
 * ВАЖНО: имена колонок должны 1-в-1 совпадать с тем, что создано в MySQL,
 * иначе будут ошибки вида "Unknown column ...".
 */

/** Officers(officer_id, ..., login, password) */
object Officers : Table("Officers") {
    val id = integer("officer_id").autoIncrement()
    val login = varchar("login", 255)
    val password = varchar("password", 255)

    override val primaryKey = PrimaryKey(id)
}

/** Drivers(driver_id, last_name, first_name, middle_name, birth_date, ..., phone, ...) */
object Drivers : Table("Drivers") {
    val id = integer("driver_id").autoIncrement()

    val lastName = varchar("last_name", 255)
    val firstName = varchar("first_name", 255)
    val middleName = varchar("middle_name", 255).nullable()

    // В вашей БД это DATE
    val birthDate = date("birth_date").nullable()

    val registrationAddress = text("registration_address").nullable()
    val actualAddress = text("actual_address").nullable()
    val phone = varchar("phone", 50).nullable()

    override val primaryKey = PrimaryKey(id)
}

/** Licenses(license_id, driver_id, series, number, category, issue_date, ...) */
object Licenses : Table("Licenses") {
    val id = integer("license_id").autoIncrement()
    val driverId = integer("driver_id").references(Drivers.id)
    val series = varchar("series", 50)
    val number = varchar("number", 50)
    val category = varchar("category", 50).nullable()
    val issueDate = date("issue_date")

    override val primaryKey = PrimaryKey(id)
}

/** Vehicles(vehicle_id, owner_id, brand, model, number_plate, vin, reg_certificate, insurance_..., has_hull_insurance) */
object Vehicles : Table("Vehicles") {
    val id = integer("vehicle_id").autoIncrement()
    val ownerId = integer("owner_id").references(Drivers.id).nullable()

    val brand = varchar("brand", 255)
    val model = varchar("model", 255)
    val numberPlate = varchar("number_plate", 20)
    val vin = varchar("vin", 17).nullable()

    val regCertificate = varchar("reg_certificate", 255).nullable()

    val insuranceName = varchar("insurance_name", 255).nullable()
    val insurancePolicy = varchar("insurance_policy", 255).nullable()
    val insuranceExpiry = date("insurance_expiry").nullable()
    val hasHullInsurance = bool("has_hull_insurance").nullable()

    override val primaryKey = PrimaryKey(id)
}

/** AccidentsRecords(accident_id, officer_id, type_id, date_time, street, house, witnesses_info, circumstances_json, explanation) */
object AccidentsRecords : Table("AccidentsRecords") {
    val id = integer("accident_id").autoIncrement()

    val officerId = integer("officer_id").references(Officers.id)
    val typeId = integer("type_id").references(AccidentTypes.id).nullable()

    val dateTime = datetime("date_time")
    val street = varchar("street", 255)
    val house = varchar("house", 255).nullable()

    val witnessesInfo = text("witnesses_info").nullable()

    /**
     * В MySQL это JSON. Чтобы не тащить дополнительные мапперы/модули, храним как TEXT.
     * (В Exposed нет универсального json<T>() без доп. модулей и версий.)
     */
    val circumstancesJson = text("circumstances_json").nullable()

    val explanation = text("explanation").nullable()

    override val primaryKey = PrimaryKey(id)
}

/** AccidentTypes(type_id, name, description) */
object AccidentTypes : Table("AccidentTypes") {
    val id = integer("type_id").autoIncrement()
    val name = varchar("name", 255)
    val description = text("description").nullable()

    override val primaryKey = PrimaryKey(id)
}

/** AccidentParticipants(participant_id, accident_id, driver_id, vehicle_id, role, impact_spot, damage_details, remarks) */
object AccidentParticipants : Table("AccidentParticipants") {
    val id = integer("participant_id").autoIncrement()

    val accidentId = integer("accident_id").references(AccidentsRecords.id)
    val driverId = integer("driver_id").references(Drivers.id)
    val vehicleId = integer("vehicle_id").references(Vehicles.id)

    val role = char("role") // 'A' / 'B'

    val impactSpot = text("impact_spot").nullable()
    val damageDetails = text("damage_details").nullable()
    val remarks = text("remarks").nullable()

    override val primaryKey = PrimaryKey(id)
}

/** ViolationsTypes(type_id, clause, description, fine_amount) */
object ViolationsTypes : Table("ViolationsTypes") {
    val id = integer("type_id").autoIncrement()

    val clause = varchar("clause", 255)
    val description = text("description")
    val fineAmount = integer("fine_amount")

    override val primaryKey = PrimaryKey(id)
}

/** ViolationRecords(record_id, officer_id, driver_id, vehicle_id, type_id, date_time, street, house_number, witness_victim_info) */
object ViolationRecords : Table("ViolationRecords") {
    val id = integer("record_id").autoIncrement()

    val officerId = integer("officer_id").references(Officers.id)
    val driverId = integer("driver_id").references(Drivers.id)
    val vehicleId = integer("vehicle_id").references(Vehicles.id)
    val typeId = integer("type_id").references(ViolationsTypes.id)

    val dateTime = datetime("date_time")
    val street = varchar("street", 255)
    val houseNumber = varchar("house_number", 50).nullable()

    val witnessVictimInfo = text("witness_victim_info").nullable()

    override val primaryKey = PrimaryKey(id)
}

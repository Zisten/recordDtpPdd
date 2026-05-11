package org.course.recorddtppdd.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Officer(
    val id: Int,
    val login: String,
    val password: String
)

/**
 * Упрощённая модель водителя для UI.
 * В БД ФИО разнесено на last/first/middle.
 */
data class Driver(
    val id: Int,
    val fullName: String,
    val birthDate: LocalDate?,
    val registrationAddress: String?,
    val actualAddress: String?,
    val phone: String?
)

data class License(
    val id: Int,
    val driverId: Int,
    val series: String,
    val number: String,
    val category: String?,
    val issueDate: LocalDate
)

/**
 * Упрощённая модель ТС для UI.
 */
data class Vehicle(
    val id: Int,
    val ownerId: Int?,
    val brand: String,
    val model: String,
    val numberPlate: String,
    val vin: String?,
    val regCertificate: String?,
    val insuranceName: String?,
    val insurancePolicy: String?,
    val insuranceExpiry: LocalDate?,
    val hasHullInsurance: Boolean?
)

data class AccidentRecord(
    val id: Int,
    val officerId: Int,
    val typeId: Int?,
    val dateTime: LocalDateTime,
    val street: String,
    val house: String?,
    val witnessesInfo: String?,
    val circumstancesJson: String?,
    val explanation: String?,
    val guiltySide: String
)

data class AccidentParticipant(
    val id: Int,
    val accidentId: Int,
    val driverId: Int,
    val vehicleId: Int,
    val role: Char,
    val impactSpot: String?,
    val damageDetails: String?,
    val remarks: String?
)

data class AccidentType(
    val id: Int,
    val name: String,
    val description: String?
)

data class ViolationType(
    val id: Int,
    val clause: String,
    val description: String,
    val fineAmount: Int
)

data class ViolationRecord(
    val id: Int,
    val officerId: Int,
    val driverId: Int,
    val vehicleId: Int,
    val typeId: Int,
    val dateTime: LocalDateTime,
    val street: String,
    val houseNumber: String?,
    val witnessVictimInfo: String?,
    val npaPoint: String,
    val driverFullName: String
)

/** Сводные данные для главного экрана */
data class HomeStats(
    val accidentsToday: Int,
    val violationsToday: Int
)

data class SqlReportQuery(
    val title: String,
    val sql: String,
    val rows: List<String>
)

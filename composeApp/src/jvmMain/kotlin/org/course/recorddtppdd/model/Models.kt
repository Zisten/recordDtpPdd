package org.course.recorddtppdd.model

import java.time.LocalDateTime

data class Officer(
    val id: Int,
    val login: String,
    val password: String
)

data class Driver(
    val id: Int,
    val fullName: String,
    val birthdate: String,
    val address: String,
    val phone: String
)

data class License(
    val id: Int,
    val driverId: Int,
    val series: String,
    val number: String,
    val category: String,
    val issueDate: String
)

data class Vehicle(
    val id: Int,
    val make: String,
    val model: String,
    val vin: String,
    val numberPlate: String,
    val sts: String,
    val ownerName: String,
    val ownerAddress: String
)

data class AccidentRecord(
    val id: Int,
    val locationStreet: String,
    val locationBuilding: String,
    val datetime: LocalDateTime,
    val witnessesName: String,
    val witnessesAddress: String,
    val description: String,
    val circumstances: String,
    val guiltySide: String,
    val officerId: Int,
    val createdAt: LocalDateTime
)

data class AccidentParticipant(
    val id: Int,
    val accidentId: Int,
    val side: String,
    val driverId: Int,
    val vehicleId: Int,
    val damages: String,
    val notes: String,
    val licenseId: Int?,
    val insuranceCompany: String,
    val insurancePolicy: String,
    val insuranceExpiry: String,
    val hasHull: Boolean
)

data class ViolationRecord(
    val id: Int,
    val driverId: Int,
    val vehicleId: Int?,
    val licenseId: Int?,
    val violationTypeId: Int?,
    val npaPoint: String,
    val description: String,
    val witnessName: String,
    val witnessAddress: String,
    val witnessPhone: String,
    val datetime: LocalDateTime,
    val officerId: Int,
    val createdAt: LocalDateTime
)

data class ViolationType(
    val id: Int,
    val name: String,
    val description: String
)

/** Сводные данные для главного экрана */
data class HomeStats(
    val accidentsToday: Int,
    val violationsToday: Int
)

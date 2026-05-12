/*
 Navicat Premium Dump SQL

 Source Server         : MySQL
 Source Server Type    : MySQL
 Source Server Version : 90000 (9.0.0)
 Source Host           : localhost:3306
 Source Schema         : DTP_PDD

 Target Server Type    : MySQL
 Target Server Version : 90000 (9.0.0)
 File Encoding         : 65001

 Date: 11/05/2026 10:33:30
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `Payments`;
DROP TABLE IF EXISTS `ViolationRecords`;
DROP TABLE IF EXISTS `AccidentParticipants`;
DROP TABLE IF EXISTS `AccidentsRecords`;
DROP TABLE IF EXISTS `Licenses`;
DROP TABLE IF EXISTS `Vehicles`;
DROP TABLE IF EXISTS `ViolationsTypes`;
DROP TABLE IF EXISTS `Drivers`;
DROP TABLE IF EXISTS `AccidentTypes`;
DROP TABLE IF EXISTS `Officers`;

-- ----------------------------
-- Table structure for AccidentTypes
-- ----------------------------
CREATE TABLE `AccidentTypes` (
  `type_id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` text,
  PRIMARY KEY (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for Officers
-- ----------------------------
CREATE TABLE `Officers` (
  `officer_id` int NOT NULL AUTO_INCREMENT,
  `last_name` varchar(255) NOT NULL,
  `first_name` varchar(255) NOT NULL,
  `middle_name` varchar(255) DEFAULT NULL,
  `login` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  PRIMARY KEY (`officer_id`),
  UNIQUE KEY `login` (`login`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for Drivers
-- ----------------------------
CREATE TABLE `Drivers` (
  `driver_id` int NOT NULL AUTO_INCREMENT,
  `last_name` varchar(255) NOT NULL,
  `first_name` varchar(255) NOT NULL,
  `middle_name` varchar(255) DEFAULT NULL,
  `birth_date` date DEFAULT NULL,
  `birth_place` varchar(255) DEFAULT NULL,
  `registration_address` text,
  `actual_address` text,
  `phone` varchar(50) DEFAULT NULL,
  `employment_info` text,
  PRIMARY KEY (`driver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for Vehicles
-- ----------------------------
CREATE TABLE `Vehicles` (
  `vehicle_id` int NOT NULL AUTO_INCREMENT,
  `owner_id` int DEFAULT NULL,
  `brand` varchar(255) NOT NULL,
  `model` varchar(255) NOT NULL,
  `number_plate` varchar(20) NOT NULL,
  `vin` varchar(17) DEFAULT NULL,
  `reg_certificate` varchar(255) DEFAULT NULL,
  `insurance_name` varchar(255) DEFAULT NULL,
  `insurance_policy` varchar(255) DEFAULT NULL,
  `insurance_expiry` date DEFAULT NULL,
  `has_hull_insurance` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`vehicle_id`),
  KEY `fk_Vehicles_Owner` (`owner_id`),
  CONSTRAINT `fk_Vehicles_Owner` FOREIGN KEY (`owner_id`) REFERENCES `Drivers` (`driver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for ViolationsTypes
-- ----------------------------
CREATE TABLE `ViolationsTypes` (
  `type_id` int NOT NULL AUTO_INCREMENT,
  `clause` varchar(255) NOT NULL,
  `description` text NOT NULL,
  `fine_amount` int NOT NULL,
  PRIMARY KEY (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for AccidentsRecords
-- ----------------------------
CREATE TABLE `AccidentsRecords` (
  `accident_id` int NOT NULL AUTO_INCREMENT,
  `officer_id` int NOT NULL,
  `type_id` int DEFAULT NULL,
  `date_time` datetime NOT NULL,
  `street` varchar(255) NOT NULL,
  `house` varchar(255) DEFAULT NULL,
  `witnesses_info` text,
  `circumstances_json` json DEFAULT NULL,
  `explanation` text,
  PRIMARY KEY (`accident_id`),
  KEY `fk_Accident_Officer` (`officer_id`),
  KEY `fk_Accident_Type` (`type_id`),
  CONSTRAINT `fk_Accident_Officer` FOREIGN KEY (`officer_id`) REFERENCES `Officers` (`officer_id`),
  CONSTRAINT `fk_Accident_Type` FOREIGN KEY (`type_id`) REFERENCES `AccidentTypes` (`type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for AccidentParticipants
-- ----------------------------
CREATE TABLE `AccidentParticipants` (
  `participant_id` int NOT NULL AUTO_INCREMENT,
  `accident_id` int NOT NULL,
  `driver_id` int NOT NULL,
  `vehicle_id` int NOT NULL,
  `role` char(1) NOT NULL,
  `impact_spot` text,
  `damage_details` text,
  `remarks` text,
  PRIMARY KEY (`participant_id`),
  KEY `fk_AccidentPart_Accident` (`accident_id`),
  KEY `fk_AccidentPart_Driver` (`driver_id`),
  KEY `fk_AccidentPart_Vehicle` (`vehicle_id`),
  CONSTRAINT `fk_AccidentPart_Accident` FOREIGN KEY (`accident_id`) REFERENCES `AccidentsRecords` (`accident_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_AccidentPart_Driver` FOREIGN KEY (`driver_id`) REFERENCES `Drivers` (`driver_id`),
  CONSTRAINT `fk_AccidentPart_Vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `Vehicles` (`vehicle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for ViolationRecords
-- ----------------------------
CREATE TABLE `ViolationRecords` (
  `record_id` int NOT NULL AUTO_INCREMENT,
  `officer_id` int NOT NULL,
  `driver_id` int NOT NULL,
  `vehicle_id` int NOT NULL,
  `type_id` int NOT NULL,
  `date_time` datetime NOT NULL,
  `street` varchar(255) NOT NULL,
  `house_number` varchar(50) DEFAULT NULL,
  `witness_victim_info` text,
  PRIMARY KEY (`record_id`),
  KEY `fk_Violation_Driver` (`driver_id`),
  KEY `fk_Violation_Vehicle` (`vehicle_id`),
  KEY `fk_Violation_Type` (`type_id`),
  KEY `fk_Violation_Officer` (`officer_id`),
  CONSTRAINT `fk_Violation_Driver` FOREIGN KEY (`driver_id`) REFERENCES `Drivers` (`driver_id`),
  CONSTRAINT `fk_Violation_Officer` FOREIGN KEY (`officer_id`) REFERENCES `Officers` (`officer_id`),
  CONSTRAINT `fk_Violation_Type` FOREIGN KEY (`type_id`) REFERENCES `ViolationsTypes` (`type_id`),
  CONSTRAINT `fk_Violation_Vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `Vehicles` (`vehicle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for Payments
-- ----------------------------
CREATE TABLE `Payments` (
  `payment_id` int NOT NULL AUTO_INCREMENT,
  `record_id` int NOT NULL,
  `payment_date` date NOT NULL,
  `amount_paid` decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (`payment_id`),
  KEY `fk_Payments_Violation` (`record_id`),
  CONSTRAINT `fk_Payments_Violation` FOREIGN KEY (`record_id`) REFERENCES `ViolationRecords` (`record_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for Licenses
-- ----------------------------
CREATE TABLE `Licenses` (
  `license_id` int NOT NULL AUTO_INCREMENT,
  `driver_id` int NOT NULL,
  `series` varchar(50) NOT NULL,
  `number` varchar(50) NOT NULL,
  `category` varchar(50) DEFAULT NULL,
  `issue_date` date NOT NULL,
  `issued_by` text,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`license_id`),
  UNIQUE KEY `uniq_license` (`series`,`number`),
  KEY `fk_Licenses_Driver` (`driver_id`),
  CONSTRAINT `fk_Licenses_Driver` FOREIGN KEY (`driver_id`) REFERENCES `Drivers` (`driver_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;

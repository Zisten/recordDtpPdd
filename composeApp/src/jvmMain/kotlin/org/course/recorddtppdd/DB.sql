-- ----------------------------
-- Table structure for AccidentParticipants
-- ----------------------------
DROP TABLE IF EXISTS `AccidentParticipants`;
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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for AccidentsRecords
-- ----------------------------
DROP TABLE IF EXISTS `AccidentsRecords`;
CREATE TABLE `AccidentsRecords` (
                                    `accident_id` int NOT NULL AUTO_INCREMENT,
                                    `officer_id` int NOT NULL,
                                    `date_time` datetime NOT NULL,
                                    `street` varchar(255) NOT NULL,
                                    `house` varchar(255) DEFAULT NULL,
                                    `witnesses_info` text,
                                    `circumstances_json` json DEFAULT NULL,
                                    `explanation` text,
                                    PRIMARY KEY (`accident_id`),
                                    KEY `fk_Accident_Officer` (`officer_id`),
                                    CONSTRAINT `fk_Accident_Officer` FOREIGN KEY (`officer_id`) REFERENCES `Officers` (`officer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for Drivers
-- ----------------------------
DROP TABLE IF EXISTS `Drivers`;
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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for DriversArchive
-- ----------------------------
DROP TABLE IF EXISTS `DriversArchive`;
CREATE TABLE `DriversArchive` (
                                  `archive_id` int NOT NULL AUTO_INCREMENT,
                                  `driver_id` int NOT NULL,
                                  `full_name` varchar(255) NOT NULL,
                                  `deleted_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (`archive_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for Licenses
-- ----------------------------
DROP TABLE IF EXISTS `Licenses`;
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
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for Officers
-- ----------------------------
DROP TABLE IF EXISTS `Officers`;
CREATE TABLE `Officers` (
                            `officer_id` int NOT NULL AUTO_INCREMENT,
                            `last_name` varchar(255) NOT NULL,
                            `first_name` varchar(255) NOT NULL,
                            `middle_name` varchar(255) DEFAULT NULL,
                            `login` varchar(255) NOT NULL,
                            `password` varchar(255) NOT NULL,
                            PRIMARY KEY (`officer_id`),
                            UNIQUE KEY `login` (`login`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for Payments
-- ----------------------------
DROP TABLE IF EXISTS `Payments`;
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
-- Table structure for Vehicles
-- ----------------------------
DROP TABLE IF EXISTS `Vehicles`;
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for ViolationRecords
-- ----------------------------
DROP TABLE IF EXISTS `ViolationRecords`;
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
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Table structure for ViolationsTypes
-- ----------------------------
DROP TABLE IF EXISTS `ViolationsTypes`;
CREATE TABLE `ViolationsTypes` (
                                   `type_id` int NOT NULL AUTO_INCREMENT,
                                   `clause` varchar(255) NOT NULL,
                                   `description` text NOT NULL,
                                   `fine_amount` int NOT NULL,
                                   PRIMARY KEY (`type_id`)
) ENGINE=InnoDB AUTO_INCREMENT=142 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- ---------------------------------------------------------
-- 1. ТАБЛИЦА ДЛЯ ТРИГГЕРА АРХИВИРОВАНИЯ
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `DriversArchive` (
                                                `archive_id` int NOT NULL AUTO_INCREMENT,
                                                `driver_id` int NOT NULL,
                                                `full_name` varchar(255) NOT NULL,
    `deleted_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`archive_id`)
    );

-- ---------------------------------------------------------
-- 2. ТРИГГЕРЫ (3 шт.)
-- ---------------------------------------------------------
-- Триггер 1: Проверка даты нарушения (нельзя добавить из будущего)
DELIMITER //
CREATE TRIGGER trg_check_violation_date
    BEFORE INSERT ON ViolationRecords
    FOR EACH ROW
BEGIN
    IF NEW.date_time > NOW() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Дата нарушения не может быть в будущем!';
END IF;
END//
DELIMITER ;

-- Триггер 2: Проверка даты ДТП
DELIMITER //
CREATE TRIGGER trg_check_accident_date
    BEFORE INSERT ON AccidentsRecords
    FOR EACH ROW
BEGIN
    IF NEW.date_time > NOW() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Дата ДТП не может быть в будущем!';
END IF;
END//
DELIMITER ;

-- Триггер 3: Логирование удаленных водителей в архив
DELIMITER //
CREATE TRIGGER trg_log_driver_delete
    AFTER DELETE ON Drivers
    FOR EACH ROW
BEGIN
    INSERT INTO DriversArchive (driver_id, full_name)
    VALUES (OLD.driver_id, CONCAT(OLD.last_name, ' ', OLD.first_name));
END//
DELIMITER ;

-- ---------------------------------------------------------
-- 3. ПРЕДСТАВЛЕНИЯ / VIEWS (2 шт.)
-- ---------------------------------------------------------
-- Представление 1: Профиль риска водителя (ФИО, кол-во нарушений, общая сумма штрафов)
CREATE OR REPLACE VIEW View_DriverRiskProfile AS
SELECT
    d.driver_id,
    CONCAT(d.last_name, ' ', d.first_name) AS driver_name,
    COUNT(vr.record_id) AS total_violations,
    COALESCE(SUM(vt.fine_amount), 0) AS total_fines
FROM Drivers d
         LEFT JOIN ViolationRecords vr ON d.driver_id = vr.driver_id
         LEFT JOIN ViolationsTypes vt ON vr.type_id = vt.type_id
GROUP BY d.driver_id, driver_name;

-- Представление 2: Детали ДТП с расшифровкой офицера и кол-вом участников
CREATE OR REPLACE VIEW View_AccidentFullDetails AS
SELECT
    a.accident_id,
    a.date_time,
    a.street,
    CONCAT(o.last_name, ' ', o.first_name) AS officer_name,
    (SELECT COUNT(*) FROM AccidentParticipants ap WHERE ap.accident_id = a.accident_id) as participants_count
FROM AccidentsRecords a
         JOIN Officers o ON a.officer_id = o.officer_id;

-- ---------------------------------------------------------
-- 4. ХРАНИМЫЕ ПРОЦЕДУРЫ И ФУНКЦИИ (3 шт.)
-- ---------------------------------------------------------
-- Функция 1: Подсчет неоплаченных/общих штрафов конкретного водителя
DELIMITER //
CREATE FUNCTION GetTotalFines(p_driver_id INT) RETURNS INT
    DETERMINISTIC
BEGIN
    DECLARE total INT;
SELECT COALESCE(SUM(vt.fine_amount), 0) INTO total
FROM ViolationRecords vr
         JOIN ViolationsTypes vt ON vr.type_id = vt.type_id
WHERE vr.driver_id = p_driver_id;
RETURN total;
END//
DELIMITER ;

-- Процедура 1: Получение истории нарушений конкретного водителя
DELIMITER //
CREATE PROCEDURE GetDriverHistory(IN p_driver_id INT)
BEGIN
SELECT vr.date_time, vt.clause, vt.fine_amount, vr.street
FROM ViolationRecords vr
         JOIN ViolationsTypes vt ON vr.type_id = vt.type_id
WHERE vr.driver_id = p_driver_id
ORDER BY vr.date_time DESC;
END//
DELIMITER ;

-- Процедура 2: Статистика ДТП по конкретной улице
DELIMITER //
CREATE PROCEDURE GetStreetAccidentStats(IN p_street VARCHAR(255))
BEGIN
SELECT date_time, explanation
FROM AccidentsRecords
WHERE street LIKE CONCAT('%', p_street, '%')
ORDER BY date_time DESC;
END//
DELIMITER ;

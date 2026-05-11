CREATE TABLE IF NOT EXISTS AuditLog (
    id INT AUTO_INCREMENT PRIMARY KEY,
    action_desc VARCHAR(255),
    action_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 1. ПРЕДСТАВЛЕНИЯ / VIEWS (2 шт.)
-- ==========================================
-- View 1: Полная информация по ДТП с адресами и типом из AccidentTypes
CREATE OR REPLACE VIEW View_AccidentsFull AS
SELECT a.accident_id, a.date_time, a.street, a.house, o.login AS officer_login, t.name AS type_name
FROM AccidentsRecords a
JOIN Officers o ON a.officer_id = o.officer_id
LEFT JOIN AccidentTypes t ON a.type_id = t.type_id;

-- View 2: Сводка по водителям (подзапросы для подсчета нарушений и ДТП)
CREATE OR REPLACE VIEW View_DriverStats AS
SELECT d.driver_id, d.last_name, d.first_name,
       (SELECT COUNT(*) FROM ViolationRecords vr WHERE vr.driver_id = d.driver_id) as total_violations,
       (SELECT COUNT(*) FROM AccidentParticipants ap WHERE ap.driver_id = d.driver_id) as total_accidents
FROM Drivers d;


-- ==========================================
-- 2. ТРИГГЕРЫ / TRIGGERS (3 шт.)
-- ==========================================
DELIMITER //

-- Trigger 1: Логирование нового нарушения ПДД
CREATE TRIGGER After_Violation_Insert
AFTER INSERT ON ViolationRecords
FOR EACH ROW
BEGIN
    INSERT INTO AuditLog (action_desc) 
    VALUES (CONCAT('Добавлено нарушение водителю ID: ', NEW.driver_id, ' по статье ID: ', NEW.type_id));
END//

DELIMITER //
-- Trigger 2: Защита от пустой улицы при добавлении ДТП (NOT NULL)
CREATE TRIGGER Before_Accident_Insert
BEFORE INSERT ON AccidentsRecords
FOR EACH ROW
BEGIN
    IF NEW.street IS NULL OR TRIM(NEW.street) = '' THEN
        SET NEW.street = 'Не указано';
    END IF;
END//

DELIMITER //
-- Trigger 3: Автоматическое приведение VIN к верхнему регистру
CREATE TRIGGER Before_Vehicle_Insert
BEFORE INSERT ON Vehicles
FOR EACH ROW
BEGIN
    IF NEW.vin IS NOT NULL THEN
        SET NEW.vin = UPPER(NEW.vin);
    END IF;
END//

DELIMITER //
-- ==========================================
-- 3. ХРАНИМЫЕ ПРОЦЕДУРЫ И ФУНКЦИИ (3 шт.)
-- ==========================================
-- Function 1: Расчет общей суммы штрафов водителя
CREATE FUNCTION Func_GetTotalDriverFines(p_driver_id INT) RETURNS DECIMAL(10,2)
DETERMINISTIC
BEGIN
    DECLARE total DECIMAL(10,2);
    SELECT COALESCE(SUM(vt.fine_amount), 0) INTO total
    FROM ViolationRecords vr
    JOIN ViolationsTypes vt ON vr.type_id = vt.type_id
    WHERE vr.driver_id = p_driver_id;
    RETURN total;
END//

DELIMITER //
-- Procedure 1: Получение истории водителя по ID
CREATE PROCEDURE Proc_GetDriverHistory(IN p_driver_id INT)
BEGIN
    SELECT 'Нарушение' as event_type, date_time, street FROM ViolationRecords WHERE driver_id = p_driver_id
    UNION ALL
    SELECT 'ДТП' as event_type, a.date_time, a.street 
    FROM AccidentsRecords a JOIN AccidentParticipants ap ON a.accident_id = ap.accident_id 
    WHERE ap.driver_id = p_driver_id;
END//
DELIMITER //
-- Procedure 2: Массовое действие (например, отметка о старых штрафах)
CREATE PROCEDURE Proc_AuditMinorFines()
BEGIN
    INSERT INTO AuditLog (action_desc) VALUES ('Вызвана процедура Proc_AuditMinorFines');
END//

DELIMITER ;

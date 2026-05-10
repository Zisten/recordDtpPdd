-- ============================================================
-- schema.sql  —  База данных DTP_PDD
-- MySQL 8+
-- ============================================================

CREATE DATABASE IF NOT EXISTS DTP_PDD
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE DTP_PDD;

-- ──────────────────────────────────────────────────────────────
-- ТАБЛИЦЫ
-- ──────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS Officers (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    login    VARCHAR(100)  NOT NULL UNIQUE,
    password VARCHAR(255)  NOT NULL
) ENGINE=InnoDB;

-- Вставим тестового офицера (логин: admin / пароль: admin)
INSERT IGNORE INTO Officers (login, password) VALUES ('admin', 'admin');

CREATE TABLE IF NOT EXISTS Drivers (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    birthdate VARCHAR(20),
    address   VARCHAR(500),
    phone     VARCHAR(50)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Licenses (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    driver_id  INT         NOT NULL,
    series     VARCHAR(20),
    number     VARCHAR(20),
    category   VARCHAR(50),
    issue_date VARCHAR(20),
    FOREIGN KEY (driver_id) REFERENCES Drivers(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Vehicles (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    make          VARCHAR(100),
    model         VARCHAR(100),
    vin           VARCHAR(50),
    number_plate  VARCHAR(30)  NOT NULL,
    sts           VARCHAR(50),
    owner_name    VARCHAR(255),
    owner_address VARCHAR(500)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS AccidentsRecords (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    location_street   VARCHAR(255),
    location_building VARCHAR(50),
    datetime          DATETIME     NOT NULL,
    witnesses_name    VARCHAR(255) DEFAULT '',
    witnesses_address VARCHAR(500) DEFAULT '',
    description       TEXT,
    circumstances     TEXT         DEFAULT '[]',
    guilty_side       VARCHAR(20)  DEFAULT 'не определён',
    officer_id        INT          NOT NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (officer_id) REFERENCES Officers(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS AccidentParticipants (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    accident_id       INT         NOT NULL,
    side              VARCHAR(5)  NOT NULL,
    driver_id         INT         NOT NULL,
    vehicle_id        INT         NOT NULL,
    damages           TEXT        DEFAULT '',
    notes             TEXT        DEFAULT '',
    license_id        INT,
    insurance_company VARCHAR(255) DEFAULT '',
    insurance_policy  VARCHAR(100) DEFAULT '',
    insurance_expiry  VARCHAR(20)  DEFAULT '',
    has_hull          TINYINT(1)   DEFAULT 0,
    FOREIGN KEY (accident_id) REFERENCES AccidentsRecords(id) ON DELETE CASCADE,
    FOREIGN KEY (driver_id)   REFERENCES Drivers(id),
    FOREIGN KEY (vehicle_id)  REFERENCES Vehicles(id),
    FOREIGN KEY (license_id)  REFERENCES Licenses(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ViolationsTypes (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT         DEFAULT ''
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ViolationRecords (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    driver_id         INT         NOT NULL,
    vehicle_id        INT,
    license_id        INT,
    violation_type_id INT,
    npa_point         VARCHAR(100) DEFAULT '',
    description       TEXT         DEFAULT '',
    witness_name      VARCHAR(255) DEFAULT '',
    witness_address   VARCHAR(500) DEFAULT '',
    witness_phone     VARCHAR(50)  DEFAULT '',
    datetime          DATETIME     NOT NULL,
    officer_id        INT          NOT NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (driver_id)         REFERENCES Drivers(id),
    FOREIGN KEY (vehicle_id)        REFERENCES Vehicles(id),
    FOREIGN KEY (license_id)        REFERENCES Licenses(id),
    FOREIGN KEY (violation_type_id) REFERENCES ViolationsTypes(id),
    FOREIGN KEY (officer_id)        REFERENCES Officers(id)
) ENGINE=InnoDB;

-- ──────────────────────────────────────────────────────────────
-- VIEW-1: Сводная информация о ДТП с данными офицера
-- (использует JOIN)
-- ──────────────────────────────────────────────────────────────
CREATE OR REPLACE VIEW v_accidents_summary AS
SELECT
    ar.id,
    ar.datetime,
    CONCAT(ar.location_street, ', ', ar.location_building) AS location,
    ar.guilty_side,
    ar.created_at,
    o.login AS officer_login,
    (
        -- Подзапрос 1: количество участников данного ДТП
        SELECT COUNT(*) FROM AccidentParticipants ap WHERE ap.accident_id = ar.id
    ) AS participants_count
FROM AccidentsRecords ar
         -- JOIN 1
         JOIN Officers o ON o.id = ar.officer_id;

-- ──────────────────────────────────────────────────────────────
-- VIEW-2: Сводная информация о нарушениях ПДД с ФИО водителя
-- (использует JOIN)
-- ──────────────────────────────────────────────────────────────
CREATE OR REPLACE VIEW v_violations_summary AS
SELECT
    vr.id,
    vr.datetime,
    vr.npa_point,
    vr.description,
    -- JOIN 2
    d.full_name  AS driver_name,
    d.phone      AS driver_phone,
    -- JOIN 3
    vt.name      AS violation_type,
    vr.created_at,
    o.login      AS officer_login
FROM ViolationRecords vr
         JOIN Drivers d        ON d.id = vr.driver_id
         LEFT JOIN ViolationsTypes vt ON vt.id = vr.violation_type_id
         JOIN Officers o        ON o.id = vr.officer_id;

-- ──────────────────────────────────────────────────────────────
-- АНАЛИТИЧЕСКИЕ ЗАПРОСЫ С CTE И ОКОННЫМИ ФУНКЦИЯМИ
-- ──────────────────────────────────────────────────────────────

-- CTE-1: Ранжирование офицеров по количеству оформленных ДТП
-- (оконная функция 1: RANK)
WITH officer_accident_stats AS (
    SELECT
        officer_id,
        COUNT(*) AS total_accidents
    FROM AccidentsRecords
    GROUP BY officer_id
)
SELECT
    o.login,
    COALESCE(oas.total_accidents, 0)                AS accidents,
    RANK() OVER (ORDER BY COALESCE(oas.total_accidents, 0) DESC) AS rnk
FROM Officers o
         LEFT JOIN officer_accident_stats oas ON oas.officer_id = o.id;

-- CTE-2: Подзапрос 2 — водители с более чем одним нарушением
-- (оконная функция 2: ROW_NUMBER)
WITH driver_violation_counts AS (
    SELECT
        driver_id,
        COUNT(*) AS violation_count,
        ROW_NUMBER() OVER (ORDER BY COUNT(*) DESC) AS rn
    FROM ViolationRecords
    GROUP BY driver_id
    HAVING COUNT(*) > 1
)
SELECT
    d.full_name,
    d.phone,
    dvc.violation_count,
    dvc.rn
FROM driver_violation_counts dvc
         JOIN Drivers d ON d.id = dvc.driver_id;

-- CTE-3: Скользящее 7-дневное количество ДТП
-- (оконная функция 3: SUM OVER с фреймом)
WITH daily_accidents AS (
    SELECT
        DATE(datetime)   AS day,
        COUNT(*)         AS cnt
    FROM AccidentsRecords
    GROUP BY DATE(datetime)
)
SELECT
    day,
    cnt,
    SUM(cnt) OVER (
        ORDER BY day
        ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
        )          AS rolling_7d_sum,
    -- оконная функция 4: LAG для изменения
    LAG(cnt, 1, 0) OVER (ORDER BY day) AS prev_day_cnt
FROM daily_accidents;

-- ──────────────────────────────────────────────────────────────
-- ТРИГГЕРЫ
-- ──────────────────────────────────────────────────────────────

-- Лог-таблица для аудита
CREATE TABLE IF NOT EXISTS AuditLog (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50),
    table_name VARCHAR(50),
    record_id  INT,
    event_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Триггер 1: Логировать создание нового ДТП
DROP TRIGGER IF EXISTS trg_after_accident_insert;
CREATE TRIGGER trg_after_accident_insert
    AFTER INSERT ON AccidentsRecords
    FOR EACH ROW
BEGIN
    INSERT INTO AuditLog (event_type, table_name, record_id)
    VALUES ('INSERT', 'AccidentsRecords', NEW.id);
END;

-- Триггер 2: Логировать создание нового нарушения
DROP TRIGGER IF EXISTS trg_after_violation_insert;
CREATE TRIGGER trg_after_violation_insert
    AFTER INSERT ON ViolationRecords
    FOR EACH ROW
BEGIN
    INSERT INTO AuditLog (event_type, table_name, record_id)
    VALUES ('INSERT', 'ViolationRecords', NEW.id);
END;

-- Триггер 3: Устанавливать created_at при INSERT если не задан
--            (защитный триггер перед вставкой)
DROP TRIGGER IF EXISTS trg_before_violation_insert;
CREATE TRIGGER trg_before_violation_insert
    BEFORE INSERT ON ViolationRecords
    FOR EACH ROW
BEGIN
    IF NEW.created_at IS NULL THEN
        SET NEW.created_at = NOW();
    END IF;
END;

-- ──────────────────────────────────────────────────────────────
-- ХРАНИМЫЕ ПРОЦЕДУРЫ / ФУНКЦИИ
-- ──────────────────────────────────────────────────────────────

-- Процедура 1: Получить статистику за указанную дату
DROP PROCEDURE IF EXISTS sp_daily_stats;
CREATE PROCEDURE sp_daily_stats(IN p_date DATE)
BEGIN
    SELECT
        p_date                                          AS stat_date,
        (SELECT COUNT(*) FROM AccidentsRecords
         WHERE DATE(datetime) = p_date)                AS accidents_count,
        (SELECT COUNT(*) FROM ViolationRecords
         WHERE DATE(datetime) = p_date)                AS violations_count;
END;

-- Процедура 2: Получить всех участников ДТП по ID
DROP PROCEDURE IF EXISTS sp_accident_participants;
CREATE PROCEDURE sp_accident_participants(IN p_accident_id INT)
BEGIN
    SELECT
        ap.side,
        d.full_name   AS driver_name,
        d.phone       AS driver_phone,
        v.make        AS vehicle_make,
        v.model       AS vehicle_model,
        v.number_plate,
        ap.damages,
        ap.insurance_company,
        ap.insurance_policy
    FROM AccidentParticipants ap
             JOIN Drivers  d ON d.id = ap.driver_id
             JOIN Vehicles v ON v.id = ap.vehicle_id
    WHERE ap.accident_id = p_accident_id;
END;

-- Функция 3: Вернуть количество нарушений водителя
DROP FUNCTION IF EXISTS fn_driver_violation_count;
CREATE FUNCTION fn_driver_violation_count(p_driver_id INT)
    RETURNS INT
    READS SQL DATA
    DETERMINISTIC
BEGIN
    DECLARE v_count INT;
    SELECT COUNT(*) INTO v_count
    FROM ViolationRecords
    WHERE driver_id = p_driver_id;
    RETURN COALESCE(v_count, 0);
END;

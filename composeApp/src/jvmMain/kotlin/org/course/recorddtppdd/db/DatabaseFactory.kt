package org.course.recorddtppdd.db

import org.course.recorddtppdd.config.AppConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Фабрика подключения к базе данных.
 *
 * Использует Exposed ORM (DSL).
 * Подключение происходит через [Database.connect]:
 *   - url, user, password берутся из [AppConfig]
 *   - драйвер: com.mysql.cj.jdbc.Driver
 *
 * Все запросы выполняются внутри блока [transaction { }].
 *
 * Пример:
 * ```kotlin
 * DatabaseFactory.init()
 * val officers = transaction { Officers.selectAll().toList() }
 * ```
 */
object DatabaseFactory {

    private var initialized = false

    /**
     * Инициализирует подключение к MySQL и создаёт таблицы, если они ещё не существуют.
     * Вызывается один раз при старте приложения.
     * @throws Exception если подключение к БД не удалось.
     */
    fun init() {
        if (initialized) return

        Database.connect(
            url = AppConfig.DB_URL,
            driver = AppConfig.DB_DRIVER,
            user = AppConfig.DB_USER,
            password = AppConfig.DB_PASSWORD
        )

        // Создаём таблицы если их нет (SchemaUtils.createMissingTablesAndColumns)
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Officers,
                Drivers,
                Licenses,
                Vehicles,
                AccidentsRecords,
                AccidentParticipants,
                ViolationsTypes,
                ViolationRecords
            )
        }

        initialized = true
    }
}

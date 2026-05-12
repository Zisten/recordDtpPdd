package org.course.recorddtppdd.db

import org.course.recorddtppdd.config.AppConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

/**
 * Фабрика подключения к базе данных.
 *
 * Схема создаётся через Flyway (скрипт в resources/db/migration).
 * Поэтому мы НЕ вызываем SchemaUtils.create* для MySQL.
 */
object DatabaseFactory {

    private var initialized = false

    fun init() {
        if (initialized) return

        val flyway = Flyway.configure()
            .dataSource(AppConfig.DB_URL, AppConfig.DB_USER, AppConfig.DB_PASSWORD)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load()
        flyway.migrate()

        Database.connect(
            url = AppConfig.DB_URL,
            driver = AppConfig.DB_DRIVER,
            user = AppConfig.DB_USER,
            password = AppConfig.DB_PASSWORD
        )

        initialized = true
    }
}

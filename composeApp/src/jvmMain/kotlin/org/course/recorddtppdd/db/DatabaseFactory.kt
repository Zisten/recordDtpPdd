package org.course.recorddtppdd.db

import org.course.recorddtppdd.config.AppConfig
import org.jetbrains.exposed.sql.Database

/**
 * Фабрика подключения к базе данных.
 *
 * ВАЖНО:
 *  - В этом проекте схема БД создаётся/мигрируется SQL-скриптом (см. /sql в репозитории).
 *  - Поэтому мы НЕ вызываем SchemaUtils.create* для MySQL, иначе Exposed попытается
 *    "досоздать" таблицы/колонки по Tables.kt и может привести к конфликтам
 *    (в т.ч. ошибке вида: "Incorrect table definition; there can be only one auto column...").
 */
object DatabaseFactory {

    private var initialized = false

    fun init() {
        if (initialized) return

        Database.connect(
            url = AppConfig.DB_URL,
            driver = AppConfig.DB_DRIVER,
            user = AppConfig.DB_USER,
            password = AppConfig.DB_PASSWORD
        )

        initialized = true
    }
}

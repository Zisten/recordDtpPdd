package org.course.recorddtppdd.db

import org.course.recorddtppdd.config.AppConfig
import org.jetbrains.exposed.sql.Database

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

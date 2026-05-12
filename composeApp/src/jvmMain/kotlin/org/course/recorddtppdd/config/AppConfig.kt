package org.course.recorddtppdd.config

/**
 * Конфигурация подключения к базе данных MySQL.
 * Измените эти значения, если ваши параметры БД отличаются.
 */
object AppConfig {
    const val DB_URL = "jdbc:mysql://localhost:3306/DTP_PDD"
    const val DB_USER = "root"
    const val DB_PASSWORD = "123ns123"
    const val DB_DRIVER = "com.mysql.cj.jdbc.Driver"
}

# Система учёта ДТП и нарушений ПДД

Desktop-приложение на **Kotlin + Compose Multiplatform (JVM)** для оформления дорожно-транспортных происшествий и нарушений ПДД с хранением данных в **MySQL**.

---

## Технологии

| Стек | Версия |
|---|---|
| Kotlin | 2.3.21 |
| Compose Multiplatform Desktop | 1.10.3 |
| Material3 | 1.10.0-alpha05 |
| Exposed ORM (DSL) | 0.60.0 |
| Flyway | 10.16.0 |
| MySQL Connector/J | 8.0.33 |
| SLF4J Simple | 2.0.16 |

---

## Быстрый старт

### 1. Поднять MySQL

Установите и запустите MySQL 8+. Рекомендуется Docker:

```bash
docker run -d \
  --name mysql-dtp \
  -e MYSQL_ROOT_PASSWORD=123ns123 \
  -p 3306:3306 \
  mysql:8
```

### 2. Настроить параметры подключения

Откройте файл:

```
composeApp/src/jvmMain/kotlin/org/course/recorddtppdd/config/AppConfig.kt
```

Измените значения по необходимости:

```kotlin
object AppConfig {
    const val DB_URL      = "jdbc:mysql://localhost:3306/DTP_PDD?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true"
    const val DB_USER     = "root"
    const val DB_PASSWORD = "123ns123"
}
```

### 3. Запустить приложение

При первом запуске Flyway автоматически применит миграцию из `db/migration`.

```bash
./gradlew :composeApp:run
```

На Windows:

```bat
.\gradlew.bat :composeApp:run
```

---

## Структура приложения

```
composeApp/src/jvmMain/kotlin/org/course/recorddtppdd/
├── config/         — параметры подключения к БД
├── db/             — Exposed таблицы, DatabaseFactory, Repository
├── model/          — data-классы (Officer, Driver, Vehicle, ...)
├── ui/
│   ├── theme/      — Color.kt, Theme.kt (Material3, без android.os.Build)
│   └── screens/    — AuthScreen, MainScreen, HomeScreen, AccidentFormScreen, ViolationFormScreen
├── App.kt          — корневой composable, управление навигацией
└── main.kt         — точка входа
```

---

## Подключение MySQL через Exposed

Exposed — Kotlin ORM от JetBrains. Приложение использует **DSL-стиль** (не DAO):

```kotlin
// 1. Подключение (один раз при старте)
Database.connect(
    url    = AppConfig.DB_URL,
    driver = "com.mysql.cj.jdbc.Driver",
    user   = AppConfig.DB_USER,
    password = AppConfig.DB_PASSWORD
)

// 2. Миграции выполняются через Flyway при старте приложения

// 3. Запросы внутри transaction { }
val officer = transaction {
    Officers.selectAll()
        .where { (Officers.login eq login) and (Officers.password eq password) }
        .firstOrNull()
}
```

Все запросы к БД оборачиваются в `transaction { }`. Таблицы описаны в `db/Tables.kt`.

---

## Экраны

| Экран | Описание |
|---|---|
| **Авторизация** | Вход по логину/паролю из таблицы `Officers` |
| **Главная** | Карточки статистики, таблицы ДТП и нарушений + вкладка SQL-отчётов (подзапросы/JOIN/окна/VIEW/триггеры/процедуры/CTE) |
| **Оформление ДТП** | 6-шаговый wizard: место → данные ТС (A и B) → водитель (A и B) → повреждения → обстоятельства → завершение |
| **Оформление ПДД** | 3-шаговый wizard: нарушитель → ТС (с автоподстановкой) → нарушение (НПА из ViolationsTypes, автозаполнение вида нарушения) |

---

## SQL-скрипт (`composeApp/src/jvmMain/resources/db/migration/V1__schema.sql`)

Скрипт содержит таблицы БД `DTP_PDD` согласно актуальной схеме (Drivers, Vehicles, Officers, AccidentsRecords, ViolationRecords и др.).

---

## Задача ветки

Упростить архитектуру приложения, убрать MVVM, использовать schema.sql для инициализации БД, сохранить текущий UI и функциональность.

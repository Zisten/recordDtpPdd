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

### 2. Применить SQL-схему

```bash
mysql -u root -p123ns123 < sql/schema.sql
```

Скрипт создаёт базу данных `DTP_PDD`, все таблицы, представления (VIEW), триггеры, хранимые процедуры/функции, а также тестового пользователя:
- **Логин:** `admin`
- **Пароль:** `admin`

### 3. Настроить параметры подключения

Откройте файл:

```
composeApp/src/jvmMain/kotlin/org/course/recorddtppdd/config/AppConfig.kt
```

Измените значения по необходимости:

```kotlin
object AppConfig {
    const val DB_URL      = "jdbc:mysql://localhost:3306/DTP_PDD?useSSL=false&serverTimezone=UTC"
    const val DB_USER     = "root"
    const val DB_PASSWORD = "123ns123"
}
```

### 4. Запустить приложение

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
├── viewmodel/      — AuthViewModel, HomeViewModel, AccidentFormViewModel, ViolationFormViewModel
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

// 2. Создание таблиц (если не существуют)
transaction {
    SchemaUtils.createMissingTablesAndColumns(Officers, Drivers, ...)
}

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

## SQL-скрипт (`sql/schema.sql`)

Скрипт содержит:
- ✅ **2 подзапроса** — в VIEW `v_accidents_summary` (COUNT участников) и в процедуре `sp_daily_stats`
- ✅ **3 JOIN** — в VIEW `v_accidents_summary` (JOIN Officers), `v_violations_summary` (JOIN Drivers, JOIN Officers)
- ✅ **4 оконные функции** — `RANK()`, `ROW_NUMBER()`, `SUM() OVER`, `LAG()`
- ✅ **2 VIEW** — `v_accidents_summary`, `v_violations_summary`
- ✅ **3 триггера** — логирование INSERT в AccidentsRecords, ViolationRecords; защитный BEFORE INSERT
- ✅ **3 хранимые процедуры/функции** — `sp_daily_stats`, `sp_accident_participants`, `fn_driver_violation_count`
- ✅ **3 CTE** — ранжирование офицеров, водители-рецидивисты, скользящая сумма ДТП

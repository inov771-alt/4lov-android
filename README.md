# 4LOV Android

Нативное Android-приложение проекта 4LOV (ForLove), Kotlin + Jetpack Compose + Material 3.

## Статус

Это начальная рабочая основа Android-проекта: настроены Gradle, Android Manifest, Compose-тема, навигация между основными разделами, пять категорий 4LOV и слой, отделяющий интерфейс от серверной интеграции.

**Интеграция с PHP API ещё не подключена.** В доступных материалах на момент создания этой версии не было фактических файлов серверного API и его документации. Поэтому в проект намеренно не внесены предполагаемые URL методов, поля запросов или JSON-ответы. Экран не показывает фиктивные объявления и не имитирует публикацию.

## Продуктовые ограничения

- Категории: Отдам (`give`), Приму в дар (`receive`), Нужна помощь (`need`), Могу помочь (`help`), Животные (`animals`).
- География: Россия и Беларусь.
- Нет обязательной регистрации, личных кабинетов, внутренних чатов, лайков, рейтингов и подписок.
- Фирменный знак: `4♡`. Основной акцент — зелёный.

## Технологии

- Kotlin 2.1.20
- Android Gradle Plugin 8.9.2
- Gradle 8.11.1
- Jetpack Compose / Material 3
- Navigation Compose
- OkHttp (для последующей серверной интеграции)
- minSdk 26, compileSdk/targetSdk 35, Java 17

## Открытие

1. Открой Android Studio.
2. Выбери **Open** и укажи каталог `4lov-android`.
3. Дождись загрузки Gradle и установки необходимых Android SDK Platform 35 и Build Tools.
4. Запусти конфигурацию `app` на эмуляторе или устройстве.

Если Android Studio предложит обновить Android Gradle Plugin или Gradle, сначала проверь совместимость версий, а не обновляй их автоматически.

## Сборка APK

В Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

Либо из терминала при наличии Gradle Wrapper:

```bash
./gradlew assembleDebug
```

В этой среде наличие Android SDK/Gradle не подтверждено, поэтому успешная Android-сборка пока не заявляется. Файл `gradle-wrapper.properties` фиксирует версию Gradle; стандартные wrapper-скрипты и wrapper JAR следует сгенерировать командой `gradle wrapper` из установленного Gradle либо через Android Studio.

## Настройка API

Базовый адрес сайта вынесен в `app/src/main/java/ru/fourlov/app/data/ApiConfig.kt`.

Реальные пути методов, HTTP-методы, параметры, модели ответов, правила получения токена, отправка multipart-фотографий и формат жалоб должны быть реализованы после сверки с фактическими PHP-файлами API и серверной документацией. Интерфейс `FourLovApi` — граница интеграции, а не описание уже существующего серверного контракта.

## Структура

```text
4lov-android/
├── app/
│   ├── src/main/AndroidManifest.xml
│   └── src/main/java/ru/fourlov/app/
│       ├── MainActivity.kt
│       ├── data/          # конфигурация и граница API
│       ├── model/         # модели категорий
│       └── ui/theme/      # палитра и Compose-тема
├── gradle/wrapper/
├── .gitignore
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

## GitHub

Создай пустой репозиторий `4lov-android`, затем из корня проекта выполни:

```bash
git init
git add .
git commit -m "Initial 4LOV Android project"
git branch -M main
git remote add origin https://github.com/USERNAME/4lov-android.git
git push -u origin main
```

Замени `USERNAME` на свой GitHub username. Не добавляй в репозиторий `local.properties`, ключи подписи, пароли, серверные секреты или дампы базы данных.

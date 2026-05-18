# Восстановление APK и текущая архитектура

Дата: 2026-05-18.

## Контекст

Проект восстановлен из `compassv33.apk`, оригинальное приложение `PDA Compass V3.1`, пакет `net.afterday.compas`.

Восстановление шло по двум линиям:

- Apktool - ресурсы, manifest, smali, rebuild baseline;
- JADX - Java-подобные исходники для чтения и переноса в Gradle-проект.

## Текущий результат

Создан Android Gradle-проект:

- `settings.gradle.kts`;
- `build.gradle.kts`;
- `gradle.properties`;
- `app/build.gradle.kts`;
- `app/src/main/AndroidManifest.xml`;
- `app/src/main/java/net/afterday/compas`;
- `app/src/main/res`;
- `app/src/main/assets`.

Текущий app-модуль собирается как debug APK с:

- `compileSdk = 36`;
- `targetSdk = 35`;
- `minSdk = 23`;
- `versionCode = 1816`;
- `versionName = 1816-default-5s`.

## Архитектурная форма

Высокоуровневая схема:

```text
MainActivity / LocalMainService
  -> SensorsProvider / DeviceProvider
  -> Engine
  -> InfluenceProviderImpl
  -> Wi-Fi / Bluetooth / GPS influence providers
  -> Game / Player / Inventory / Events
  -> UI fragments and custom PDA views
```

Основные решения:

- не переписывать приложение с нуля;
- сохранить восстановленную структуру, чтобы не потерять игровую механику;
- модернизировать Android-совместимость постепенно;
- радиодетекцию развивать через совместимую модель influence provider.

## Риски

- Декомпилированный код может содержать неочевидные артефакты и synthetic-классы.
- Старые Android Support зависимости стоит мигрировать на AndroidX отдельным этапом.
- Wi-Fi realtime на Android 12-16 нестабилен без учета throttling.
- Полевые сценарии зависят от конкретных телефонов, прошивок, маяков и размещения оборудования.


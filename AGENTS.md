# Правила проекта Clock

Персональный форк Mascot: `com.olivka.clock`, Android 8.0+ (API 26), compile/target SDK 34.
Документация — на русском; `README.md` — английский (по умолчанию), `README.ru.md` — русский; комментарии в коде — на английском.
Разработка ведётся с помощью LLM-агентов: сгенерированный код и текст проверяются, собираются и тестируются.

## Версии

- Нумерация строго `x.y.z` (`3.12.0`), не `x.y`.
- `versionName` — `x.y.z`; `versionCode` — целое число, монотонно растёт.
- При выпуске версии синхронизировать:
  - `app/build.gradle.kts` — `versionName`, `versionCode`;
  - `version.txt` — `versionCode`, `versionName`;
  - `PROJECT_NOTES.md` — текущую версию;
  - `PLAN.md` (локальный, не публикуется) — текущую версию;
  - `changelog.md` — заголовок `## x.y.z — YYYY-MM-DD`;
  - `changelog.dev.md` (локальный, не публикуется) — технические детали.
- `changelog.md` — короткая публичная история человеческим языком;
  `changelog.dev.md` — полная техническая история для разработки.
- Исторические записи changelog не переписывать; обозначения до 3.6.0 (`3.5`, `3.4`, …) сохраняются как есть.

## Сборка и проверка

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Release: R8 + `shrinkResources`, `non-debuggable`, подпись локальным debug-сертификатом.
Не публиковать в Google Play.

## Документация

- Любое изменение поведения, разрешений или планирования будильников отражать в
  `README.md`, `README.ru.md`, `PROJECT_NOTES.md`, локальные `PLAN.md` и
  `changelog.dev.md`, а также в публичном `changelog.md`.
- Держать `README.md` и `README.ru.md` синхронными; в шапке каждого — перекрёстные ссылки.
- Не добавлять `INTERNET`, аналитику, рекламу, AI/ML и лишние разрешения без явной необходимости.

## Скиллы агентов

Android-скиллы лежат в `.opencode/skills/` и подхватываются OpenCode автоматически.
Источники, лицензии и ограничения — в `.opencode/SKILLS.md`. Правила этого файла
имеют приоритет над рекомендациями скиллов.

- Скиллы — рекомендации, а не разрешение менять стек. Не добавлять DI (Hilt/Koin),
  Robolectric, screenshot-плагины, новые зависимости, разрешения или менять
  `compileSdk`/`targetSdk` без отдельной задачи и согласования.
- `testing-setup` предлагает Hilt. В Clock DI нет: тесты остаются на JUnit4 и чистых
  функциях; UI-тесты — только по явному запросу.
- `agp-9-upgrade` применять только по отдельной задаче обновления AGP.

## Диагностика будильников

Если будильник звенит без UI или уведомления, сначала проверить системное ограничение фона:

```bash
adb shell cmd appops get com.olivka.clock RUN_ANY_IN_BACKGROUND
adb shell dumpsys alarm | grep -A4 "Restricted packages"
adb logcat -b system -d | grep "startForeground() not allowed"
```

`deny` или наличие пакета в `Restricted packages` означает, что Android отклоняет
`startForeground()`. На Motorola это делает `com.motorola.batterycare` (Smart Background),
иногда даже при выбранном «Разрешать всегда». Подробности — в `PROJECT_NOTES.md`.

# Правила проекта Clock

Персональный форк Mascot: `com.olivka.clock`, Android 8.0+ (API 26), compile/target SDK 34.
Разработка ведётся с помощью LLM-агентов: сгенерированный код и текст проверяются, собираются и тестируются.

## Языки

- Проект работает на двух основных языках: английском и русском.
- `README.md` и `changelog.md` — английский по умолчанию; `README.ru.md`
  и `changelog.ru.md` — русский.
- Внутренняя документация (`AGENTS.md`, `PROJECT_NOTES.md`, `PLAN.md`,
  `changelog.dev.md`) — на русском; комментарии в коде — на английском.
- Скиллы агентов — на английском: это инструкции для агента, а не
  пользовательская документация.

## Версии

- Нумерация строго `x.y.z` (`3.12.0`), не `x.y`.
- `versionName` — `x.y.z`; `versionCode` — целое число, монотонно растёт.
- `version.txt` — единственный источник `versionName` и `versionCode`;
  `app/build.gradle.kts` читает его при конфигурации.
- При выпуске версии синхронизировать:
  - `version.txt` — `versionCode`, `versionName`;
  - `PROJECT_NOTES.md` — текущую версию;
  - `PLAN.md` (локальный, не публикуется) — текущую версию;
  - `changelog.md` — английский заголовок `## x.y.z — YYYY-MM-DD` и список;
  - `changelog.ru.md` — тот же заголовок и русский список;
  - `changelog.dev.md` (локальный, не публикуется) — технические детали.
- `changelog.md` и `changelog.ru.md` — короткая публичная история на английском
  и русском; `changelog.dev.md` — полная техническая история для разработки.
- Исторические записи changelog не переписывать; обозначения до 3.6.0 (`3.5`, `3.4`, …) сохраняются как есть.

## Changelog

- `changelog.md` (EN) и `changelog.ru.md` (RU) показывают только изменения
  приложения: функции, поведение, UI, исправления, разрешения и данные.
- Документацию, README, скиллы агентов, CI, issue-шаблоны, релизную
  инфраструктуру и рефакторинг без изменения поведения в changelog не
  записывать.
- Changelog генерировать перед слиянием фича-ветки в `main` по диффу ветки,
  а не по каждому промежуточному коммиту. Если в ветке нет изменений
  приложения, changelog не меняется.
- `changelog.dev.md` — технические детали тех же изменений приложения;
  не-app изменения в него тоже не попадают.
- Ветка с одними документами, скиллами или CI не требует записи в changelog.

## Сборка и проверка

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Release: R8 + `shrinkResources`, `non-debuggable`, подпись локальным debug-сертификатом.
Не публиковать в Google Play.

## Релизы

- `.github/workflows/build.yml` собирает и проверяет каждый push в `main`,
  pull request и ручной запуск; APK лежит в артефактах workflow (30 дней).
- `.github/workflows/release.yml` запускается по push/merge в `main`, по тегу
  `vX.Y.Z` и вручную. На push в `main` сравнивает `version.txt` с последним
  опубликованным релизом: при совпадении `versionName` и `versionCode` сборка
  релиза пропускается; при новом `versionName` workflow создаёт тег и GitHub
  Release. Изменение только `versionCode` без нового `versionName` — ошибка.
- Релиз содержит один APK `Clock.apk` (стабильная ссылка
  `releases/latest/download/Clock.apk`), `Clock.apk.sha256` и
  `SHA256SUMS.txt`.
- Release notes собираются из секций `## X.Y.Z` файлов `changelog.md`
  и `changelog.ru.md`: сначала английский список, затем русский.
- Перед слиянием в `main`: поднять версию в `version.txt`, обновить
  документацию и changelog. После merge `release.yml` сам ставит тег `vX.Y.Z`
  и публикует релиз, если версия отличается от предыдущего.
- `./scripts/release.sh` — ручной fallback: проверяет дерево и версию, собирает
  проект, ставит annotated-тег и пушит `main` вместе с тегом; `--skip-build` —
  без локальной сборки, `--dry-run` — только проверки и сборка без тега и push.
- Секрет `ANDROID_DEBUG_KEYSTORE_BASE64` содержит текущий
  `~/.android/debug.keystore`. Не менять ключ: иначе APK перестанет
  устанавливаться поверх уже установленного приложения. При смене ключа
  обновить ожидаемый SHA-256 сертификата в `release.yml`.
- В APK нет нативных библиотек, поэтому публикуется один APK `Clock.apk`;
  ABI-сплиты не нужны.

## Документация

- Любое изменение поведения, разрешений или планирования будильников отражать в
  `README.md`, `README.ru.md`, `PROJECT_NOTES.md`, локальные `PLAN.md` и
  `changelog.dev.md`, а также в публичных `changelog.md` и `changelog.ru.md`.
- В публичных changelog попадают только изменения приложения; документация,
  скиллы, CI и релизная инфраструктура описываются во внутренних документах.
- Держать синхронными пары `README.md`/`README.ru.md` и
  `changelog.md`/`changelog.ru.md`; в шапке каждого файла — перекрёстные ссылки.
- Не добавлять `INTERNET`, аналитику, рекламу, AI/ML и лишние разрешения без явной необходимости.

## Скиллы агентов

Скиллы лежат в `.opencode/skills/` и подхватываются OpenCode автоматически.
Источники, лицензии и ограничения — в `.opencode/SKILLS.md`. Правила этого файла
имеют приоритет над рекомендациями скиллов.

- Тексты скиллов пишутся на английском — это инструкции для агента.
- Документационные скиллы: `create-readme`, `write-changelog`, `wiki-changelog`,
  `create-agentsmd`, `prepare-pull-request`. Changelog они обновляют только по
  изменениям приложения.
- `prepare-pull-request` коммитит изменения, поднимает версию и changelog, пушит
  ветку и создаёт PR; после merge релиз публикует `release.yml`, `release.sh` —
  ручной fallback.
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

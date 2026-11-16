# Clock — технические заметки

**Версия:** 3.12.0 (`versionCode 37`)
**Пакет:** `com.olivka.clock`
**Обновлено:** 2026-11-16
**Устройства:** realme RMX3871 (Android 16 / API 36), Xiaomi MI 8 Lite (Android 15 / API 35), Xiaomi 24129PN74G (Android 16 / HyperOS 3.0)
**История:** [changelog.md](changelog.md) · [changelog.ru.md](changelog.ru.md) · **Лицензии:** [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)

---

## 1. Состояние

- Release: `app/build/outputs/apk/release/app-release.apk`, ~2 МБ, R8 + `shrinkResources`, `non-debuggable`, локальная подпись.
- Debug: `app/build/outputs/apk/debug/app-debug.apk`.
- Releases: GitHub Actions по тегу `vX.Y.Z`; стабильная ссылка `releases/latest/download/Clock.apk`, версионный APK и `SHA256SUMS.txt`.
- Unit-тесты: **26**, проходят; lint — 0 errors.
- Git: `origin` — [github.com/DmitryAbramkin/Clock](https://github.com/DmitryAbramkin/Clock), ветка `main`; CI — GitHub Actions, релизы — по тегам.
- На устройстве установлен только `com.olivka.clock`; старый `com.hotaro.mascot` удалён, данные перенесены вручную.
- `.opencode/` входит в проект; скиллы и их лицензии описаны в `.opencode/SKILLS.md`.

---

## 2. Архитектура и файлы

- `namespace` / `applicationId`: `com.olivka.clock`; source root `app/src/main/java/com/olivka/clock`.
- Один модуль: Compose UI, Room, DataStore/SharedPreferences, сервисы и `AlarmManager`.
- DI нет: зависимости создаются в `StrictClockApplication`, ViewModel'и — через `viewModel()`.

| Компонент | Файлы |
|---|---|
| Activity / Application | `MainActivity.kt`, `StrictClockApplication.kt` |
| Навигация и оболочка | `ui/StrictClockApp.kt`, `ui/AppNavigation.kt` |
| Будильники | `ui/ClockDashboard.kt`, `ui/SetupAlarmScreen.kt`, `ui/ShiftScheduleEditorScreen.kt` |
| Звонок | `ui/WakeUpScreen.kt`, `service/AlarmService.kt`, `service/AlarmReceiver.kt` |
| Планирование | `service/AlarmScheduler.kt`, `utils/AlarmUtils.kt`, `service/BootReceiver.kt` |
| Таймеры | `ui/TimerScreen.kt`, `ui/TimerDetailScreen.kt`, `ui/TimerSetupScreen.kt`, `service/TimerService.kt` |
| Секундомер | `ui/StopwatchScreen.kt`, `service/StopwatchService.kt` |
| Настройки | `ui/SettingsScreen.kt`, `ui/AlarmPreferenceScreen.kt`, `ui/SoundVibrationScreen.kt`, `ui/CustomisationScreen.kt` |
| Разрешения | `ui/PermissionsScreen.kt`, `ui/PermissionsState.kt`, `utils/DeviceRestrictions.kt` |
| Ввод времени | `ui/theme/WheelTimePicker.kt`, `ui/theme/CenteredTimeInput.kt`, `ui/theme/TimeEdit.kt` |
| Общие UI-компоненты | `ui/theme/Screen.kt`, `ui/theme/Adaptive.kt`, `ui/theme/ControlButtons.kt` |
| О приложении / лицензии | `ui/AboutScreen.kt`, `ui/LicensesScreen.kt`, `res/raw/license_*.txt` |
| Данные | `data/AlarmEntity.kt`, `data/TimerEntity.kt`, `data/AppDatabase.kt`, репозитории, DAO, `data/AlarmSnapshotStore.kt` |
| Виджет | `widget/ClockWidgetProvider.kt`, `res/layout/clock_widget.xml` |
| Тесты | `app/src/test/java/com/olivka/clock/utils/AlarmUtilsTest.kt`, `TimerUtilsTest.kt` |

---

## 3. Данные

### AlarmEntity

```kotlin
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timeHour: Int,
    val timeMinute: Int,
    val daysOfWeek: String,            // Never|Daily|Mon-Fri|Sat-Sun|Mon, Wed, Fri
    val isActive: Boolean,
    val soundName: String = "Default",
    val soundUri: String = "",
    val vibrationEnabled: Boolean = true,
    val shiftEnabled: Boolean = false,
    val shiftLengthDays: Int = 4,      // 1..60
    val shiftStartDate: String = "",   // ISO yyyy-MM-dd
    val shiftSlots: String = "",       // CSV минут от полуночи; -1 = выходной
    val skipNextTriggerAt: Long = 0L,  // epoch millis одного пропуска
    val label: String = "",
    val vibrationFollowsGlobal: Boolean = true
)
```

### TimerEntity

```kotlin
@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String = "",
    val durationMs: Long,       // исходная длительность; +1 увеличивает её
    val remainingMs: Long,      // последнее сохранённое значение
    val isRunning: Boolean = false,
    val endAtMillis: Long = 0L, // wall-clock конец запущенного таймера
    val createdAt: Long = 0L,
    val hasStarted: Boolean = false,
    val soundName: String = "Default",
    val soundUri: String = "",
    val vibrationEnabled: Boolean = true,
    val vibrationFollowsGlobal: Boolean = true
)
```

- `shiftSlots`: N значений; `-1` — выходной, пример `"420,420,-1,-1"`.
- `hasStarted` отличает паузу от сброшенного таймера: у паузы прогресс есть, у нового/сброшенного — нет.
- `soundName = "Default"` + пустой `soundUri` — ссылка на глобальный звук; `vibrationFollowsGlobal = true` — ссылка на глобальную вибрацию. Конкретное значение сохраняется после ручного изменения; касание свитча вибрации считается изменением.
- `remainingMs` пишется только на переходах (старт, пауза, сброс, `+1`), а не каждую секунду. Пока таймер запущен, источник истины — `endAtMillis`.
- `TimerRepository` отдаёт `Flow<List<TimerEntity>>`; дубликаты проверяются по `durationMs` + `label` без учёта регистра и пробелов.

### Room

- Версия **13**. Миграции:
  - 7→8: `skipNextTriggerAt`;
  - 8→9: `label`;
  - 9→10: таблица `timers`;
  - 10→11: `hasStarted`;
  - 11→12: `soundName`, `soundUri`, `vibrationEnabled` таймеров;
  - 12→13: `vibrationFollowsGlobal` будильников и таймеров.
- `fallbackToDestructiveMigration()` оставлен как аварийный fallback.

### SharedPreferences

`strict_clock_prefs`: `snooze_minutes`, `no_swipe_delete_confirm`, `auto_delete_one_time`, `use_keyboard_time_input`, `wheel_haptics_enabled`, `predictive_back_enabled`, `app_language`, `show_snooze_button`, `default_alarm_sound`, `timer_sound`, `default_alarm_vibration`, `default_timer_vibration`, `timer_hour/minute/second`, `timer_alarm_active_<id>`, `keep_screen_on_timer`, `keep_screen_on_stopwatch`, `overlay_prompt_shown`, `notifications_requested`, `last_root_tab`.

`alarm_snapshot`: JSON последнего списка для мгновенного показа после запуска/смерти процесса.

---

## 4. Планирование и звонок

`AlarmUtils.getNextTriggerTime(alarm, now): Long?`

- `shiftEnabled` → расчёт по циклу; иначе `Never`/`Daily` → сегодня/завтра, `Mon-Fri`/`Sat-Sun`/custom → следующий подходящий день.
- Пустой набор дней → `null`.
- Если `skipNextTriggerAt` совпал с ближайшим срабатыванием, возвращается следующее.
- `nextShiftTrigger`: `baseDate = max(today, start)`, индекс `floorMod(daysBetween(start, baseDate), length)`, перебор `0..length`.

`AlarmScheduler`

- main `requestCode = alarm.id`; snooze `requestCode = alarm.id + 100_000`.
- `cancel` отменяет оба; `schedule` без следующего времени отменяет alarm; `scheduleSnooze` — 1–30 минут.

`AlarmService`

- Звонит и сразу планирует следующее срабатывание; one-time не перепланируется.
- Канал `alarm_channel_v3` — без звука и вибрации.
- Звук: `timer_sound` для таймера, `default_alarm_sound` для будильника со звуком «По умолчанию», иначе URI конкретного будильника.
- Уведомление публикуется через `NotificationManager.notify()` **до** `startForeground()`; если FGS отклонён из-за background restriction, уведомление и full-screen intent всё равно остаются.
- `startForeground()` обёрнут в try/catch (`ForegroundServiceStartNotAllowedException` и OEM-варианты).
- Если уведомления отключены или на Android 14+ отозван `USE_FULL_SCREEN_INTENT`, WakeUp запускается напрямую.
- `ringing: StateFlow<Boolean>` переключает Compose на WakeUp, даже если приложение уже открыто.
- `START_STICKY`-рестарт без intent сразу останавливает сервис, чтобы не звонить «фантомным» будильником.
- `setSilent(true)` не используется (подавляет full-screen intent на Android 16).
- `ensureAlarmVolume()` поднимает alarm stream до половины при 0/минимуме/mute.

`BootReceiver` — `BOOT_COMPLETED`, `USER_UNLOCKED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`; `goAsync()` + `ioScope`.
`StrictClockApplication.onCreate` перепланирует активные будильники и точные alarm запущенных таймеров как safety net.

---

## 5. Таймеры и секундомер

`TimerService`

- Обслуживает произвольное число таймеров; состояние — `TimerEntity`, в памяти рабочая копия.
- Отсчёт от `endAtMillis` (wall clock), а не вычитанием 1000 мс.
- На старте/возобновлении и `+1` ставит `setAlarmClock` с `requestCode = 900_000 + timerId`.
- Пауза, сброс и удаление отменяют alarm только этого таймера; флаг `timer_alarm_active_<id>` защищает от позднего срабатывания.
- `claimAlarm()` атомарно снимает флаг, поэтому foreground-отсчёт и `AlarmReceiver` не звонят дважды.
- Завершение запускает `AlarmService` с `IS_TIMER=true`, `TIMER_ID`, `TIMER_LABEL`.
- Каждому таймеру — отдельное уведомление `id = 2000 + timerId` с прогрессом, паузой/возобновлением, `+1` и отменой.
- Пока запущен хотя бы один таймер, сервис foreground; уведомление первого запущенного promoted.
- Канал `timer_channel_v4`: `IMPORTANCE_DEFAULT`, без звука и вибрации, `VISIBILITY_PUBLIC`, `CATEGORY_PROGRESS`.
- `onDestroy` намеренно **не** отменяет точные alarm — они страхуют от убийства процесса.

`TimerScreen` / `TimerDetailScreen` / `TimerSetupScreen`

- Список карточек: название, время, сброс, пауза/запуск, прогресс; удаление свайпом с общей настройкой подтверждения.
- `+` открывает отдельный экран `HH:MM:SS` с названием, проверкой дублей, звуком и вибрацией.
- Страница таймера: круг, сброс, пауза/запуск, `+1`, общая кнопка «Не гасить экран», заголовок «Таймер на …» без названия.
- `TimerUtils.formatRemaining()` показывает `2:30` / `1:02:03`.

`StopwatchScreen` — отдельная вкладка, круги, «Не гасить экран», канал `stopwatch_channel_v2` (`IMPORTANCE_DEFAULT` без звука) открывает вкладку.

`StrictClockApp`

- Dismiss останавливает сервис, one-time deactivate/delete, остальные перепланирует.
- Для таймера Dismiss удаляет завершённый таймер и открывает «Таймеры».
- `ACTION_RESTORE` поднимает уведомления и точные alarm запущенных таймеров.
- `SharedFlow` навигации: уведомление открывает `TimerDetail` или вкладку секундомера без пересоздания activity.
- «Не гасить экран» хранится в `keep_screen_on_timer` и `keep_screen_on_stopwatch`.

---

## 6. UI, навигация и локализация

- Четыре корневые вкладки: «Будильники», «Таймеры», «Секундомер», «Настройки»; подстраницы — `ClockScaffold` / `ClockPage`.
- Нижняя навигация — overlay-пилюля: 56 dp, сегмент 48 dp, иконки 24 dp, подпись только у выбранной вкладки, анимация 300 мс emphasized.
- Заголовки корневых разделов: 28 sp, bold, 24 dp сверху.
- `clampedSp` ограничивает масштаб заголовков, `AutoSizeText` подбирает размер цифр, `adaptiveDp` увеличивает отступы.
- `WheelTimePicker`: бесконечная прокрутка, сегментированная pill-подсветка, интерполяция стиля к центру, `NestedScrollConnection` против увода страницы. Отдача: на хаптик-актуаторе — `TextHandleMove` с троттлингом 40 мс; на обычном вибромоторе — отдельный fallback через `Vibrator` (импульс 8 мс, амплитуда 1, троттлинг 140 мс). Поддержка хаптика определяется `hasAmplitudeControl()`. `CenteredTimeInput` — клавиатурный режим с автофокусом и переходами.
- Predictive back и переходы: контент следует за жестом; root-вкладки — fade + scale 0.98.
- About: GitHub отображается как `olivka`, ведёт на `github.com/DmitryAbramkin`; ссылки на Mascot и material-clock; переход на экран «Лицензии».
- Licenses: список MIT/Apache 2.0 с раскрываемыми полными текстами из `res/raw/`.
- Виджет `ClockWidgetProvider`: 4×2, `TextClock` даты `EEE, d MMM` и времени `HH:mm`, клики в календарь и Clock.
- Локализация: `values/strings.xml` (EN), `values-ru/strings.xml` (RU); Android 13+ — `LocaleManager`, Android 8–12 — `attachBaseContext`; fallback на английский. Имя приложения — `Clock` / «Часы».

---

## 7. Сборка и release

| Параметр | Значение |
|---|---|
| OS | Arch Linux |
| JDK | OpenJDK 17 |
| Android SDK | `$HOME/Android/Sdk` |
| compile/target SDK | 34 |
| minSdk | 26 |
| Build-Tools | 34.0.0 |
| Gradle | wrapper 8.4 |

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
./gradlew clean testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Release: `isMinifyEnabled`, `isShrinkResources`, ProGuard keep `com.olivka.clock.data.**` и `com.olivka.clock.widget.**`, non-debuggable, ~2 МБ, локальная подпись. Не для Google Play.

Версия хранится только в `version.txt` (`versionCode`, `versionName`); `app/build.gradle.kts` читает его при конфигурации. Дублировать значения в Gradle не нужно.

### CI и релизы

| Workflow | Триггер | Действие |
|---|---|---|
| `.github/workflows/build.yml` | push в `main`, pull request, ручной запуск | `testDebugUnitTest`, `lintDebug`, `assembleRelease`, проверка версии в APK и подписи, APK в артефактах на 30 дней |
| `.github/workflows/release.yml` | push тега `vX.Y.Z`, ручной запуск с тегом | те же проверки, затем GitHub Release |

- Подпись в CI восстанавливается из секрета `ANDROID_DEBUG_KEYSTORE_BASE64`
  в `~/.android/debug.keystore`; это тот же ключ, что и локально, поэтому
  APK устанавливается поверх локальных сборок. Workflow сверяет SHA-256
  сертификата с ожидаемым (`F7:DA:07:...:F7:22`); при смене ключа обновить
  константу в `release.yml`.
- AGP ищет debug-keystore через `ANDROID_PREFS_ROOT` / `ANDROID_SDK_HOME`;
  workflow выставляет оба в `$HOME`, иначе runner создаёт новый ключ.
- `apksigner` из Build-Tools 37 печатает `V2 Signer: certificate SHA-256
  digest:`, а 34 — `Signer #1 certificate SHA-256 digest:`; проверка
  разбирает оба формата.
- Релиз содержит `Clock-vX.Y.Z-universal.apk`, копию `Clock.apk` для стабильной
  ссылки `releases/latest/download/Clock.apk`, `SHA256SUMS.txt` и отдельный
  `.sha256` для каждого APK.
- Release notes собираются из секций `## X.Y.Z` в `changelog.md` и
  `changelog.ru.md`: сначала английский список, затем русский; при отсутствии
  секции хотя бы в одном файле workflow падает.
- В APK нет `lib/` и `.so`, поэтому universal — единственная осмысленная
  сборка; ABI-сплиты дали бы одинаковые файлы.
- `./scripts/release.sh` проверяет чистоту дерева, ветку `main`, версию и
  оба changelog, собирает проект, ставит annotated-тег и пушит `main` вместе с
  тегом. `--skip-build` пропускает локальную сборку, `--dry-run` выполняет
  только проверки и сборку.
- Повторный запуск workflow для существующего тега перезаписывает assets
  (`gh release upload --clobber`) и notes.

---

## 8. Тесты и проверка

- 26 unit-тестов: `AlarmUtilsTest` (20) и `TimerUtilsTest` (6).
- Покрыто: daily/weekdays/weekends/custom, shift до/после старта, wrap, 60 дней, DST, encode/decode slots, skip daily/shift, duplicate schedule, формат/длительность/дубликаты таймеров.
- Вручную: release install/launch, full-screen intent, snooze/dismiss, shift, skip, Room 7→13, snapshot/вкладка, язык/даты/уведомления, виджет, несколько таймеров, уведомления и переходы, секундомер, глобальные звук/вибрация, клавиатурный/колёсный ввод, `font_scale` 1.0/1.6/2.0, плотность 440/520 dpi, панель жестов/3 кнопки, ландшафт, планшетный rail, экран разрешений.

---

## 9. Разрешения

Объявлены: `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`, `VIBRATE`, `MODIFY_AUDIO_SETTINGS`, `USE_FULL_SCREEN_INTENT`, `SYSTEM_ALERT_WINDOW` (опционально), `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (личная сборка).

Нет `INTERNET`, `CAMERA`, Device Admin и Accessibility.

`PermissionsScreen` показывает уведомления, точные будильники, полноэкранные уведомления, overlay, оптимизацию батареи (AOSP whitelist; HyperOS/MIUI — standby bucket 5), фоновую активность (`isBackgroundRestricted()` + `runAnyInBackground`) и OEM-автозапуск. Статус автозапуска через API не читается — карточка только открывает системную настройку. Ссылка на [dontkillmyapp.com](https://dontkillmyapp.com/) есть в онбординге и в блоке «Если будильник не звонит».

---

## 10. Ограничения и OEM

1. **Background restriction.** Если Clock в списке Restricted apps / «Не разрешать» (`RUN_ANY_IN_BACKGROUND=deny`), Android 12+ молча игнорирует `startForeground()`: будильник звенит без уведомления и full-screen intent. Нужно «Разрешать всегда» / «Без ограничений»; «Не оптимизировать батарею» этого не заменяет.
2. **Motorola BatteryCare.** `com.motorola.batterycare` — привилегированный пакет с AI-ограничениями, может мешать даже при «Разрешать всегда». Обратимо:
   ```bash
   adb shell pm disable-user --user 0 com.motorola.batterycare
   adb shell pm enable com.motorola.batterycare
   ```
3. **realme autostart.** После перезагрузки без запуска приложения `BootReceiver` может не выполниться; включите автозапуск.
4. **Автозапуск OEM.** Системного API для чтения статуса нет; карточка Autostart появляется только при известной activity и всегда открывает настройку.
5. **HyperOS.** Точного API для «No restrictions» нет; используется эвристика `com.miui.powerkeeper` + `UsageStatsManager.appStandbyBucket == 5`.
6. **Точные будильники.** С `USE_EXACT_ALARM` система сама добавляет приложение в allowlist; карточка показывает автоматическое разрешение.
7. **Локальная подпись.** Обновление только с тем же сертификатом; APK не для магазина.
8. **Старый package id.** `com.hotaro.mascot` — отдельное приложение; данные автоматически не переносятся.
9. **Overlay.** Опционально; без него full-screen intent обычно работает, но на OEM может быть менее стабильным.
10. **Ландшафт.** При ширине ≥ 600 dp — rail; при крупном шрифте остаются только иконки; экран звонка прокручивается.

---

## 11. Диагностика

```bash
# release
adb install -r app/build/outputs/apk/release/app-release.apk

# версия
adb shell dumpsys package com.olivka.clock | grep -E "versionName|versionCode"

# alarms / activity
adb shell dumpsys alarm | grep -n -i "com.olivka.clock"
adb shell dumpsys activity activities | grep topResumedActivity

# background restriction
adb shell cmd appops get com.olivka.clock RUN_ANY_IN_BACKGROUND
adb shell dumpsys alarm | grep -A4 "Restricted packages"
adb logcat -b system -d | grep "startForeground() not allowed"

# battery / background
adb shell dumpsys deviceidle whitelist | grep com.olivka.clock
adb shell am get-standby-bucket com.olivka.clock

# UI scale
adb shell settings put system font_scale 1.6
adb shell wm density 520
adb shell wm density reset
adb shell settings put secure navigation_mode 0   # 3 кнопки
adb shell settings put secure navigation_mode 2   # жесты

# debug DB (только debug build)
adb exec-out run-as com.olivka.clock cat databases/strict_clock_database > /tmp/db
sqlite3 /tmp/db "PRAGMA wal_checkpoint(TRUNCATE); SELECT * FROM alarms;"
```

---

## 12. Git и лицензии

- Публикация — [github.com/DmitryAbramkin/Clock](https://github.com/DmitryAbramkin/Clock); `origin` указывает на этот репозиторий. Ветка `main`.
- Релизы: [github.com/DmitryAbramkin/Clock/releases](https://github.com/DmitryAbramkin/Clock/releases); теги `vX.Y.Z`, стабильная ссылка `releases/latest/download/Clock.apk`. Репозиторий публичный, Actions бесплатны; APK подписан debug-сертификатом и не предназначен для Google Play.
- `LICENSE` MIT: уведомления Hotaro и olivka сохранены.
- Иконка основана на [material-clock](https://github.com/rbouaf/material-clock) (MIT); полные уведомления — в [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) и на экране «Лицензии».
- `local.properties`, APK/AAB, `.kotlin/` и каталог `examples/` не коммитятся.
- `.opencode/` входит в проект; источники и лицензии скиллов — в `.opencode/SKILLS.md`.

---

## 13. Документация и changelog

- Публичная история: `changelog.md` (EN) и `changelog.ru.md` (RU); в обоих
  файлах только изменения приложения. Документация, README, скиллы, CI,
  issue-шаблоны и релизная инфраструктура в changelog не попадают; они
  описываются в `AGENTS.md`, `PROJECT_NOTES.md`, `PLAN.md` и
  `.opencode/SKILLS.md`.
- Changelog генерируется перед слиянием фича-ветки в `main` по диффу ветки;
  ветка без изменений приложения changelog не трогает.
- `changelog.dev.md` — технические детали изменений приложения, локальный
  и не публикуется.
- Release notes собираются из обеих публичных версий: сначала английский
  список, затем русский.
- Пары `README.md`/`README.ru.md` и `changelog.md`/`changelog.ru.md` держатся
  синхронными; английские файлы — по умолчанию, русские — перевод.
- Внутренние документы: `AGENTS.md`, `PROJECT_NOTES.md`, `PLAN.md`,
  `changelog.dev.md`. Скиллы агентов — в `.opencode/skills/`, источники
  и лицензии — в `.opencode/SKILLS.md`; тексты скиллов на английском.

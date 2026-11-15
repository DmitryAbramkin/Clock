# Android-скиллы проекта Clock

Скиллы лежат в `.opencode/skills/`, подхватываются OpenCode автоматически и не
входят в APK. Загрузить вручную: `@<id>` или инструмент `skill`.

## Источники и лицензии

| Источник | Коммит | Лицензия |
|---|---|---|
| [android/skills](https://github.com/android/skills) | `5815d27fda564aa9f07ebdcb5e57ed3e8d904e0d` | Apache 2.0 (`skills/LICENSE-android-skills.txt`) |
| [MiniMax-AI/skills](https://github.com/MiniMax-AI/skills) | `02853d885acb1abc157477ce00c4d98a8ed14a71` | MIT (`skills/LICENSE-minimax-skills.txt`) |

Это vendored-копии без изменений, вместе с `references/`, `scripts/` и другими
вспомогательными файлами. `android skills update` их не отслеживает.

## Состав

| ID | Зачем нужен |
|---|---|
| `android-cli` | Установка APK, скриншоты, `android layout`, SDK и скиллы. |
| `jetpack-compose-performance` | Recomposition, stability, lazy-списки в Compose UI. |
| `android-permissions-security` | Точные будильники, FGS, уведомления, full-screen intent, overlay. |
| `android-intent-security` | `BootReceiver`, `PendingIntent`, виджет и full-screen intent. |
| `r8-analyzer` | Release с R8 и `shrinkResources`, keep-правила и размер APK. |
| `anr-audit-and-fix` | Будильники, foreground-сервисы и receiver'ы: main thread и ANR. |
| `testing-setup` | Расширение unit-тестов и тестовой инфраструктуры. |
| `agp-9-upgrade` | Задел на будущий переход с AGP 8.2.2 / Gradle 8.4. |
| `android-native-dev` | Справочник по Kotlin/Compose, Material 3, accessibility и адаптивности. |

## Ограничения проекта

Правила `AGENTS.md` важнее рекомендаций скиллов:

- никакого `INTERNET`, аналитики, рекламы и AI/ML;
- не публиковать в Google Play;
- не добавлять разрешения без явной необходимости;
- `compileSdk`/`targetSdk` — 34, `minSdk` — 26, Compose BOM — `2024.02.00`, AGP — 8.2.2, Gradle — 8.4;
- примеры с более новыми Compose API, Hilt, Navigation 3, Glance и т. п. сначала проверять на совместимость;
- `android-native-dev` — общий гайд, а не источник истины для этого форка.

## Намеренно не взято

`camera`, `wear`, `tv`, `cars`, `xr`, `device-ai`, `media3-cast-integration`, `identity/*`,
`play/*`, `navigation-3`, `edge-to-edge`, `adaptive`, `glance-*`,
`profilers/android-profiler`, `sentry-android-sdk`, `espresso-skill`,
`firebase-apk-scanner`.

Причины: нет соответствующих функций; проект остаётся на targetSdk 34;
навигация и edge-to-edge самописные; виджет на `RemoteViews`, а не Glance;
сеть, телеметрия и Firebase запрещены.

## Обновление

```bash
git clone --depth 1 https://github.com/android/skills.git /tmp/android-skills
git clone --depth 1 --filter=blob:none --sparse https://github.com/MiniMax-AI/skills.git /tmp/minimax-skills
git -C /tmp/minimax-skills sparse-checkout set skills/android-native-dev

for src in \
  devtools/android-cli \
  jetpack-compose/jetpack-compose-performance \
  security/android-permissions-security \
  security/android-intent-security \
  performance/r8-analyzer \
  performance/anr-audit-and-fix \
  testing/testing-setup \
  build-system/agp/agp-9-upgrade; do
  name=$(basename "$src")
  rm -rf ".opencode/skills/$name"
  cp -a "/tmp/android-skills/$src" ".opencode/skills/$name"
done

rm -rf .opencode/skills/android-native-dev
cp -a /tmp/minimax-skills/skills/android-native-dev .opencode/skills/android-native-dev
```

После обновления проверить frontmatter (`name`, `description`), обновить коммиты
в таблице и прогнать `./gradlew testDebugUnitTest lintDebug`. Отдельный скилл:
`android skills add r8-analyzer --project=.`.

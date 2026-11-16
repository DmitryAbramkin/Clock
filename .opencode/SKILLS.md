# Скиллы проекта Clock

Скиллы лежат в `.opencode/skills/`, подхватываются OpenCode автоматически и не
входят в APK. Загрузить вручную: `@<id>` или инструмент `skill`.

## Источники и лицензии

| Источник | Коммит | Лицензия |
|---|---|---|
| [android/skills](https://github.com/android/skills) | `5815d27fda564aa9f07ebdcb5e57ed3e8d904e0d` | Apache 2.0 (`skills/LICENSE-android-skills.txt`) |
| [MiniMax-AI/skills](https://github.com/MiniMax-AI/skills) | `02853d885acb1abc157477ce00c4d98a8ed14a71` | MIT (`skills/LICENSE-minimax-skills.txt`) |
| [github/awesome-copilot](https://github.com/github/awesome-copilot) | `dc0083dbe22133bfa7ce3500dee85e187e0cd47a` | MIT (`skills/LICENSE-github-awesome-copilot.txt`) |
| [microsoft/vscode-pull-request-github](https://github.com/microsoft/vscode-pull-request-github) | `1353d0b842a6fa681817e2d3517f88a34c7a97cd` | MIT (`skills/LICENSE-microsoft-vscode-pull-request-github.txt`) |
| [microsoft/skills](https://github.com/microsoft/skills) | `f03ec5392f7a0f3aca12af47477e92f1edf7b44c` | MIT (`skills/LICENSE-microsoft-skills.txt`) |

Android-скиллы — vendored-копии без изменений, вместе с `references/`,
`scripts/` и другими вспомогательными файлами. `android skills update` их не
отслеживает.

Документационные скиллы (`create-readme`, `write-changelog`, `wiki-changelog`,
`create-agentsmd`) адаптированы под конвенции проекта: английские инструкции для
агента, двуязычный README, пара `changelog.md`/`changelog.ru.md`,
`changelog.dev.md`, `version.txt` как единственный источник версии. Это не
дословные копии upstream: при обновлении правки переносятся вручную.

## Состав

### Android

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

### Документация

| ID | Зачем нужен |
|---|---|
| `create-readme` | Обновление и синхронизация `README.md` и `README.ru.md`. |
| `write-changelog` | Запись о версии приложения в `changelog.md`, `changelog.ru.md` и `changelog.dev.md` перед слиянием фича-ветки; метаданные — при релизе. |
| `wiki-changelog` | Полная история изменений приложения в обоих публичных changelog: аудит, восстановление пропусков, сводный документ. |
| `create-agentsmd` | Аудит и точечное обновление `AGENTS.md` без переписывания правил. |

Скиллы пишутся на английском: это инструкции для агента. Публичная
документация проекта — на английском и русском.

## Когда какой скилл

- «Обнови README», «добавь скриншоты в README» — `create-readme`.
- «Подготовь changelog перед слиянием фича-ветки», «добавь запись о версии» — `write-changelog`.
- «Собери полный changelog», «чего не хватает в истории» — `wiki-changelog`.
- «Проверь AGENTS.md», «актуализируй инструкции для агентов» — `create-agentsmd`.

Скиллы не публикуют релиз: тег и push делает `./scripts/release.sh` после
проверки. `wiki-changelog` не переписывает исторические записи, а
`create-agentsmd` не заменяет `AGENTS.md` шаблоном.

## Ограничения проекта

Правила `AGENTS.md` важнее рекомендаций скиллов:

- никакого `INTERNET`, аналитики, рекламы и AI/ML;
- не публиковать в Google Play;
- не добавлять разрешения без явной необходимости;
- `compileSdk`/`targetSdk` — 34, `minSdk` — 26, Compose BOM — `2024.02.00`, AGP — 8.2.2, Gradle — 8.4;
- примеры с более новыми Compose API, Hilt, Navigation 3, Glance и т. п. сначала проверять на совместимость;
- `android-native-dev` — общий гайд, а не источник истины для этого форка;
- публичные `changelog.md` и `changelog.ru.md` — только изменения приложения,
  без эмодзи и внутренних идентификаторов;
- документация, README, скиллы, CI, issue-шаблоны и релизная инфраструктура
  в changelog не попадают;
- changelog генерируется перед слиянием фича-ветки в `main`; ветка без
  изменений приложения changelog не трогает;
- release notes собираются из обоих файлов: сначала английский список, затем
  русский;
- `README.md`/`README.ru.md` и `changelog.md`/`changelog.ru.md` всегда синхронны;
- история в обеих публичных версиях не переписывается, обозначения до 3.6.0
  сохраняются.

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

Документационные скиллы обновляются вручную: сравнить upstream с локальной
версией и перенести только полезные изменения, не заменяя файл целиком.

- <https://github.com/github/awesome-copilot/tree/main/skills/create-readme>
- <https://github.com/github/awesome-copilot/tree/main/skills/create-agentsmd>
- <https://github.com/microsoft/vscode-pull-request-github/tree/main/.github/skills/write-changelog>
- <https://github.com/microsoft/skills/tree/main/.github/plugins/deep-wiki/skills/wiki-changelog>

После обновления проверить frontmatter (`name`, `description`), обновить коммиты
в таблице, выполнить `git diff --check` и прогнать
`./gradlew testDebugUnitTest lintDebug`, если менялся код. Отдельный скилл:
`android skills add r8-analyzer --project=.`.

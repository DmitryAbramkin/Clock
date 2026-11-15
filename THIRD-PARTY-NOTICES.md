# Third-party notices — Clock

Clock (`com.olivka.clock`) использует код, ресурсы и оформление перечисленных ниже
проектов. Полные тексты лицензий также доступны на экране **Лицензии** в приложении
(`ui/LicensesScreen.kt`, `res/raw/license_*.txt`).

## Mascot

- **Источник:** <https://github.com/Hotaro26/Mascot>
- **Лицензия:** MIT
- **Copyright:** Copyright (c) 2026 Hotaro
- **Используется:** базовый исходный код, структура приложения и производные материалы.
- **Изменения:** форк существенно переработан; исходное уведомление сохранено в [LICENSE](LICENSE).

Полный текст MIT для Mascot — в [LICENSE](LICENSE).

## material-clock

- **Источник:** <https://github.com/rbouaf/material-clock>
- **Лицензия:** MIT для собственного кода проекта; шрифты — отдельно по OFL 1.1.
- **Copyright:** Copyright (c) 2026 Rayane Bouafia
- **Используется:** launcher-иконка — марка из двух дисков (синий градиентный диск на бледно-голубом). В Clock она переработана в `res/drawable/ic_launcher_clock_foreground.xml`: добавлены стрелки 10:10, розовое свечение и белый фон; monochrome-слой произведён от той же марки.
- **Изменения:** да, иконка адаптирована. Остальной код material-clock не используется.

Полный текст MIT:

```text
MIT License

Copyright (c) 2026 Rayane Bouafia

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## AndroidX, Jetpack Compose, Room, Material Icons

- **Лицензия:** Apache License 2.0
- **Copyright:** The Android Open Source Project; Material Icons — Google LLC / AOSP.
- **Используется:** UI, Compose, Room, DataStore, Material Icons.
- **Полный текст:** `app/src/main/res/raw/license_apache_2_0.txt` и экран «Лицензии».

## Kotlin

- **Лицензия:** Apache License 2.0
- **Copyright:** JetBrains s.r.o.
- **Используется:** язык и стандартная библиотека.
- **Полный текст:** `app/src/main/res/raw/license_apache_2_0.txt` и экран «Лицензии».

## Примечания

- **MIT** разрешает использовать, копировать, изменять, публиковать, распространять, сублицензировать и продавать копии, в том числе коммерчески. Обязательное условие — сохранять copyright и текст разрешения в копиях или существенных частях работы. Гарантий нет.
- **Форк разрешён.** Код Mascot и material-clock распространяется по MIT, поэтому Clock может публиковаться как форк с изменениями. Уведомления обоих проектов должны оставаться в репозитории и распространяемых копиях.
- **Упоминания.** MIT не требует рекламировать авторов в интерфейсе, но требует сохранять copyright и текст лицензии. Авторы указаны в «О приложении», на экране «Лицензии» и в этом файле.
- **Иконка material-clock.** В его `LICENSE` сказано, что MIT покрывает собственный код проекта; отдельной лицензии для графики нет. Формулировка «source code only» формально может не покрывать иконку. Перед публичным релизом стоит подтвердить условия у автора или заменить иконку на собственную. Уведомление и ссылка сохранены в любом случае.
- **Шрифты material-clock.** Google Sans Flex и Noto Sans Arabic распространяются по SIL OFL 1.1, но в Clock не включены.
- **Товарные знаки.** Google, Material, Android и Pixel — товарные знаки Google LLC. Clock не связан с Google, Hotaro26 или Rayane Bouafia и не одобрен ими.

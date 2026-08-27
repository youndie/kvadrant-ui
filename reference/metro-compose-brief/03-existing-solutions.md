# Аудит существующих решений

Что уже есть в экосистеме, что можно переиспользовать, где ниша свободна.
Данные на **26 августа 2026**. `✅` — проверено чтением исходника/страницы репозитория, `⚠️` — выведено косвенно, `❓` — подтвердить не удалось.

---

## Краткий вывод

1. **Ниша Metro для Compose практически пуста.** Единственный предшественник — `louis993546/Metro-Compose`: 9 звёзд, только Android, в Maven не публикуется. Под MIT, то есть код и подход можно легально переиспользовать.
2. **Fluent-нишу занял `compose-fluent-ui`** (727★, Apache-2.0, полноценный CMP). Он не Metro и не конкурент напрямую, но это лучший образец того, как структурировать стороннюю дизайн-систему для Compose. `Pivot` там не реализован.
3. **Ни одна Windows-библиотека для Compose не имеет моста к Material.** Прецедента для нашего адаптера в Windows-мире нет; единственный годный образец адаптивного слоя в KMP — `compose-cupertino`.
4. **Pivot / Panorama придётся писать с нуля.** Ни в Compose, ни в Android-мире открытых реализаций нет. Источник точной семантики — исходники WinJS.
5. **Шрифтовой вопрос закрыт наполовину:** Segoe бандлить нельзя, Selawik под OFL можно — но **в Selawik нет кириллицы** (см. §5). Для русскоязычного UI нужен второй шрифт.

---

## 1. Fluent Design / WinUI для Compose

### 1.1 compose-fluent-ui — де-факто монополист

| Параметр | Значение |
|---|---|
| Артефакт | `io.github.compose-fluent:fluent` |
| Repo | https://github.com/compose-fluent/compose-fluent-ui ✅ |
| Внимание | старый URL `Konyaco/compose-fluent-ui` **редиректит** — проект переехал в организацию ✅ |
| Звёзды / форки | ~727 / 35 ✅ · 689 коммитов |
| Лицензия | **Apache-2.0** ✅ (иконки — `fluentui-system-icons`, MIT, копирайт Microsoft) |
| Релиз | **v0.1.0**, Maven Central, **2025-08-10** ✅; снапшоты автопубликуются с ветки `dev` |
| Таргеты | desktop (JVM), iosX64/iosArm64/iosSimulatorArm64, androidTarget, wasmJs, js ✅ |
| Самооценка авторов | «experimental, any API would be changed without notification», «lots of hard-coding and workarounds» ✅ |

**Что реализовано** ✅ — слои Mica/Layer/Card/Acrylic (поверх `chrisbanes/haze`, Apache-2.0); Button/AccentButton/SubtleButton/DropdownButton/HyperlinkButton/RepeatButton/ToggleButton/SplitButton/ToggleSplitButton/RadioButton/ToggleSwitch/CheckBox/ComboBox/ProgressBar/ProgressRing/Slider/TextField/Text/ColorPicker/RatingControl/PillButton/SegmentedButton/LiteFilter/ListItem/GridViewItem/FlipView/PipsPager; CalendarView/DateTimePicker/SideNav/TopNav/NavigationView/BreadcrumbBar/TabView/SelectorBar/Tooltip/InfoBar/Badge/MenuBar/MenuFlyout/Expander/CommandBar/CommandBarFlyout/AutoSuggestBox; FluentDialog/ContentDialog/Flyout.

**Чего нет:** `Pivot` (стоит незакрытым в чеклисте навигации ✅), File Picker, кастомный accent color, accessibility semantics, «настоящие» Window-layer Mica/Acrylic.

**Мост к Material3 — отсутствует полностью** ✅. Проверено по `fluent/build.gradle.kts`:

```kotlin
commonMain.dependencies {
    api(compose.foundation)
    api(project(":fluent-icons-core"))
    implementation(compose.uiUtil)
    implementation(libs.kotlinx.datetime)
    implementation(libs.haze)
}
```

Зависимостей на `compose.material` / `compose.material3` нет вообще; адаптерного модуля в проекте нет.

> ⚠️ **Терминологическая ловушка.** В `io.github.composefluent.background` есть классы `Material`, `MaterialContainer`, `MaterialContainerScope`, `Modifier.materialOverlay()`. Это **Fluent Materials (Mica/Acrylic)**, а не Material Design. При чтении их кода и при выборе имён у себя — не перепутать.

### 1.2 Что стоит скопировать из архитектуры FluentTheme ✅

```kotlin
@ExperimentalFluentApi @Composable
fun FluentTheme(
    colors: Colors = FluentTheme.colors,
    typography: Typography = FluentTheme.typography,
    cornerRadius: CornerRadius = FluentTheme.cornerRadius,
    useAcrylicPopup: Boolean = LocalAcrylicPopupEnabled.current,
    compactMode: Boolean = true,
    content: @Composable () -> Unit,
)
```

| Приём | Зачем нам |
|---|---|
| **Объект-аксессор** `FluentTheme.colors/typography/shapes` с `@Composable @ReadOnlyComposable` геттерами | ровно паттерн `MaterialTheme`; берём один в один |
| **`LocalColors` и прочие — `internal`**, наружу торчит только объект | не даём пользователю подменять локалы в обход API; публичны только `LocalTextStyle` и `ProvideTextStyle` |
| **`FluentThemeConfiguration`** — отдельная composable для точечного override в поддереве | тяжёлый корень (контейнеры, хосты диалогов) и лёгкий scoped-override разведены; у нас так же для смены акцента в одной секции |
| Цвета генерируются из одного accent: `lightColors(accent = Color(0xFF0078D4))` → `Colors(generateShades(accent), isDark)` | у Metro accent тоже один, но рампа не нужна — берём саму идею единственной входной точки |
| `Shapes` выводятся из `CornerRadius` через `cornerRadius.toShapes()` | у нас всё вырождается в `RectangleShape`, но структура та же |
| `staticCompositionLocalOf { lightColors() }` с рабочим дефолтом | дружелюбнее, чем `compositionLocalOf { error(...) }` у Metro-Compose |

**Типографика Fluent** ✅ — 8 стилей: caption 12/16, body 14/20, bodyStrong 14/20 SemiBold, bodyLarge 18/24, subtitle 20/28 SemiBold, title 28/36 SemiBold, titleLarge 40/52 SemiBold, display 68/92 SemiBold. **`fontFamily` не задан нигде** — используется платформенный дефолт, шрифт не бандлится. Нам так нельзя: без Segoe-подобного шрифта Metro не читается как Metro.

### 1.3 Альтернатив нет
⚠️ Поиск по GitHub topics `fluent-ui?l=kotlin`, `jetpack-compose-fluent` и по `kmp-awesome` второй сопоставимой библиотеки не выявил.

---

## 2. Metro для Compose

### 2.1 Metro-Compose — единственный предшественник

| Параметр | Значение |
|---|---|
| Repo | https://github.com/louis993546/Metro-Compose ✅ |
| Автор | Louis Tsai — тот же, что сделал Compose95 (Windows 95 на Compose) ✅ |
| Звёзды / форки | **~9 / 5** ✅ · 730 коммитов |
| Лицензия | **MIT** ✅ |
| Таргеты | **только Android** ✅ (`com.android.library`, namespace `com.louis993546.metro`, JVM 21) |
| Публикация | **нет** ✅ — блок `publishing` закомментирован; в README: «GH начал брать деньги за packages, публикация отключена» |
| Активность | ❓ точную дату получить не удалось (GitHub отдаёт её через JS). Косвенно: Compose **BoM 2024.11.00** в README ⚠️ |
| Material interop | **нет** ✅ — зависимости только `androidx.compose.ui:ui` + `foundation` |

**Полный публичный API** ✅ (из `metro/api/metro.api`, дамп binary-compatibility-validator):

| Файл | Что даёт |
|---|---|
| `ApplicationBarKt` | `ApplicationBar` (2 перегрузки) |
| `ButtonsKt` | `Button(modifier, text)`, `CircleButton(modifier, content)` |
| `FontFamilyKt` | `fontFamily` — **Selawik** |
| `ListViewKt` | `ListView(modifier, items)`, `AcronymIcon`, `ListViewHeaderAcronymStyle` |
| `ListViewItem` | `Header(contents, key, label)` / `Content(...)` — заготовка jump-list |
| `MessageBoxKt` | `MessageBox(modifier, onDismiss, content)` |
| `MetroColor$Companion` | **20 акцентов WP**: amber, brown, cobalt, crimson, cyan, emerald, green, indigo, lime, magenta, mauve, olive, orange, pink, red, steel, taupe, teal, violet, yellow |
| `MetroThemeKt` | `MetroTheme(darkTheme, accentColor, content)` + 6 CompositionLocals |
| `PagesKt` | **`Pages(modifier, items, content)` + `TitleBar(modifier, text)`** — заготовка Pivot |
| `TextFieldKt` | `TextField(modifier, value, onValueChange)` |
| `TextKt` | `Text(...)`, `Disclaimer(...)` |
| `TileKt` | `Tile(modifier, content)` |

**Тема** ✅ — плоская модель без объекта-аксессора:

```kotlin
val LocalAccentColor = compositionLocalOf<Color> { error("No accent color found!") }
// + LocalBackgroundColor / LocalTextOnAccentColor / LocalTextOnBackgroundColor
// + LocalButtonColor / LocalTextOnButtonColor

@Composable
fun MetroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Color,
    content: @Composable () -> Unit,
) {
    val backgroundColor      = if (darkTheme) Color.Black else Color.White
    val textOnBackgroundColor = if (darkTheme) Color.White else Color.Black
    val textOnAccentColor     = accentColor.textColor()          // по luminance >= 0.5
    val buttonColor           = if (darkTheme) Color(0xFF1F1F1F) else Color(0xFFDDDDDD)
    ...
}
```

Что забираем и что меняем:

| Их решение | Наше |
|---|---|
| 6 плоских токенов | сохраняем минимализм, но добавляем subtle/disabled/border/inactive/contrast — они есть в оригинальном WP и без них не собрать ни один контрол |
| фон — абсолютные `Color.Black` / `Color.White` | **сохраняем** (OLED-чёрный — часть идентичности Metro), но light-foreground берём `#DE000000`, а не чистый чёрный — как в оригинале |
| контраст на акценте по luminance ≥ 0.5 | сохраняем как дефолт + override-словарь (правило переводит в чёрный только `yellow`, а `lime`/`amber` остаются с контрастом ~2.2:1) |
| `compositionLocalOf { error(...) }` | меняем на `staticCompositionLocalOf` с рабочим дефолтом |
| `LocalIndication provides TiltIndication()` **закомментирован** — не доделан | это наша фича №1, реализуем полностью |

**Чего у них нет:** Panorama, настоящий Pivot с параллаксом заголовка, live-плитки с флипом, ProgressBar-«точки», Toggle / Slider / CheckBox / RadioButton, DatePicker, ContextMenu.

### 2.2 Другого Metro для Compose не существует
⚠️ Проверено поиском по topics `metro-ui`, `windows-phone?l=kotlin`, `jetpack-compose`.

---

## 3. Metro в других фреймворках — как источники семантики

| Проект | URL | ★ | Лицензия | Ценность для нас |
|---|---|---|---|---|
| **fluent_ui** (Flutter) | bdlukaa/fluent_ui | ~3.5k ✅ | **BSD-3** ✅ | **Лучший референс по объёму и позиционированию.** WinUI3, не Metro. Flutter Favorite, 2511 коммитов, v4.15.1, 30+ локализаций. Позиционируется как «третья дизайн-система рядом с Material и Cupertino» — ровно наша рамка. Отдельного Material-адаптера нет: сосуществует через собственные `FluentApp` / `FluentThemeData` |
| **WinJS** | winjs/winjs | ❓ | ⚠️ Apache-2.0 | **Самый ценный источник семантики.** Оригинальные MS-реализации `Pivot`, `Hub`, `ListView`, `SemanticZoom`, `FlipView`, `AppBar` с настоящими таймингами и жестами. ⚠️ Maintenance mode: стабильный 4.4.5 (янв 2019), `winjs-bower` заархивирован 15.07.2023 |
| **Metro UI (v5)** | olton/metroui | ~7.1k ✅ | **MIT** ✅ | Очень активен (6640 коммитов, © 2012-2025 Serhii Pimenov), но ⚠️ **к v5 дрейфанул в сторону универсального «Bootstrap replacement»** и от строгого Metro-канона. Есть платный SUPPORT PACK и CLA для PR |
| Metro-UI-CSS v4 / v3 / v2 | olton/Metro-UI-CSS-4, -3, -2 ✅ | — | MIT | **Ближе к оригинальному канону**, чем v5. Docs: v4.metroui.org.ua, v2.metroui.org.ua |
| Metro4-React | olton/Metro4-React | ❓ | ⚠️ вероятно MIT | Официальный React-порт Metro 4, ⚠️ похоже заброшен. Docs: react.metroui.org.ua |
| react-metroui | tscommunity/react-metroui | ❓ | ❓ | Metro-UI-CSS на React + TS, метрики не проверены |
| metrodesign.js | hydroperx/metrodesign.js | ❓ | ❓ | ❓ URL не открылся — возможно переименован/удалён |
| MetroFramework / MetroSet-UI / MetroBlazor | thielj/MetroFramework, 1someone1/MetroBlazor | ❓ | ❓ | Полезны как **визуальные** референсы Win8 (тайлы, live tiles, drag-sort групп), парадигма не декларативная |
| **SwiftUI Metro** | — | — | — | ❌ **Не существует.** Ни одной заметной библиотеки |

---

## 4. Образец адаптерного слоя: compose-cupertino

Не Windows, но это **ровно та задача, что у нас в §6 архитектуры** — второй дизайн-язык рядом с Material в одном KMP-проекте.

Repo: https://github.com/alexzhirkevich/compose-cupertino ✅, артефакт `io.github.alexzhirkevich:cupertino-adaptive`.

```kotlin
AdaptiveTheme(
    material  = { MaterialTheme(content = it) },
    cupertino = { CupertinoTheme(content = it) },
    target = theme,
    content = content,
)

// примитив для своих компонентов
AdaptiveWidget(
    material  = { /* Material-ветка  */ },
    cupertino = { /* Cupertino-ветка */ },
)
```

Плюс готовый набор `AdaptiveButton`, `AdaptiveSwitch`, `AdaptiveSlider`, `AdaptiveScaffold`, `AdaptiveTopAppBar`, `AdaptiveDatePicker`, `AdaptiveDialog`, `AdaptiveNavigationBar`, `AdaptiveDivider`, `AdaptiveCircularProgressIndicator`.

**Модель «два лямбда-слота + target-энум + `AdaptiveWidget` как базовый примитив» переносится на нашу пару MetroTheme/MaterialTheme напрямую.** Это единственный найденный работающий прецедент в KMP.

---

## 5. Шрифты

### 5.1 Segoe UI / Segoe WP — бандлить нельзя ⚠️

*(несколько независимых источников; юридического заключения не запрашивалось)*

- EULA Microsoft запрещает копирование, редистрибуцию и реверс-инжиниринг файлов шрифта; копии не могут распространяться коммерчески ни отдельно, ни в составе продукта.
- Разрешено использование Segoe и иконочных шрифтов **только для разработки и тестирования программ, работающих на Microsoft Platform**.
- Коммерческая лицензия на **Segoe UI** доступна через **Monotype**; **Segoe UI Variable** не лицензируется вне продуктов Microsoft и на не-Windows платформах.
- Практический прецедент: issue #23 в `telegramdesktop/tdesktop` — «EULA violation: Segoe UI font».

**Вывод:** в KMP-библиотеку класть нельзя ни для Android, ни для desktop/iOS/web. Единственный легальный путь на Windows — подхватывать системный шрифт по имени, не бандля файл.

То же касается **Segoe MDL2 Assets / Segoe Fluent Icons** — иконочный набор придётся рисовать самим (~120 глифов, см. roadmap).

### 5.2 Selawik — правильная, но неполная замена

| Параметр | Значение |
|---|---|
| Repo | https://github.com/microsoft/Selawik ✅ |
| Лицензия | **SIL OFL 1.1** ✅ |
| Автор | Microsoft ✅ · «Selawik is an open source replacement for Segoe UI» |
| Метрическая совместимость | ✅ metrics-compatible с Segoe UI — переносы строк и интерлиньяж сохраняются при подмене |
| Начертания | `selawkl` ExtraLight, `selawksl` Light, `selawk` Regular, `selawksb` SemiBold, `selawkb` Bold ✅ — Light и SemiBold, критичные для Metro, на месте |
| Активность | ⚠️ **заморожен**: 6 коммитов, единственный релиз с бинарниками — **1.01, 8 декабря 2015** ✅ |
| Известные проблемы (README) | ✅ «missing kerning to match Segoe UI», «needs improved hinting» |

> ### 🚨 Selawik не содержит кириллицы — REFUTED, это блокер
>
> Microsoft Typography, страница Selawik: `Script Tags: dlng:'Latn' slng:'Latn'`, code pages — **только 1252 / 1250 / 1254 / 1257**. Кодовой страницы **1251 (Cyrillic) нет**.
> Font Library, анализируя сами TTF: «Full Language Support: Baltic, Basic Latin, Catalan, Central European, Dutch, Euro, Romanian, Turkish, Western European» — ни кириллицы, ни греческого ни в одном начертании.
>
> **Следствие:** русскоязычный UI на одном Selawik невозможен. Нужен либо второй шрифт в fallback-стеке (Inter / Noto Sans — оба OFL, оба с полной кириллицей), либо расширение самого Selawik (он под OFL — форк и дорисовка легальны, но это отдельный шрифтовой проект).
>
> **Рекомендуемый стек:** `Selawik → Inter → Noto Sans → platform default`.
> **Риск:** Selawik и Inter метрически несовместимы, поэтому русский и английский текст в одном интерфейсе будут иметь слегка разный ритм. Решение принимать до старта — см. открытый вопрос №1.

**Готовый прецедент бандлинга** ✅ — Metro-Compose делает ровно это (проект MIT, шрифт OFL, приложение опубликовано в Play Store):

```kotlin
val fontFamily = FontFamily(
    Font(resId = R.font.selawkl,  weight = FontWeight.ExtraLight),
    Font(resId = R.font.selawksl, weight = FontWeight.Light),
    Font(resId = R.font.selawk),
    Font(resId = R.font.selawksb, weight = FontWeight.SemiBold),
    Font(resId = R.font.selawkb,  weight = FontWeight.Bold),
)
```

Для CMP это переписывается на `org.jetbrains.compose.resources.Font(Res.font.selawk, …)` — см. `04-architecture.md`, §4.

### 5.3 Fallback-кандидаты ⚠️ (Wikipedia + Font Squirrel, не первоисточники)

| Шрифт | Лицензия | Пригодность |
|---|---|---|
| **Inter** | SIL OFL 1.1 (двойное OFL+Apache снято в 2020 после релицензирования Roboto) | Хороший fallback, полная кириллица, variable. Метрически **не** совместим с Segoe |
| **Open Sans** | SIL OFL (до марта 2021 — Apache 2.0 ⚠️, старые копии могут быть под Apache) | Нейтральный гротеск, кириллица есть, метрики другие |
| **Noto Sans** | SIL OFL 1.1 (в 2013–2015 был Apache) | Максимальное покрытие Unicode — лучший «страховочный» последний фолбэк |

---

## 6. Итоговая таблица «переиспользовать / переписать»

| Что | Откуда взять | Действие |
|---|---|---|
| Структура темы (объект-аксессор, internal-локалы, scoped-config) | compose-fluent-ui, Apache-2.0 | **скопировать паттерн** |
| Палитра 20 акцентов WP | Metro-Compose, MIT | **скопировать значения** (перепроверив по §4.2 спеки) |
| `contrastOn(accent)` по luminance | Metro-Compose, MIT | скопировать + добавить override-словарь |
| Адаптивный слой Metro↔Material | compose-cupertino, модель `AdaptiveTheme`/`AdaptiveWidget` | **скопировать модель**, реализовать самим |
| Семантика Pivot / Hub / SemanticZoom / AppBar | WinJS, Apache-2.0 | **читать как спецификацию**, писать с нуля |
| Тайминги и кривые моушна | WindowsPhoneToolkit + WinJS Animations.js | значения уже извлечены → `02-metro-spec.md` §5 |
| Tilt effect | `TiltEffect.cs` (WindowsPhoneToolkit) | формулы извлечены, реализация с нуля на `IndicationNodeFactory` |
| Шрифт | Selawik (OFL) + Inter (OFL) | бандлить оба, стек фолбэков |
| Иконки | ничего готового; Segoe MDL2 копировать нельзя | **рисовать с нуля**, ~120 глифов |
| Panorama, live-плитки, ProgressBar-точки, ToggleSwitch, ListPicker | нигде нет | **писать с нуля** по числам из `02-metro-spec.md` |

---

## Источники

**Compose / Kotlin**
- https://github.com/compose-fluent/compose-fluent-ui · [releases](https://github.com/compose-fluent/compose-fluent-ui/releases) · [FluentTheme.kt](https://raw.githubusercontent.com/compose-fluent/compose-fluent-ui/master/fluent/src/commonMain/kotlin/io/github/composefluent/FluentTheme.kt) · [Typography.kt](https://raw.githubusercontent.com/compose-fluent/compose-fluent-ui/master/fluent/src/commonMain/kotlin/io/github/composefluent/Typography.kt) · [build.gradle.kts](https://raw.githubusercontent.com/compose-fluent/compose-fluent-ui/master/fluent/build.gradle.kts) · [TODO.md](https://raw.githubusercontent.com/compose-fluent/compose-fluent-ui/master/TODO.md) · [Maven Central](https://repo1.maven.org/maven2/io/github/compose-fluent/fluent/) · [docs](https://compose-fluent.github.io/compose-fluent-ui/)
- https://github.com/louis993546/Metro-Compose · [metro.api](https://raw.githubusercontent.com/louis993546/Metro-Compose/main/metro/api/metro.api) · [MetroTheme.kt](https://raw.githubusercontent.com/louis993546/Metro-Compose/main/metro/src/main/java/com/louis993546/metro/MetroTheme.kt) · [FontFamily.kt](https://raw.githubusercontent.com/louis993546/Metro-Compose/main/metro/src/main/java/com/louis993546/metro/FontFamily.kt) · [build.gradle.kts](https://raw.githubusercontent.com/louis993546/Metro-Compose/main/metro/build.gradle.kts) · [страница проекта](https://www.louis993546.com/metro-compose/)
- https://github.com/alexzhirkevich/compose-cupertino · [docs/Adaptive.md](https://raw.githubusercontent.com/alexzhirkevich/compose-cupertino/master/docs/Adaptive.md)
- https://github.com/chrisbanes/haze · https://github.com/terrakok/kmp-awesome

**Flutter / Web / .NET**
- https://github.com/bdlukaa/fluent_ui · https://pub.dev/packages/fluent_ui · https://bdlukaa.github.io/fluent_ui/
- https://github.com/olton/metroui · https://v5.metroui.org.ua/ · https://v4.metroui.org.ua/ · https://v2.metroui.org.ua/ · https://github.com/olton/Metro-UI-CSS-4 · https://github.com/olton/Metro4-React · https://github.com/tscommunity/react-metroui
- https://github.com/winjs/winjs · https://en.wikipedia.org/wiki/WinJS
- https://github.com/thielj/MetroFramework · https://github.com/1someone1/MetroBlazor · https://github.com/JakeWharton/Android-DirectionalViewPager

**Шрифты**
- https://github.com/microsoft/Selawik · https://learn.microsoft.com/en-us/typography/font-list/selawik · https://fontlibrary.org/en/font/selawik · https://github.com/microsoft/fonts · https://learn.microsoft.com/en-us/typography/fonts/font-faq
- https://github.com/telegramdesktop/tdesktop/issues/23 · https://blog.saif71.com/segoe-ui-font-licensing/
- https://en.wikipedia.org/wiki/Inter_(typeface) · https://en.wikipedia.org/wiki/Open_Sans · https://en.wikipedia.org/wiki/Noto_fonts

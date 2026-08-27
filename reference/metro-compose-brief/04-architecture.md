# MetroTheme — архитектура библиотеки

Дизайн-система Metro (Windows Phone 8 / Windows 8) для Compose Multiplatform + слой-адаптер к `androidx.compose.material3`.

Документ — технический бриф уровня «можно начинать писать код». Все числовые токены — из первоисточников (WP8 theme resources, Windows 8 typography/grid guidelines, `TiltEffect.cs` из Windows Phone Toolkit, WinJS animation library).

> **Статус проверки версий.** Пункты, помеченные в тексте **`[проверить]`**, продублированы в §10. Часть из них уже закрыта отдельной верификацией — результаты ниже.

### Результаты верификации помеченных пунктов

| Пункт | Вердикт |
|---|---|
| Кириллица в Selawik | 🚨 **REFUTED — кириллицы нет.** `Script Tags: dlng:'Latn'`, code pages 1252/1250/1254/1257; 1251 отсутствует. Блокер, см. §4.4 |
| `Font(Res.font.x, weight, style)` из `org.jetbrains.compose.resources`, `@Composable` | ✅ **VERIFIED** для CMP 1.12. Доки прямо предупреждают: раз `Font` — composable, то `TextStyle` и `Typography` тоже должны быть composable |
| Состав `ColorScheme` | ✅ **VERIFIED: 48 ролей.** Но `surfaceContainer*` и 12 fixed-ролей — **не новинка 1.5-alpha**: старый конструктор помечен `@Deprecated("Use constructor with additional 'fixed' container roles.")` ещё с 1.3.0. В 1.5-alpha изменились `MotionScheme` и ripple, а не `ColorScheme` |
| `RippleThemeConfiguration` / `LocalRippleConfiguration` | ✅ **VERIFIED.** `RippleThemeConfiguration(focus: Focus)` + `LocalRippleThemeConfiguration` — уровень темы; `LocalRippleConfiguration: ProvidableCompositionLocal<RippleConfiguration?>` — per-subtree, **отключение = `provides null`**. Новая сигнатура `ripple(bounded, radius, color, focusRingShape, enablePressIndication, enableFocusIndication, enableHoverIndication, enableDragIndication)`. `rememberRipple` — `DeprecationLevel.ERROR` |
| `IndicationNodeFactory` + `LayoutModifierNode` | ✅ **VERIFIED теоретически.** `IndicationNodeFactory` — стабильный `@Stable interface` без opt-in. `Modifier.indication` оборачивает ноду в приватный `IndicationModifierNode : DelegatingNode()`, а `DelegatingNode.delegate()` пересоздаёт координатор при `Nodes.Layout in delegatedKindSet`. `AbstractClickableNode` сам `LayoutModifierNode` не реализует и layout-делегатов не имеет — **слот свободен**. Ограничение: «Delegating to multiple LayoutModifierNodes without the delegating node implementing LayoutModifierNode itself is not allowed». Практическую проверку всё равно делаем спайком |
| Roborazzi для не-Android CMP | ✅ **VERIFIED, но экспериментально** — в README есть «Experimental feature: iOS support» и «Experimental feature: Compose Desktop support» |
| Paparazzi | только Android, ноль упоминаний multiplatform |
| Официальное решение для CMP | ❌ нет. Google Compose Preview Screenshot Testing, Known issues, дословно: «Both the IDE and the underlying plugin are engineered exclusively for Android projects. They don't support non-Android targets in KMP projects» |
| `@Preview` в commonMain | ⚠️ **частично REFUTED.** С CMP 1.10 канон — `androidx.compose.ui.tooling.preview.Preview`; `org.jetbrains.compose.ui.tooling.preview.Preview` **deprecated**. Зависимость `org.jetbrains.compose.ui:ui-tooling-preview:1.12.0`. Какие IDE рендерят common-превью — **UNKNOWN**, проверить руками |
| `abiValidation` в Kotlin 2.4 | ⚠️ **REFUTED как «стабильный».** DSL работает (`checkKotlinAbi`/`updateKotlinAbi`, `filters {}`, `binariesSource`), но в API-референсе KGP 2.4.0 аннотация `ExperimentalAbiValidation` на месте: «no compatibility guarantees». Брать можно, с `@OptIn` |
| Системный акцент Windows из JVM | ✅ **VERIFIED.** Реестр через JNA (`Advapi32Util.registryGetIntValue`). **`AccentColor` лежит под `DWM`**, не под `Explorer\Accent`, формат **ABGR** (`COLORREF = 0x00BBGGRR`); `ColorizationColor` — ARGB, но старший байт не альфа, его надо заменить на `0xFF`. Лучший источник — `AccentPalette` (REG_BINARY, 8×4 байта, вся рампа Light3→Dark3). Готовой библиотеки нет: jSystemThemeDetector и skiko дают только dark/light, FlatLaf accent не читает. Готча из Chromium: «Windows will set unsupported accent color values in the registry, while coercing the value to another color. Use the UISettings API to ensure we are getting the coerced color to match» |

---

## 1. Версионная база и риски

### 1.1 Матрица версий

| Артефакт | Версия | Статус | Роль в проекте |
|---|---|---|---|
| Kotlin | 2.4.0 | stable | язык + встроенный Compose-компилятор (`org.jetbrains.kotlin.plugin.compose`) |
| `org.jetbrains.compose` (Gradle plugin) | 1.12.0 | stable | сборка CMP, ресурсы, таргеты |
| `org.jetbrains.compose.runtime/foundation/ui` | 1.12.0 | stable | **единственная** обязательная зависимость `metro-core` |
| `org.jetbrains.compose.components:components-resources` | 1.12.0 | stable | бандлинг шрифта Selawik |
| `org.jetbrains.compose.material3:material3` | 1.12.0 | stable | база `androidx` M3 **1.4.x** — целевая база адаптера |
| `org.jetbrains.compose.material3:material3` | 1.12.0-alpha03 | **alpha** | собран из `androidx` M3 **1.5.0-alpha22** — только для экспериментальной ветки |
| `androidx.compose.material3:material3` | 1.4.x | stable | Android-only потребители адаптера |
| `androidx.compose.material3:material3` | 1.5.0-alpha22 | **alpha** | источник новых API (см. 1.2) |
| `dev.drewhamilton.poko` / `binary-compatibility-validator` | см. §9 | — | публичный API-контракт |

### 1.2 Что появилось только в M3 1.5.0-alpha и почему это риск

| API | Что даёт | Риск |
|---|---|---|
| `MaterialTheme.Values` | агрегатор токенов темы вместо россыпи `MaterialTheme.colorScheme/typography/shapes` | сигнатура ещё меняется между alpha; ломает бинарную совместимость адаптера |
| `LocalMaterialTheme` | единый composition local всей темы | заменяет 4 отдельных локала → адаптер на нём не соберётся против 1.4.x |
| `RippleThemeConfiguration` | переименование/расширение `LocalRippleConfiguration` | **прямо ломает** ключевой для нас приём «выключить ripple глобально» |
| `MotionScheme` | стандартизованные spec'и анимаций | заманчиво для маппинга `MetroMotion`, но API нестабилен |
| `Shapes.largeIncreased`, `extraLargeIncreased` | новые слоты форм | +2 слота, которые надо занулить в `RectangleShape`; отсутствуют в 1.4 |

Практический вывод: **`metro-material-adapter` компилируется против M3 1.4.x**, а всё, что живёт только в 1.5-alpha, изолируется в отдельном source set / отдельном артефакте.

### 1.3 Стратегия пиннинга

1. **`metro-core` не зависит от Material вообще.** Только `runtime`, `foundation`, `ui`, `ui-text`, `components-resources`. Это главное архитектурное решение — оно снимает 90 % версионных рисков (так же поступает `compose-fluent-ui`).
2. **Version catalog** (`gradle/libs.versions.toml`) — единственный источник версий; никаких строковых координат в build-скриптах.
3. **Адаптер объявляет Material как `api` с `strictly`-диапазоном**, чтобы потребитель не подтянул 1.5-alpha случайно:
   ```kotlin
   // metro-material-adapter/build.gradle.kts
   commonMain.dependencies {
       api(project(":metro-core"))
       api(compose.material3) {                       // 1.12.0 → androidx M3 1.4.x
           version { strictly("[1.12.0, 1.13.0)"); prefer("1.12.0") }
       }
   }
   ```
4. **Две ветки адаптера при необходимости:** `metro-material-adapter` (1.4.x, релизная) и `metro-material-adapter-next` (1.5-alpha, помечена `@ExperimentalMetroApi`, публикуется отдельными версиями `-next.N`). Общий код — в `metro-material-adapter-common`, различия — в тонком слое `internal expect/actual`-подобных шимов (обычными интерфейсами, не `expect`).
5. **Compose Compiler = версии Kotlin.** С Kotlin 2.x компилятор Compose поставляется плагином из KGP; отдельной версии не пиним.
6. **`kotlin.compose.stabilityConfigurationFile`** — добавить `MetroColors/MetroTypography/...` в конфиг стабильности не нужно (они `@Immutable`), но файл завести заранее для сторонних типов.

---

## 2. Модель темы MetroTheme

### 2.1 Общая форма (по образцу `compose-fluent-ui`)

```
MetroTheme(colors, typography, metrics, motion) { ... }   ← composable-провайдер
MetroTheme.colors / .typography / .metrics / .motion      ← объект-аксессор
MetroThemeConfiguration(...) { ... }                      ← scoped-override поддерева
LocalMetroColors и др.                                    ← internal, наружу не торчат
```

Правило: **локалы `internal`, наружу — только объект-аксессор.** Это позволяет позже сменить механику (например, перейти на один `LocalMetroTheme`) без слома публичного API.

### 2.2 `staticCompositionLocalOf` vs `compositionLocalOf`

| Критерий | `staticCompositionLocalOf` | `compositionLocalOf` |
|---|---|---|
| Чтение | без записи в snapshot-observer, дешевле | регистрируется как state-read |
| Изменение значения | **инвалидирует всё поддерево провайдера** | инвалидирует только фактических читателей |
| Стоимость при частых изменениях | высокая | низкая |
| Стоимость при редких изменениях | нулевая | небольшой постоянный оверхед на каждом чтении |

Тема меняется редко (смена dark/light, смена акцента в настройках) и читается очень часто (каждый `Text`, каждый `Surface`). Поэтому:

**Решение: все четыре локала — `staticCompositionLocalOf`.**

Оговорка: если понадобится **анимировать** акцент (плавный переход между accent-пресетами), при `static`-локале каждый кадр анимации перестраивает всё поддерево. Два выхода:
- (a) анимировать не в теме, а точечно: `animateColorAsState(MetroTheme.colors.accent)` в конкретном компоненте;
- (b) перейти на паттерн Material3: `@Stable class` с полями `by mutableStateOf(...)` + `updateFrom()` — тогда `static`-локал остаётся, а перерисовываются только реальные читатели поля.

По умолчанию берём (a) + `@Immutable data class` — это даёт бесплатные `copy()`/`equals()`, что важно для `MetroThemeConfiguration` и для тестов. Вариант (b) держим в уме как оптимизацию.

### 2.3 Data-классы

```kotlin
// metro-core/src/commonMain/kotlin/.../MetroColors.kt
package io.metro.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class MetroColors(
    /** Системный/выбранный акцент. */
    val accent: Color,
    /** Абсолютный фон страницы: чёрный в dark, белый в light. */
    val background: Color,
    /** Фон «хрома»: панели приложения, всплывашки, контекстные меню. */
    val chrome: Color,
    /** Основной текст/иконки на background. */
    val foreground: Color,
    /** Вторичный текст (WP: PhoneSubtleBrush). */
    val subtleForeground: Color,
    /** Disabled-состояние. */
    val disabledForeground: Color,
    /** Текст/иконки поверх accent (см. contrastOn). */
    val contrastForeground: Color,
    /** Цвет рамок контролов (WP border thickness = 2px → 1.5.dp). */
    val border: Color,
    /** Неактивный элемент (невыбранная точка Pivot, off-состояние ToggleSwitch). */
    val inactive: Color,
    /** Полупрозрачная подложка (WP: PhoneSemitransparentBrush). */
    val semitransparent: Color,
    val isDark: Boolean,
)
```

```kotlin
@Immutable
data class MetroTypography(
    // --- Windows Phone ramp (основной набор) ---
    val small: TextStyle,
    val normal: TextStyle,
    val medium: TextStyle,
    val mediumLarge: TextStyle,
    val large: TextStyle,
    val extraLarge: TextStyle,
    val extraExtraLarge: TextStyle,
    val huge: TextStyle,
    // --- Семантические алиасы WP ---
    val subtle: TextStyle,        // normal + subtleForeground
    val title1: TextStyle,        // extraExtraLarge, заголовок страницы (Pivot/Panorama title)
    val title2: TextStyle,        // large
    val title3: TextStyle,        // mediumLarge
    val groupHeader: TextStyle,   // medium, SemiBold, uppercase
    val accent: TextStyle,        // normal, цвет = accent
    val contrast: TextStyle,      // normal, цвет = contrastForeground
    // --- Windows 8 ramp (для десктопных/планшетных лейаутов) ---
    val win8: Win8Typography,
)

@Immutable
data class Win8Typography(
    val header: TextStyle,
    val subheader: TextStyle,
    val title: TextStyle,
    val subtitle: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
)
```

```kotlin
@Immutable
data class MetroMetrics(
    /** Коэффициент перевода исходных Metro-пикселей в dp/sp. См. §4.1. */
    val scale: Float = 0.75f,

    // Windows 8 grid
    val gridUnit: Dp = 15.dp,          // 20 px
    val gridSubUnit: Dp = 3.75.dp,     // 5 px
    val pageLeftMargin: Dp = 90.dp,    // 120 px
    val headerBaseline: Dp = 75.dp,    // 100 px от верха

    // Windows Phone
    val contentMargin: Dp = 9.dp,      // ContentPanel Margin="12,0,12,0"
    val appBarHeight: Dp = 54.dp,      // ApplicationBar 72 px
    val statusBarHeight: Dp = 24.dp,   // 32 px
    val borderThickness: Dp = 1.5.dp,  // WP border 2 px
    val touchTargetMin: Dp = 48.dp,    // не Metro, но обязательный минимум a11y

    val baselineGrid: Dp = 15.dp,      // Win8 20 px — шаг для lineHeight
)
```

```kotlin
@Immutable
data class MetroMotion(
    /** Главная кривая Metro (Windows 8). */
    val standardEasing: Easing = CubicBezierEasing(0.1f, 0.9f, 0.2f, 1f),
    val linear: Easing = LinearEasing,

    val fadeInMillis: Int = 250,
    val fadeOutMillis: Int = 167,

    val pointerDownMillis: Int = 167,
    val pointerDownScale: Float = 0.975f,

    val pageEnterMillis: Int = 1000,
    val pageEnterOffset: Dp = 21.dp,      // 28 px
    val pageEnterStaggerMillis: Int = 83,
    val pageEnterStaggerCapMillis: Int = 333,

    // TiltEffect.cs
    val tiltMaxAngleRad: Float = 0.3f,    // 17.188°
    val tiltMaxDepression: Dp = 18.75.dp, // 25 px
    val tiltReturnDelayMillis: Int = 200,
    val tiltReturnMillis: Int = 100,
)
```

### 2.4 Провайдер, локалы и объект-аксессор

```kotlin
// metro-core/src/commonMain/kotlin/.../MetroTheme.kt
package io.metro.theme

import androidx.compose.runtime.*

internal val LocalMetroColors = staticCompositionLocalOf<MetroColors> {
    error("MetroColors not provided. Wrap your content in MetroTheme { }.")
}
internal val LocalMetroTypography = staticCompositionLocalOf<MetroTypography> {
    error("MetroTypography not provided. Wrap your content in MetroTheme { }.")
}
internal val LocalMetroMetrics = staticCompositionLocalOf { MetroMetrics() }
internal val LocalMetroMotion = staticCompositionLocalOf { MetroMotion() }

@Composable
fun MetroTheme(
    colors: MetroColors = if (isSystemInDarkTheme()) MetroColors.dark() else MetroColors.light(),
    typography: MetroTypography = MetroTypography.default(),
    metrics: MetroMetrics = MetroMetrics(),
    motion: MetroMotion = MetroMotion(),
    content: @Composable () -> Unit,
) {
    val rememberedColors = remember(colors) { colors }
    CompositionLocalProvider(
        LocalMetroColors provides rememberedColors,
        LocalMetroTypography provides typography,
        LocalMetroMetrics provides metrics,
        LocalMetroMotion provides motion,
        LocalIndication provides remember(motion) { TiltIndication(motion) },
        LocalContentColor provides rememberedColors.foreground,
        LocalTextStyle provides typography.normal,
    ) {
        MetroSurface(color = rememberedColors.background, content = content)
    }
}

/** Единственная публичная точка чтения токенов. */
object MetroTheme {
    val colors: MetroColors
        @Composable @ReadOnlyComposable get() = LocalMetroColors.current
    val typography: MetroTypography
        @Composable @ReadOnlyComposable get() = LocalMetroTypography.current
    val metrics: MetroMetrics
        @Composable @ReadOnlyComposable get() = LocalMetroMetrics.current
    val motion: MetroMotion
        @Composable @ReadOnlyComposable get() = LocalMetroMotion.current
}
```

`LocalContentColor` / `LocalTextStyle` — собственные локалы `metro-core` (не из Material), чтобы ядро оставалось независимым:

```kotlin
val LocalContentColor = compositionLocalOf { Color.White }   // не static: меняется часто и точечно
val LocalTextStyle = staticCompositionLocalOf { TextStyle.Default }
```

`LocalContentColor` — намеренно **не** `static`: он переопределяется на каждом уровне (кнопка, disabled-состояние, текст на акценте), т.е. меняется часто и локально.

### 2.5 Scoped-override: `MetroThemeConfiguration`

Отдельный composable для частичного переопределения поддерева — без пересоздания всей темы.

```kotlin
/**
 * Точечно переопределяет часть токенов для поддерева.
 * Любой не переданный параметр наследуется от родителя.
 */
@Composable
fun MetroThemeConfiguration(
    colors: MetroColors = MetroTheme.colors,
    typography: MetroTypography = MetroTheme.typography,
    metrics: MetroMetrics = MetroTheme.metrics,
    motion: MetroMotion = MetroTheme.motion,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalMetroColors provides colors,
        LocalMetroTypography provides typography,
        LocalMetroMetrics provides metrics,
        LocalMetroMotion provides motion,
        content = content,
    )
}

/** Частый частный случай: другой акцент в поддереве. */
@Composable
fun MetroAccentScope(accent: Color, content: @Composable () -> Unit) {
    val base = MetroTheme.colors
    val scoped = remember(base, accent) {
        base.copy(accent = accent, contrastForeground = contrastOn(accent))
    }
    MetroThemeConfiguration(colors = scoped, content = content)
}
```

Отличие от `MetroTheme`: `MetroThemeConfiguration` **не** ставит `MetroSurface`, не трогает `LocalIndication` и не задаёт дефолты — он только переопределяет. Ровно как `FluentThemeConfiguration` у `compose-fluent-ui`.

---

## 3. Токены цвета

### 3.1 Базовая палитра WP8 (точные значения)

| Токен | Dark (ARGB) | Alpha | Light (ARGB) | Alpha | Комментарий |
|---|---|---|---|---|---|
| `foreground` | `#FFFFFFFF` | 100 % | `#DE000000` | **87 %** | light **не** инверсия dark |
| `background` | `#FF000000` | 100 % | `#FFFFFFFF` | 100 % | абсолютные чёрный/белый (AMOLED-логика) |
| `subtleForeground` | `#99FFFFFF` | 60 % | `#66000000` | 40 % | |
| `disabledForeground` | `#66FFFFFF` | 40 % | `#4D000000` | 30 % | |
| `chrome` | `#FF1F1F1F` | 100 % | `#FFDDDDDD` | 100 % | ApplicationBar, ContextMenu, Popup |
| `border` | `#BFFFFFFF` | 75 % | `#BF000000` | 75 % | толщина 2 px → 1.5 dp |
| `inactive` | `#33FFFFFF` | 20 % | `#33000000` | 20 % | невыбранные Pivot-хедеры, off-трек ToggleSwitch |
| `semitransparent` | `#BF000000` | 75 % | `#BFFFFFFF` | 75 % | подложка модалок |
| `contrastForeground` | `contrastOn(accent)` | — | `contrastOn(accent)` | — | вычисляется |

**Критично:** в light-теме `foreground` — это `#DE000000` (87 % чёрного), а не `#FF000000`, и `subtle` — 40 %, а не 60 %. Механическая инверсия dark-темы даёт заметно другой (более «жёсткий») результат. Light-палитра задаётся отдельным литералом.

```kotlin
object MetroPalette {
    // dark
    val DarkForeground = Color(0xFFFFFFFF)
    val DarkBackground = Color(0xFF000000)
    val DarkSubtle = Color(0x99FFFFFF)
    val DarkDisabled = Color(0x66FFFFFF)
    val DarkChrome = Color(0xFF1F1F1F)
    val DarkBorder = Color(0xBFFFFFFF)
    val DarkInactive = Color(0x33FFFFFF)
    val DarkSemitransparent = Color(0xBF000000)

    // light
    val LightForeground = Color(0xDE000000)   // 87 %
    val LightBackground = Color(0xFFFFFFFF)
    val LightSubtle = Color(0x66000000)       // 40 %
    val LightDisabled = Color(0x4D000000)     // 30 %
    val LightChrome = Color(0xFFDDDDDD)
    val LightBorder = Color(0xBF000000)
    val LightInactive = Color(0x33000000)
    val LightSemitransparent = Color(0xBFFFFFFF)
}

fun MetroColors.Companion.dark(accent: Color = MetroAccents.Cobalt) = MetroColors(
    accent = accent,
    background = MetroPalette.DarkBackground,
    chrome = MetroPalette.DarkChrome,
    foreground = MetroPalette.DarkForeground,
    subtleForeground = MetroPalette.DarkSubtle,
    disabledForeground = MetroPalette.DarkDisabled,
    contrastForeground = contrastOn(accent),
    border = MetroPalette.DarkBorder,
    inactive = MetroPalette.DarkInactive,
    semitransparent = MetroPalette.DarkSemitransparent,
    isDark = true,
)

fun MetroColors.Companion.light(accent: Color = MetroAccents.Cobalt) = MetroColors(
    accent = accent,
    background = MetroPalette.LightBackground,
    chrome = MetroPalette.LightChrome,
    foreground = MetroPalette.LightForeground,
    subtleForeground = MetroPalette.LightSubtle,
    disabledForeground = MetroPalette.LightDisabled,
    contrastForeground = contrastOn(accent),
    border = MetroPalette.LightBorder,
    inactive = MetroPalette.LightInactive,
    semitransparent = MetroPalette.LightSemitransparent,
    isDark = false,
)
```

### 3.2 20 акцентных пресетов WP8

```kotlin
object MetroAccents {
    val Lime     = Color(0xFFA4C400)
    val Green    = Color(0xFF60A917)
    val Emerald  = Color(0xFF008A00)
    val Teal     = Color(0xFF00ABA9)
    val Cyan     = Color(0xFF1BA1E2)
    val Cobalt   = Color(0xFF0050EF)
    val Indigo   = Color(0xFF6A00FF)
    val Violet   = Color(0xFFAA00FF)
    val Pink     = Color(0xFFF472D0)
    val Magenta  = Color(0xFFD80073)
    val Crimson  = Color(0xFFA20025)
    val Red      = Color(0xFFE51400)
    val Orange   = Color(0xFFFA6800)
    val Amber    = Color(0xFFF0A30A)
    val Yellow   = Color(0xFFE3C800)
    val Brown    = Color(0xFF825A2C)
    val Olive    = Color(0xFF6D8764)
    val Steel    = Color(0xFF647687)
    val Mauve    = Color(0xFF76608A)
    val Sienna   = Color(0xFF7A3B3F)

    val All: List<Pair<String, Color>> = listOf(
        "lime" to Lime, "green" to Green, "emerald" to Emerald, "teal" to Teal,
        "cyan" to Cyan, "cobalt" to Cobalt, "indigo" to Indigo, "violet" to Violet,
        "pink" to Pink, "magenta" to Magenta, "crimson" to Crimson, "red" to Red,
        "orange" to Orange, "amber" to Amber, "yellow" to Yellow, "brown" to Brown,
        "olive" to Olive, "steel" to Steel, "mauve" to Mauve, "sienna" to Sienna,
    )
}
```

| Имя | Hex | Rel. luminance (≈) | `contrastOn` |
|---|---|---|---|
| lime | `#A4C400` | 0.472 | white (**граничный**) |
| green | `#60A917` | 0.284 | white |
| emerald | `#008A00` | 0.187 | white |
| teal | `#00ABA9` | 0.316 | white |
| cyan | `#1BA1E2` | 0.311 | white |
| cobalt | `#0050EF` | 0.096 | white |
| indigo | `#6A00FF` | 0.083 | white |
| violet | `#AA00FF` | 0.146 | white |
| pink | `#F472D0` | 0.401 | white |
| magenta | `#D80073` | 0.176 | white |
| crimson | `#A20025` | 0.086 | white |
| red | `#E51400` | 0.208 | white |
| orange | `#FA6800` | 0.319 | white |
| amber | `#F0A30A` | 0.448 | white |
| yellow | `#E3C800` | **0.573** | **black** |
| brown | `#825A2C` | 0.126 | white |
| olive | `#6D8764` | 0.230 | white |
| steel | `#647687` | 0.192 | white |
| mauve | `#76608A` | 0.153 | white |
| sienna | `#7A3B3F` | 0.086 | white |

Только `yellow` переходит порог 0.5. `lime` (0.472) и `amber` (0.448) — граничные: чисто по формуле дают белый текст, но контраст ≈ 2.2:1, что провально по WCAG. Поэтому в API предусмотрен явный override-словарь.

### 3.3 `contrastOn`

```kotlin
/**
 * Цвет текста/иконок поверх произвольного акцента.
 * Базовое правило Metro-Compose: luminance >= 0.5 → чёрный, иначе белый.
 * Ручные исключения — для граничных акцентов, где формула даёт нечитаемую пару.
 */
fun contrastOn(
    accent: Color,
    threshold: Float = 0.5f,
    overrides: Map<Color, Color> = MetroContrastOverrides,
): Color = overrides[accent]
    ?: if (accent.luminance() >= threshold) Color.Black else Color.White

/** Акценты, где формула по luminance даёт контраст ниже WCAG AA (4.5:1). */
val MetroContrastOverrides: Map<Color, Color> = mapOf(
    MetroAccents.Lime to Color.Black,
    MetroAccents.Amber to Color.Black,
    MetroAccents.Yellow to Color.Black,
)

/** Контрастность двух цветов по WCAG 2.x — для тестов палитры. */
fun contrastRatio(a: Color, b: Color): Float {
    val l1 = a.luminance() + 0.05f
    val l2 = b.luminance() + 0.05f
    return if (l1 > l2) l1 / l2 else l2 / l1
}
```

`Color.luminance()` из `androidx.compose.ui.graphics` уже считает WCAG relative luminance (с sRGB-гаммой), поэтому свою реализацию писать не нужно.

### 3.4 Системный акцент: `expect/actual`

```kotlin
// commonMain
/**
 * Цвет системного акцента, если платформа его предоставляет.
 * null → вызывающий код берёт фолбэк (MetroAccents.Cobalt).
 */
@Composable
expect fun rememberSystemAccentColor(): Color?

@Composable
fun rememberMetroAccent(fallback: Color = MetroAccents.Cobalt): Color =
    rememberSystemAccentColor() ?: fallback
```

```kotlin
// androidMain
@Composable
actual fun rememberSystemAccentColor(): Color? {
    val context = LocalContext.current
    return remember(context) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                Color(context.resources.getColor(android.R.color.system_accent1_600, context.theme))
            else -> null
        }
    }
}
```
Альтернатива на Android 12+: `dynamicDarkColorScheme(context).primary` (требует material3) или `WallpaperManager.getWallpaperColors(FLAG_SYSTEM)?.primaryColor` (API 27+, нужен permission на некоторых OEM). В `metro-core` (без material3) используем прямой доступ к `system_accent1_*` ресурсам — зависимостей не добавляет.

```kotlin
// desktopMain (JVM) — Windows
@Composable
actual fun rememberSystemAccentColor(): Color? = remember {
    if (!System.getProperty("os.name").orEmpty().startsWith("Windows")) return@remember null
    readWindowsAccent()
}

/**
 * Порядок предпочтения:
 *  1) WinRT Windows.UI.ViewManagement.UISettings.GetColorValue(UIColorType.Accent) — точное значение;
 *  2) реестр HKCU\Software\Microsoft\Windows\DWM\AccentColor (ABGR!) — без нативных зависимостей.
 */
private fun readWindowsAccent(): Color? = runCatching {
    val process = ProcessBuilder(
        "reg", "query",
        "HKCU\\Software\\Microsoft\\Windows\\DWM",
        "/v", "AccentColor",
    ).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()
    // "    AccentColor    REG_DWORD    0xffd77800"
    val hex = Regex("0x([0-9a-fA-F]{8})").find(output)?.groupValues?.get(1) ?: return null
    val abgr = hex.toLong(16)
    val a = ((abgr shr 24) and 0xFF).toInt()
    val b = ((abgr shr 16) and 0xFF).toInt()
    val g = ((abgr shr 8) and 0xFF).toInt()
    val r = (abgr and 0xFF).toInt()
    Color(red = r, green = g, blue = b, alpha = a)
}.getOrNull()
```
Замечание: DWM-ключ хранит цвет в **ABGR**, а не ARGB — частая ошибка. Если позже добавим JNA (`net.java.dev.jna:jna-platform`), можно перейти на WinRT `UISettings`, который отдаёт ровно тот же акцент, что видят UWP-приложения, и умеет подписываться на изменения через `ColorValuesChanged`. **`[проверить]`** — доступность WinRT-моста без COM-инициализации в JVM-процессе.

macOS/Linux JVM: у macOS есть `NSColor.controlAccentColor` (через JNI/`skiko`), у Linux — нет единого источника. Возвращаем `null` → фолбэк.

```kotlin
// iosMain
@Composable
actual fun rememberSystemAccentColor(): Color? = null   // системного «accent» в iOS нет;
                                                        // UIColor.tintColor не эквивалент Metro-акцента
// wasmJsMain / jsMain
@Composable
actual fun rememberSystemAccentColor(): Color? = null   // CSS даёт только prefers-color-scheme,
                                                        // AccentColor из CSS Color 4 нечитаем из JS надёжно
```

---

## 4. Типографика

### 4.1 Перевод Metro-пикселей в sp/dp

Исходные метрики Metro заданы в «пикселях» WPF/Silverlight (1/96 дюйма). Перевод в типографские пункты — ×0.75.

Проверка корректности множителя: применяя ×0.75 к Windows 8 ramp в px мы получаем **ровно официальную Win8-рампу в пунктах** — 42 / 20 / 11 / 11 / 11 / 9 pt. Это сильный аргумент за коэффициент 0.75 как канонический.

**Решение: `sp = metro_px * 0.75`, `dp = metro_px * 0.75`** (единый коэффициент `MetroMetrics.scale`).

Дополнительная валидация на layout-метриках WP: статус-бар 32 px × 0.75 = **24 dp** — в точности высота Android status bar; ApplicationBar 72 px × 0.75 = 54 dp — правдоподобная высота нижней панели.

### 4.2 Windows Phone ramp

| WP-имя ресурса | px | sp (×0.75) | Вес | Применение |
|---|---|---|---|---|
| `PhoneFontSizeSmall` | 18.667 | **14** | Normal | подписи, метаданные |
| `PhoneFontSizeNormal` | 20 | **15** | Normal | основной текст |
| `PhoneFontSizeMedium` | 22.667 | **17** | Normal | заголовок элемента списка |
| `PhoneFontSizeMediumLarge` | 25.333 | **19** | Normal | title3 |
| `PhoneFontSizeLarge` | 32 | **24** | Normal | title2 |
| `PhoneFontSizeExtraLarge` | 42.667 | **32** | Light | заголовок раздела |
| `PhoneFontSizeExtraExtraLarge` | 72 | **54** | Light | заголовок страницы / Pivot title |
| `PhoneFontSizeHuge` | 186.667 | **140** | ExtraLight | Panorama title, цифры на плитках |

### 4.3 Windows 8 ramp

| Роль | px | sp | lineHeight px | lineHeight sp | Вес |
|---|---|---|---|---|---|
| Header | 56 | **42** | 40 | **30** | Light |
| Subheader | 26.667 | **20** | 30 | **22.5** | Light |
| Title | 14.667 | **11** | 20 | **15** | SemiBold |
| Subtitle | 14.667 | **11** | 20 | **15** | Normal |
| Body | 14.667 | **11** | 20 | **15** | SemiLight (W300) |
| Caption | 12 | **9** | 20 | **15** | Normal |

Обратите внимание: у Header `lineHeight` (30 sp) **меньше** кегля (42 sp). Это не ошибка — Windows 8 сажает крупные заголовки на плотную базовую линию, обрезая избыточный ведущий пробел. В Compose это воспроизводится только с `LineHeightStyle(trim = ...)`, иначе текст будет клипаться (см. 4.6).

### 4.4 Шрифт: Selawik через compose-resources

Segoe UI / Segoe WP бандлить нельзя (EULA Microsoft — распространение вместе с приложением запрещено). Легальная метрически совместимая замена — **Selawik** (`github.com/microsoft/Selawik`, SIL OFL-1.1).

| Файл | Вес | `FontWeight` |
|---|---|---|
| `selawkl.ttf` | ExtraLight | `W200` |
| `selawksl.ttf` | Light | `W300` |
| `selawk.ttf` | Regular | `W400` |
| `selawksb.ttf` | SemiBold | `W600` |
| `selawkb.ttf` | Bold | `W700` |

Раскладка ресурсов (модуль `metro-resources`):
```
metro-resources/src/commonMain/composeResources/font/
    selawkl.ttf
    selawksl.ttf
    selawk.ttf
    selawksb.ttf
    selawkb.ttf
```

```kotlin
// metro-resources/build.gradle.kts
compose.resources {
    publicResClass = true
    packageOfResClass = "io.metro.resources"
    generateResClass = always
}
```

```kotlin
// metro-core/src/commonMain/kotlin/.../MetroFontFamily.kt
import org.jetbrains.compose.resources.Font
import io.metro.resources.Res
import io.metro.resources.selawk
import io.metro.resources.selawkb
import io.metro.resources.selawkl
import io.metro.resources.selawksb
import io.metro.resources.selawksl

@Composable
fun rememberSelawikFamily(): FontFamily = FontFamily(
    Font(Res.font.selawkl,  FontWeight.ExtraLight, FontStyle.Normal),
    Font(Res.font.selawksl, FontWeight.Light,      FontStyle.Normal),
    Font(Res.font.selawk,   FontWeight.Normal,     FontStyle.Normal),
    Font(Res.font.selawksb, FontWeight.SemiBold,   FontStyle.Normal),
    Font(Res.font.selawkb,  FontWeight.Bold,       FontStyle.Normal),
)
```

**`[проверить]`**: в `org.jetbrains.compose.resources` функция `Font(resource, weight, style)` — `@Composable`, поэтому семейство собирается в composable-контексте и кэшируется через `remember`. Если в 1.12.0 доступен не-composable вариант (`suspend fun getFontResourceBytes` / `Font(...)` без `@Composable`), предпочесть его — тогда `MetroTypography` можно строить вне композиции (важно для тестов и для `MetroTypography.default()` как чистой функции).

**Fallback-стек.** `FontFamily` в Compose не поддерживает CSS-подобный fallback между разными семействами напрямую; фолбэк реализуется списком `Font` в одном `FontFamily` (движок ищет глиф в порядке объявления) плюс системным дефолтом:

```kotlin
@Composable
fun rememberMetroFontFamily(): FontFamily {
    val selawik = rememberSelawikFamily()
    return remember(selawik) {
        // Порядок: Selawik → Inter → Noto Sans → платформенный default.
        // Inter/Noto подключаются опционально модулем metro-resources-extended.
        selawik
    }
}
```

> ### 🚨 ПРОВЕРЕНО: кириллицы в Selawik НЕТ
>
> Вопрос был помечен как открытый — **он закрыт, и ответ отрицательный.**
> Microsoft Typography, страница Selawik: `Script Tags: dlng:'Latn' slng:'Latn'`, code pages — только **1252 / 1250 / 1254 / 1257**. Кодовой страницы **1251 (Cyrillic) нет**.
> Font Library, анализируя сами TTF: «Full Language Support: Baltic, Basic Latin, Catalan, Central European, Dutch, Euro, Romanian, Turkish, Western European» — ни кириллицы, ни греческого ни в одном из пяти начертаний.
>
> План Б становится планом А:
> 1. **Inter** (SIL OFL) как fallback-семейство для кириллицы — метрики близки, но **не идентичны** Segoe, поэтому русский и латинский текст в одном интерфейсе будут иметь слегка разный вертикальный ритм;
> 2. или **Noto Sans** — максимальное покрытие Unicode, худшее совпадение по характеру;
> 3. или отказаться от Selawik целиком в пользу Inter — единый ритм ценой того, что вся числовая рампа, выведенная из метрик Segoe, перестаёт быть точной;
> 4. в любом случае API получает `MetroTypography.default(fontFamily = ...)`, чтобы потребитель мог подставить своё семейство.
>
> Решение принимается в спайке 3 (см. `06-roadmap.md`), **до старта разработки**.

Проверка в CI: скриншот-тест с русской строкой + assert, что в отрендеренном bitmap нет «tofu»-боксов (сравнение с эталоном).

### 4.5 Проблема `lineHeight = Unspecified`

В оригинальных WP-стилях `LineHeight` **не задан** — Silverlight берёт естественную межстрочку из метрик Segoe WP (`ascent + descent + lineGap` ≈ 1.27 × кегль). Прямой перенос (`lineHeight = TextUnit.Unspecified`) в Compose даёт:
- **разную** межстрочку на разных платформах (Skia на iOS/desktop и Android-`Paint` слегка расходятся в округлении метрик);
- невозможность попасть в базовую сетку 20 px / 15 dp;
- зависимость от того, какой шрифт реально подставился (Selawik vs фолбэк) — при подмене семейства «плывёт» весь вертикальный ритм.

**Решение: явный `lineHeight` для каждого стиля, снапнутый на субъединицу сетки.**

```kotlin
/** Округляет lineHeight вверх до ближайшего кратного шагу базовой сетки. */
private fun snapToGrid(fontSize: TextUnit, grid: Float = 15f, factor: Float = 1.27f): TextUnit {
    val natural = fontSize.value * factor
    return (ceil(natural / grid) * grid).sp
}
```

Для WP-рампы это даёт: 14→15, 15→30(!)… — тут снап на 15 слишком груб для мелких кеглей. Практическое правило:
- WP-рампа: `lineHeight = round(fontSize * 1.27)` без снапа (WP не имел baseline grid);
- Win8-рампа: `lineHeight` берётся из таблицы 4.3 (жёсткая сетка 15 sp).

### 4.6 Точное попадание в baseline grid

```kotlin
private val MetroPlatformStyle = PlatformTextStyle(includeFontPadding = false)

private val MetroLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Proportional,
    trim = LineHeightStyle.Trim.Both,   // FirstLineTop + LastLineBottom
)

private fun metroStyle(
    family: FontFamily,
    size: TextUnit,
    lineHeight: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: TextUnit = 0.sp,
) = TextStyle(
    fontFamily = family,
    fontSize = size,
    lineHeight = lineHeight,
    fontWeight = weight,
    letterSpacing = letterSpacing,
    platformStyle = MetroPlatformStyle,
    lineHeightStyle = MetroLineHeightStyle,
)
```

Ключевые моменты:
- `includeFontPadding = false` — обязателен на Android, иначе к первой и последней строке добавляется паддинг из метрик шрифта и вертикальный ритм ломается. На не-Android таргетах параметр игнорируется, но `PlatformTextStyle` в common-коде допустим.
- `Trim.Both` убирает ведущий/замыкающий полупробел, из-за чего верх первой глифовой строки садится ровно на границу бокса. Именно это позволяет задать Header с `lineHeight` (30 sp) меньше кегля (42 sp) — как в Win8.
- `Alignment.Proportional` распределяет остаток пропорционально ascent/descent; для однострочных заголовков можно `Alignment.Center`.

### 4.7 Полная фабрика типографики

```kotlin
@Composable
fun MetroTypography.Companion.default(
    fontFamily: FontFamily = rememberMetroFontFamily(),
): MetroTypography = remember(fontFamily) {
    val small        = metroStyle(fontFamily, 14.sp, 18.sp)
    val normal       = metroStyle(fontFamily, 15.sp, 19.sp)
    val medium       = metroStyle(fontFamily, 17.sp, 22.sp)
    val mediumLarge  = metroStyle(fontFamily, 19.sp, 24.sp)
    val large        = metroStyle(fontFamily, 24.sp, 30.sp)
    val extraLarge   = metroStyle(fontFamily, 32.sp, 40.sp, FontWeight.Light)
    val extraExtraLarge = metroStyle(fontFamily, 54.sp, 66.sp, FontWeight.Light)
    val huge         = metroStyle(fontFamily, 140.sp, 150.sp, FontWeight.ExtraLight)

    MetroTypography(
        small = small,
        normal = normal,
        medium = medium,
        mediumLarge = mediumLarge,
        large = large,
        extraLarge = extraLarge,
        extraExtraLarge = extraExtraLarge,
        huge = huge,
        subtle = normal,
        title1 = extraExtraLarge,
        title2 = large,
        title3 = mediumLarge,
        groupHeader = medium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
        accent = normal,
        contrast = normal,
        win8 = Win8Typography(
            header    = metroStyle(fontFamily, 42.sp, 30.sp, FontWeight.Light),
            subheader = metroStyle(fontFamily, 20.sp, 22.5.sp, FontWeight.Light),
            title     = metroStyle(fontFamily, 11.sp, 15.sp, FontWeight.SemiBold),
            subtitle  = metroStyle(fontFamily, 11.sp, 15.sp, FontWeight.Normal),
            body      = metroStyle(fontFamily, 11.sp, 15.sp, FontWeight.Light),   // SemiLight ≈ W300
            caption   = metroStyle(fontFamily, 9.sp, 15.sp, FontWeight.Normal),
        ),
    )
}
```

Цветá (`subtle`, `accent`, `contrast`) **не** зашиваются в `TextStyle` — их подставляет компонент через `LocalContentColor`, иначе типографика становится зависимой от палитры и `MetroTypography` перестаёт быть переиспользуемым между темами.

---

## 5. Indication / Tilt

### 5.1 Актуальное состояние API

| API | Статус | Что делать |
|---|---|---|
| `rememberRipple()` | **deprecated** (compose-material/-material3) | не использовать |
| `ripple()` из `androidx.compose.material3` | актуально | только в адаптере |
| `Indication` | базовый интерфейс, `rememberUpdatedInstance` deprecated | реализовывать через `IndicationNodeFactory` |
| `IndicationNodeFactory : Indication` | актуальный способ | **наш путь** |
| `DelegatableNode` / `Modifier.Node` | актуально | нода эффекта |
| `LocalIndication` | актуально | подменяем на `TiltIndication` в `MetroTheme` |
| `LocalRippleConfiguration` (M3 1.4) → `RippleThemeConfiguration` (1.5-alpha) | **менялось** | шим в адаптере, см. §6.4 |

### 5.2 Модель tilt из `TiltEffect.cs`

Нормализованная точка касания центрируется: `dx = nx − 0.5`, `dy = ny − 0.5`, где `nx, ny ∈ [0,1]`.

```
angleMagnitude = |dx| + |dy|                      ∈ [0, 1]
angle          = angleMagnitude * 0.3 рад         ∈ [0, 17.188°]
depression     = (1 − angleMagnitude) * 25 px     ∈ [0, 25 px]
xContribution  = |dx| / (|dx| + |dy|)
RotationY      = angle * xContribution     * −sign(dx)
RotationX      = angle * (1 − xContribution) * sign(dy)
GlobalOffsetZ  = −depression
```

Тайминги: **нажатие применяется мгновенно, без анимации**; возврат — задержка **200 мс**, затем анимация **100 мс**. `Scale` не используется — только `PlaneProjection` (3D-поворот + сдвиг по Z в перспективе).

Смысл: касание в центре → элемент «утапливается» без поворота; касание в углу → максимальный поворот без утапливания.

### 5.3 Проблема `translationZ` в Compose

В WP `GlobalOffsetZ` двигал плоскость вдоль Z **внутри перспективной проекции**, поэтому элемент визуально уменьшался. В Compose `GraphicsLayerScope.translationZ` влияет только на порядок отрисовки и тень (elevation) — **визуального уменьшения не даёт**.

Поэтому «утапливание» эмулируем однородным масштабом по формуле перспективы:

```
scale = cameraDistancePx / (cameraDistancePx + depressionPx)
```

Санити-чек: при `cameraDistance ≈ 8 × density` и `depression = 18.75.dp` на mdpi получаем `scale ≈ 0.975` — **ровно то значение, которое WinJS использует для `pointerDown`** (`scale(0.975)`). Совпадение двух независимых источников подтверждает корректность эмуляции.

### 5.4 Полная реализация

```kotlin
// metro-core/src/commonMain/kotlin/.../TiltIndication.kt
package io.metro.theme.indication

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign

private const val RadToDeg = (180.0 / PI).toFloat()

/**
 * Metro tilt effect. Порт Windows Phone Toolkit TiltEffect.cs.
 *
 * Отличия от оригинала, вынужденные платформой:
 *  - GlobalOffsetZ эмулируется однородным scale (см. §5.3), т.к. translationZ
 *    в Compose не даёт перспективного уменьшения;
 *  - перспектива задаётся cameraDistance графического слоя.
 */
@Immutable
class TiltIndication(
    private val maxAngleRad: Float = 0.3f,
    private val maxDepression: Dp = 18.75.dp,
    private val returnDelayMillis: Int = 200,
    private val returnDurationMillis: Int = 100,
    private val cameraDistanceMultiplier: Float = 8f,
) : IndicationNodeFactory {

    constructor(motion: MetroMotion) : this(
        maxAngleRad = motion.tiltMaxAngleRad,
        maxDepression = motion.tiltMaxDepression,
        returnDelayMillis = motion.tiltReturnDelayMillis,
        returnDurationMillis = motion.tiltReturnMillis,
    )

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        TiltNode(
            interactionSource = interactionSource,
            maxAngleRad = maxAngleRad,
            maxDepression = maxDepression,
            returnDelayMillis = returnDelayMillis,
            returnDurationMillis = returnDurationMillis,
            cameraDistanceMultiplier = cameraDistanceMultiplier,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TiltIndication) return false
        return maxAngleRad == other.maxAngleRad &&
            maxDepression == other.maxDepression &&
            returnDelayMillis == other.returnDelayMillis &&
            returnDurationMillis == other.returnDurationMillis &&
            cameraDistanceMultiplier == other.cameraDistanceMultiplier
    }

    override fun hashCode(): Int {
        var result = maxAngleRad.hashCode()
        result = 31 * result + maxDepression.hashCode()
        result = 31 * result + returnDelayMillis
        result = 31 * result + returnDurationMillis
        result = 31 * result + cameraDistanceMultiplier.hashCode()
        return result
    }
}

private class TiltNode(
    private val interactionSource: InteractionSource,
    private val maxAngleRad: Float,
    private val maxDepression: Dp,
    private val returnDelayMillis: Int,
    private val returnDurationMillis: Int,
    private val cameraDistanceMultiplier: Float,
) : Modifier.Node(), LayoutModifierNode {

    /** 1f = полностью наклонён, 0f = покой. */
    private val progress = Animatable(0f)

    private var targetRotationX by mutableFloatStateOf(0f)
    private var targetRotationY by mutableFloatStateOf(0f)
    private var targetDepressionPx by mutableFloatStateOf(0f)
    private var pivot: TransformOrigin = TransformOrigin.Center

    private var widthPx = 0
    private var heightPx = 0

    private var animationJob: Job? = null

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> onPress(interaction.pressPosition)
                    is PressInteraction.Release,
                    is PressInteraction.Cancel -> onRelease()
                }
            }
        }
        // Реплейсмент на каждом кадре анимации.
        coroutineScope.launch {
            snapshotFlow { progress.value }.collect { invalidatePlacement() }
        }
    }

    override fun onDetach() {
        animationJob?.cancel()
        animationJob = null
    }

    private fun onPress(pressPosition: Offset) {
        if (widthPx <= 0 || heightPx <= 0) return

        val nx = (pressPosition.x / widthPx).coerceIn(0f, 1f)
        val ny = (pressPosition.y / heightPx).coerceIn(0f, 1f)
        val dx = nx - 0.5f
        val dy = ny - 0.5f

        val magnitude = (abs(dx) + abs(dy)).coerceIn(0f, 1f)
        val angleRad = magnitude * maxAngleRad
        val xContribution = if (magnitude == 0f) 0.5f else abs(dx) / magnitude

        // RotationY = angle * xContrib * -sign(dx)
        targetRotationY = angleRad * xContribution * -sign(dx) * RadToDeg
        // RotationX = angle * (1 - xContrib) * sign(dy)
        targetRotationX = angleRad * (1f - xContribution) * sign(dy) * RadToDeg
        targetDepressionPx = (1f - magnitude) * with(requireDensity()) { maxDepression.toPx() }

        // Оригинал наклоняет МГНОВЕННО, без анимации.
        animationJob?.cancel()
        animationJob = coroutineScope.launch { progress.snapTo(1f) }
    }

    private fun onRelease() {
        animationJob?.cancel()
        animationJob = coroutineScope.launch {
            delay(returnDelayMillis.toLong())
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = returnDurationMillis, easing = LinearEasing),
            )
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        widthPx = placeable.width
        heightPx = placeable.height

        val cameraPx = cameraDistanceMultiplier * density
        return layout(placeable.width, placeable.height) {
            val t = progress.value
            if (t == 0f) {
                placeable.place(0, 0)
            } else {
                val depression = targetDepressionPx * t
                // Перспективная эмуляция «утапливания» вдоль Z.
                val depressionScale = cameraPx / (cameraPx + depression)
                placeable.placeWithLayer(0, 0) {
                    transformOrigin = pivot
                    cameraDistance = cameraPx
                    rotationX = targetRotationX * t
                    rotationY = targetRotationY * t
                    scaleX = depressionScale
                    scaleY = depressionScale
                    // translationZ оставляем 0: в Compose он не даёт визуального сдвига,
                    // но влиял бы на порядок отрисовки/тени.
                }
            }
        }
    }
}
```

Вспомогательное (`requireDensity()` есть как `currentValueOf(LocalDensity)` у `CompositionLocalConsumerModifierNode`; в `LayoutModifierNode` внутри `measure` доступен `density` из `MeasureScope`):

```kotlin
// Если requireDensity() недоступен в вашей версии — сделайте ноду
// CompositionLocalConsumerModifierNode и используйте currentValueOf(LocalDensity).
private fun Modifier.Node.requireDensity(): Density =
    requireLayoutNode().density
```

### 5.5 Риск и фолбэк

`IndicationNodeFactory.create()` возвращает `DelegatableNode`, который фреймворк подключает в цепочку модификаторов `Modifier.indication(...)`. Использование `LayoutModifierNode` в этой позиции **технически работает**, но не является заявленным контрактом (документация ориентирует на `DrawModifierNode`). Если поведение окажется нестабильным (например, tilt не применится к содержимому, находящемуся снаружи `clickable`):

**Фолбэк** — отдельный публичный модификатор, применяемый явно:

```kotlin
fun Modifier.metroTilt(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
): Modifier = if (!enabled) this else this then TiltElement(interactionSource)
```

и в `MetroButton`/`MetroTile` он ставится **до** `clickable`. `LocalIndication` в этом случае получает `NoIndication` (`Indication`, не рисующий ничего), чтобы Material-компоненты внутри Metro-темы не показывали ripple.

```kotlin
object NoIndication : IndicationNodeFactory {
    private object Node : Modifier.Node()
    override fun create(interactionSource: InteractionSource): DelegatableNode = Node
    override fun equals(other: Any?) = other === NoIndication
    override fun hashCode() = -1
}
```

3D-поворот на **Web (wasmJs/js)**: рендер идёт через Skia/CanvasKit, `rotationX/rotationY` с `cameraDistance` поддерживаются. **`[проверить]`** качество на wasm-таргете; при артефактах — деградировать до `pointerDownScale = 0.975f` (чистый scale, WinJS-совместимо) через флаг `TiltIndication(mode = Tilt3D | ScaleOnly)`.

---

## 6. Мост к Material3

### 6.1 Полный список цветовых ролей `ColorScheme`

**M3 1.4.x (stable) — 36 ролей:**

`primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`, `inversePrimary`,
`secondary`, `onSecondary`, `secondaryContainer`, `onSecondaryContainer`,
`tertiary`, `onTertiary`, `tertiaryContainer`, `onTertiaryContainer`,
`background`, `onBackground`,
`surface`, `onSurface`, `surfaceVariant`, `onSurfaceVariant`, `surfaceTint`,
`inverseSurface`, `inverseOnSurface`,
`error`, `onError`, `errorContainer`, `onErrorContainer`,
`outline`, `outlineVariant`, `scrim`,
`surfaceBright`, `surfaceDim`,
`surfaceContainer`, `surfaceContainerHigh`, `surfaceContainerHighest`, `surfaceContainerLow`, `surfaceContainerLowest`.

**Дополнительно «fixed»-роли** (в 1.4 присутствуют, в 1.5 закреплены): `primaryFixed`, `primaryFixedDim`, `onPrimaryFixed`, `onPrimaryFixedVariant`, `secondaryFixed`, `secondaryFixedDim`, `onSecondaryFixed`, `onSecondaryFixedVariant`, `tertiaryFixed`, `tertiaryFixedDim`, `onTertiaryFixed`, `onTertiaryFixedVariant`. **`[проверить]`** точный состав в целевой версии — от него зависит, соберётся ли конструктор `ColorScheme(...)` (он позиционный и **не** имеет дефолтов).

> Практический приём: **не** вызывать конструктор `ColorScheme(...)` напрямую. Взять `darkColorScheme()` / `lightColorScheme()` (у них все параметры с дефолтами) и переопределить нужные — тогда добавление ролей в новой версии не ломает сборку.

### 6.2 Маппинг Metro → `ColorScheme`

| Роль M3 | Metro-источник | Комментарий |
|---|---|---|
| `primary` | `accent` | |
| `onPrimary` | `contrastForeground` | |
| `primaryContainer` | `accent` | Metro не различает container-уровни |
| `onPrimaryContainer` | `contrastForeground` | |
| `inversePrimary` | `accent` | |
| `secondary` | `accent` | Metro одноцветна по определению |
| `onSecondary` | `contrastForeground` | |
| `secondaryContainer` | `chrome` | |
| `onSecondaryContainer` | `foreground` | |
| `tertiary` | `accent` | |
| `onTertiary` | `contrastForeground` | |
| `tertiaryContainer` | `chrome` | |
| `onTertiaryContainer` | `foreground` | |
| `background` | `background` | абсолютный #000/#FFF |
| `onBackground` | `foreground` | |
| `surface` | `background` | **не** `chrome`: Metro-страница плоская |
| `onSurface` | `foreground` | |
| `surfaceVariant` | `chrome` | |
| `onSurfaceVariant` | `subtleForeground` | |
| `surfaceTint` | **`Color.Transparent`** | **критично**: убивает tonal elevation overlay |
| `inverseSurface` | `foreground` | |
| `inverseOnSurface` | `background` | |
| `error` | `MetroAccents.Red` (`#E51400`) | WP `PhoneAccentBrush` для ошибок не менялся; берём red |
| `onError` | `Color.White` | |
| `errorContainer` | `MetroAccents.Red` | |
| `onErrorContainer` | `Color.White` | |
| `outline` | `border` | |
| `outlineVariant` | `inactive` | |
| `scrim` | `semitransparent` | |
| `surfaceBright` | `background` | |
| `surfaceDim` | `background` | все surface-уровни **одинаковы** — плоскость Metro |
| `surfaceContainer` | `chrome` | |
| `surfaceContainerHigh` | `chrome` | |
| `surfaceContainerHighest` | `chrome` | |
| `surfaceContainerLow` | `background` | |
| `surfaceContainerLowest` | `background` | |
| `*Fixed`, `*FixedDim` | `accent` / `contrastForeground` | заполнять через `copy()`, если есть |

Главный трюк — **`surfaceTint = Color.Transparent`**. Без него любой `Surface`/`Card` с ненулевым `tonalElevation` подмешивает акцент в фон, и «плоскость» Metro разрушается. Второй обязательный шаг — обнуление elevation-дефолтов (см. 6.4).

### 6.3 Маппинг `Typography` → WP-рампа

| M3 | Metro | sp | Вес |
|---|---|---|---|
| `displayLarge` | `huge` | 140 | ExtraLight |
| `displayMedium` | `extraExtraLarge` | 54 | Light |
| `displaySmall` | `extraLarge` | 32 | Light |
| `headlineLarge` | `extraLarge` | 32 | Light |
| `headlineMedium` | `large` | 24 | Normal |
| `headlineSmall` | `mediumLarge` | 19 | Normal |
| `titleLarge` | `large` | 24 | Normal |
| `titleMedium` | `mediumLarge` | 19 | Normal |
| `titleSmall` | `medium` | 17 | Normal |
| `bodyLarge` | `medium` | 17 | Normal |
| `bodyMedium` | `normal` | 15 | Normal |
| `bodySmall` | `small` | 14 | Normal |
| `labelLarge` | `normal` | 15 | Normal |
| `labelMedium` | `small` | 14 | Normal |
| `labelSmall` | `small` | 14 | Normal |

Замечание: у M3 `labelSmall` = 11 sp, у нас минимум 14 sp (`PhoneFontSizeSmall`). Это осознанно — в Metro нет кегля меньше 14 sp, и уменьшение ради «похожести на Material» противоречит системе.

### 6.4 `Shapes` и остальные дефолты

```kotlin
val MetroShapes = Shapes(
    extraSmall = RectangleShape,
    small = RectangleShape,
    medium = RectangleShape,
    large = RectangleShape,
    extraLarge = RectangleShape,
    // M3 1.5-alpha: largeIncreased / extraLargeIncreased — задаются через copy(),
    // чтобы код собирался и против 1.4.x.
)
```

Ripple выключается по-разному в 1.4 и 1.5 — изолируем шимом:

```kotlin
// metro-material-adapter (M3 1.4.x)
@Composable
internal actual fun ProvideNoRipple(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRippleConfiguration provides null, content = content)
}
```
```kotlin
// metro-material-adapter-next (M3 1.5.0-alpha)
@Composable
internal actual fun ProvideNoRipple(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleThemeConfiguration(isEnabled = false),
        content = content,
    )
}
```
**`[проверить]`** точную сигнатуру `RippleThemeConfiguration` в 1.5.0-alpha22.

Конвертеры:

```kotlin
fun MetroColors.toMaterialColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = contrastForeground,
        primaryContainer = accent,
        onPrimaryContainer = contrastForeground,
        inversePrimary = accent,
        secondary = accent,
        onSecondary = contrastForeground,
        secondaryContainer = chrome,
        onSecondaryContainer = foreground,
        tertiary = accent,
        onTertiary = contrastForeground,
        tertiaryContainer = chrome,
        onTertiaryContainer = foreground,
        background = background,
        onBackground = foreground,
        surface = background,
        onSurface = foreground,
        surfaceVariant = chrome,
        onSurfaceVariant = subtleForeground,
        surfaceTint = Color.Transparent,          // ← ключевое
        inverseSurface = foreground,
        inverseOnSurface = background,
        error = MetroAccents.Red,
        onError = Color.White,
        errorContainer = MetroAccents.Red,
        onErrorContainer = Color.White,
        outline = border,
        outlineVariant = inactive,
        scrim = semitransparent,
        surfaceBright = background,
        surfaceDim = background,
        surfaceContainer = chrome,
        surfaceContainerHigh = chrome,
        surfaceContainerHighest = chrome,
        surfaceContainerLow = background,
        surfaceContainerLowest = background,
    )
}

fun MetroTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = huge,
    displayMedium = extraExtraLarge,
    displaySmall = extraLarge,
    headlineLarge = extraLarge,
    headlineMedium = large,
    headlineSmall = mediumLarge,
    titleLarge = large,
    titleMedium = mediumLarge,
    titleSmall = medium,
    bodyLarge = medium,
    bodyMedium = normal,
    bodySmall = small,
    labelLarge = normal,
    labelMedium = small,
    labelSmall = small,
)
```

### 6.5 Три стратегии интеропа

#### Стратегия A — `MetroTheme` сам поднимает `MaterialTheme`

```kotlin
// ❌ в metro-core — нельзя (тянет material3 в ядро)
@Composable
fun MetroTheme(
    colors: MetroColors = ...,
    typography: MetroTypography = ...,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(/* metro locals */) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = typography.toMaterialTypography(),
            shapes = MetroShapes,
        ) {
            ProvideNoRipple(content)
        }
    }
}
```

| + | − |
|---|---|
| ноль церемоний для пользователя: обернул — работает всё | `metro-core` жёстко зависит от material3 → версионный ад из §1 |
| один источник правды | нельзя использовать Metro в проекте, где Material не нужен (лишние ~1.5 МБ) |
| Material-компоненты «просто работают» внутри Metro | навязывает Material тем, кто хочет чистый Metro |

#### Стратегия B — отдельный `MetroMaterialAdapter { }` (модуль `metro-material-adapter`)

```kotlin
// metro-material-adapter/src/commonMain/kotlin/.../MetroMaterialAdapter.kt
/**
 * Делает Material3-компоненты визуально совместимыми с текущей MetroTheme.
 * Требует, чтобы выше по дереву уже была MetroTheme { }.
 */
@Composable
fun MetroMaterialAdapter(
    colorScheme: ColorScheme = MetroTheme.colors.toMaterialColorScheme(),
    typography: Typography = MetroTheme.typography.toMaterialTypography(),
    shapes: Shapes = MetroShapes,
    flattenElevation: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = colorScheme, typography = typography, shapes = shapes) {
        ProvideNoRipple {
            CompositionLocalProvider(
                LocalMinimumInteractiveComponentSize provides MetroTheme.metrics.touchTargetMin,
                LocalAbsoluteTonalElevation provides if (flattenElevation) 0.dp else LocalAbsoluteTonalElevation.current,
                content = content,
            )
        }
    }
}
```

Использование:
```kotlin
MetroTheme {
    MetroPage(title = "settings") {
        MetroToggle(...)              // родные Metro-компоненты
        MetroMaterialAdapter {
            DatePicker(state = ...)   // чужой Material-компонент, перекрашенный
        }
    }
}
```

| + | − |
|---|---|
| `metro-core` чист от Material | пользователь должен явно оборачивать |
| Material подключается опционально (отдельный артефакт) | двойной `CompositionLocalProvider` в смешанных экранах |
| можно иметь две ветки адаптера под 1.4 и 1.5 | легко забыть обернуть → Material-компонент выглядит чужеродно |
| легко тестировать отдельно | |

#### Стратегия C — обратный `MetroTheme.fromMaterial()`

Для случая «Material-приложение хочет вставить Metro-виджет».

```kotlin
// metro-material-adapter
@Composable
fun MetroColors.Companion.fromMaterial(
    scheme: ColorScheme = MaterialTheme.colorScheme,
    isDark: Boolean = scheme.background.luminance() < 0.5f,
): MetroColors = remember(scheme, isDark) {
    MetroColors(
        accent = scheme.primary,
        background = if (isDark) Color.Black else Color.White,   // Metro всегда абсолютный фон
        chrome = scheme.surfaceContainer,
        foreground = if (isDark) MetroPalette.DarkForeground else MetroPalette.LightForeground,
        subtleForeground = if (isDark) MetroPalette.DarkSubtle else MetroPalette.LightSubtle,
        disabledForeground = if (isDark) MetroPalette.DarkDisabled else MetroPalette.LightDisabled,
        contrastForeground = scheme.onPrimary,
        border = if (isDark) MetroPalette.DarkBorder else MetroPalette.LightBorder,
        inactive = if (isDark) MetroPalette.DarkInactive else MetroPalette.LightInactive,
        semitransparent = if (isDark) MetroPalette.DarkSemitransparent else MetroPalette.LightSemitransparent,
        isDark = isDark,
    )
}

/** Metro-подтема, наследующая акцент от окружающего MaterialTheme. */
@Composable
fun MetroThemeFromMaterial(content: @Composable () -> Unit) {
    MetroTheme(colors = MetroColors.fromMaterial(), content = content)
}
```

| + | − |
|---|---|
| позволяет встраивать Metro-острова в Material-приложение | берётся только акцент; остальное — Metro-константы, т.е. интеграция односторонняя и неполная |
| хороший demo-сценарий | `background` принудительно абсолютный — визуальный шов на границе |

#### Рекомендация

**Базовая — B, плюс C как утилита.** A отбрасываем: цена (зависимость ядра от Material) несопоставима с выигрышем в эргономике, а эргономику B добирает сахаром:

```kotlin
/** Удобный шорткат: MetroTheme + адаптер одним вызовом. Живёт в metro-material-adapter. */
@Composable
fun MetroMaterialTheme(
    colors: MetroColors = if (isSystemInDarkTheme()) MetroColors.dark() else MetroColors.light(),
    typography: MetroTypography = MetroTypography.default(),
    content: @Composable () -> Unit,
) {
    MetroTheme(colors = colors, typography = typography) {
        MetroMaterialAdapter(content = content)
    }
}
```

Пользователь, которому нужен «всё сразу», пишет `MetroMaterialTheme { }` — эргономика A при архитектуре B.

Дополнительно из `compose-cupertino` заимствуем идею адаптивного примитива для кросс-платформенных приложений:

```kotlin
@Composable
fun AdaptiveWidget(
    metro: @Composable () -> Unit,
    material: @Composable () -> Unit,
    target: ThemeTarget = LocalThemeTarget.current,
) = when (target) {
    ThemeTarget.Metro -> metro()
    ThemeTarget.Material -> material()
}
```

---

## 7. Что переживает решейп темой, а что придётся переписывать

Легенда: **Т** — достаточно темы (`ColorScheme` + `Shapes` + отключённый ripple); **О** — нужна обёртка (тема + переопределение `*Defaults` — elevation, colors, contentPadding, border); **З** — нужна собственная реализация.

| Компонент M3 | Т | О | З | Причина / что именно ломается |
|---|:-:|:-:|:-:|---|
| `Surface` | | ● | | `tonalElevation`/`shadowElevation` подмешивают tint и тень; нужен враппер, форсящий `0.dp` + `RectangleShape` |
| `Button` (filled) | | | ● | Metro-кнопка — 2 dp рамка + прозрачный фон, при нажатии **инверсия** (фон=accent, текст=contrast). У M3 filled Button вшиты elevation, `CircleShape`-подобная форма и ripple. Реалистичнее `MetroButton` поверх `Modifier.clickable + border` |
| `OutlinedButton` | | ● | | ближе всего к Metro: `border = BorderStroke(1.5.dp, colors.border)`, `shape = RectangleShape`, `colors = ...`. Press-инверсия всё равно требует своего `InteractionSource`-хендлинга |
| `TextButton` | ● | | | почти нейтрален; достаточно темы |
| `Card` | | ● | | `CardDefaults.cardElevation(0.dp, …)` для **всех** состояний + `RectangleShape`. Идея карточки чужда Metro (там плитки), но технически перекрашивается |
| `Switch` | | | ● | вшиты `CircleShape` thumb, капсульный track, `thumbSizeAnim`, ripple-halo вокруг thumb. Metro `ToggleSwitch` — прямоугольный track + прямоугольный thumb. Замена |
| `Slider` | | | ● | круглый thumb + скруглённый track + halo + tick-марки; Metro-слайдер — тонкая прямая линия и прямоугольный thumb. Замена |
| `Checkbox` | | | ● | `RoundedCornerShape(2.dp)`, вшитая анимация «рисования галочки» (`CheckDrawingFraction`), ripple-halo 40 dp. Metro — квадрат 2 dp рамка + мгновенная галочка. Замена |
| `RadioButton` | | ● | | форма (круг) совпадает с Metro; достаточно `RadioButtonDefaults.colors()` + отключить halo через `LocalMinimumInteractiveComponentSize`/`interactionSource` |
| `TextField` (filled) | | | ● | вшиты плавающий label, indicator-линия с анимацией, `TextFieldDefaults.shape` со скруглением сверху. Metro-поле — прямоугольник с 2 dp рамкой и **исчезающим** placeholder. Замена |
| `OutlinedTextField` | | ● | ● | `shape = RectangleShape` работает, но label-«вырез» в рамке и анимация фокуса неустранимы через API. Обёртка даёт 80 %, полное соответствие — только замена |
| `TopAppBar` | | | ● | Metro не имеет верхнего бара: заголовок страницы — обычный текст 54 sp в потоке контента, скроллящийся вместе с ним. Концептуальная замена (`MetroPageHeader`) |
| `NavigationBar` | | | ● | Metro `ApplicationBar` — высота 54 dp, круглые иконки-кнопки с рамкой, «...» для меню, без label под иконкой по умолчанию. Замена |
| `NavigationRail` / `NavigationDrawer` | | | ● | нет аналога; Metro использует Pivot/Panorama. Замена |
| `FloatingActionButton` | | ● | | круглая форма совпадает с круглыми `ApplicationBarIconButton`; нужны `elevation = FloatingActionButtonDefaults.elevation(0.dp,…)`, `shape = CircleShape`, border 1.5 dp |
| `Snackbar` | | ● | | `SnackbarDefaults` + `RectangleShape` + `containerColor = chrome`. Metro toast появляется сверху — это уровень `SnackbarHost`, решается своим host'ом |
| `AlertDialog` | | ● | | Metro `MessageBox` — прямоугольник во всю ширину, кнопки внизу. `shape = RectangleShape`, `tonalElevation = 0.dp`, `containerColor = chrome`. Обёртки достаточно |
| `Chip` (`AssistChip`/`FilterChip`) | | ● | | `shape = RectangleShape` + `border`; вшитая leading-icon-анимация выбора терпима |
| `LinearProgressIndicator` | | ● | | Metro-полоса прямая; в 1.4 появились gap/stop-indicator — их надо занулить через параметры. Обёртка на грани замены |
| `CircularProgressIndicator` | | | ● | Metro indeterminate — **пять бегущих точек** по горизонтали, а не крутящаяся дуга. Замена (`MetroProgressDots`) |
| `Tab` / `TabRow` | | | ● | Metro Pivot: заголовки крупным кеглем, невыбранные — `inactive`, свайп между страницами с параллаксом заголовков. `TabRow` + `HorizontalPager` даёт лишь грубое приближение. Замена |
| `Divider` / `HorizontalDivider` | ● | | | |
| `Icon` / `Text` | ● | | | полностью управляются `LocalContentColor` / `LocalTextStyle` |
| `Scaffold` | | ● | | нейтрален, но `containerColor` надо явно занулить в `background` |

Сводка: **темой** обходятся ~4 компонента, **обёрткой** ~10, **заменой** ~11. Практический вывод: `metro-material-adapter` полезен для чужих Material-виджетов (пикеры дат, автокомплиты), но ядро UI-кита Metro должно быть написано с нуля в `metro-core`.

---

## 8. Мультиплатформенность

### 8.1 Что уходит в `expect/actual`

| Функциональность | common-API | android | jvm (desktop) | ios | wasmJs / js |
|---|---|---|---|---|---|
| Системный акцент | `rememberSystemAccentColor(): Color?` | `system_accent1_600` (API 31+) | реестр DWM / WinRT `UISettings`; macOS `NSColor.controlAccentColor` | `null` | `null` |
| Тактильная отдача | `rememberHaptics(): MetroHaptics` | `HapticFeedbackConstants` через `LocalHapticFeedback` | no-op | `UIImpactFeedbackGenerator` | no-op (Vibration API — `[проверить]`) |
| Back-навигация | `MetroBackHandler(enabled, onBack)` | `BackHandler` / `PredictiveBackHandler` | обработка `Esc` в `onKeyEvent` | свайп-жест + `UINavigationController` | `popstate` / `history` |
| Размер экрана и ориентация | `rememberMetroWindowClass(): MetroWindowSizeClass` | `LocalConfiguration` | размер окна | `UIScreen`/traits | `window.innerWidth` |
| Шрифты | `rememberMetroFontFamily()` | compose-resources | compose-resources | compose-resources | compose-resources |
| Системные бары / safe area | `Modifier.metroSafeArea()` | `WindowInsets.systemBars` | нет вставок | `WindowInsets.safeDrawing` | нет вставок |
| Открытие URL | `MetroUriHandler` | `LocalUriHandler` | `Desktop.browse` | `UIApplication.openURL` | `window.open` |

Важно: **`isSystemInDarkTheme()` уже мультиплатформенный** в CMP 1.12 — свой `expect` не нужен.

### 8.2 Структура Gradle-модулей

```
metro-compose/
├── build-logic/                     # convention plugins: kmp, publish, bcv
├── gradle/libs.versions.toml
├── metro-core/                      # ← ядро, БЕЗ material3
│   └── src/{commonMain, androidMain, jvmMain, iosMain, wasmJsMain}
│        theme/         MetroTheme, MetroColors, MetroTypography, MetroMetrics, MetroMotion
│        indication/    TiltIndication, NoIndication, Modifier.metroTilt
│        foundation/    MetroSurface, MetroText, MetroIcon
│        components/    MetroButton, MetroToggle, MetroCheckBox, MetroSlider,
│                       MetroTextBox, MetroPivot, MetroPanorama, MetroAppBar,
│                       MetroProgressDots, MetroTile, MetroLongListSelector
│        platform/      expect/actual (акцент, гаптика, back, window class)
├── metro-resources/                 # шрифты Selawik (OFL), лицензия в артефакте
├── metro-icons/                     # Segoe MDL2-подобные иконки как ImageVector (СВОЯ отрисовка!)
├── metro-material-adapter/          # M3 1.4.x
├── metro-material-adapter-next/     # M3 1.5.0-alpha, @ExperimentalMetroApi
└── sample-gallery/                  # KMP-приложение: android + desktop + ios + wasm
```

Зависимости (строго односторонние):
```
metro-resources ← metro-core ← metro-material-adapter ← sample-gallery
metro-icons     ← metro-core
```

`metro-icons`: **нельзя** копировать глифы Segoe MDL2 Assets / Segoe Fluent Icons (проприетарный шрифт). Иконки рисуются заново как `ImageVector` в стиле Metro (1.5 dp штрих, без заливки, квадратная сетка 32×32) — это отдельная работа и отдельный риск по срокам.

### 8.3 Таргеты

| Таргет | Приоритет | Замечания |
|---|---|---|
| `androidTarget()` | P0 | minSdk 24 (Compose-минимум); dynamic accent с 31 |
| `jvm("desktop")` | P0 | Windows — основной сценарий (нативный акцент), macOS/Linux — базово |
| `iosX64`, `iosArm64`, `iosSimulatorArm64` | P1 | tilt через Skia работает; back-жест — свой |
| `wasmJs` | P1 | проверить 3D-transform и загрузку шрифтов |
| `js(IR)` | P2 | по остаточному принципу, если нет затрат |
| `macosX64/Arm64` (native) | P3 | CMP-поддержка экспериментальна |

```kotlin
// build-logic: convention plugin
kotlin {
    androidTarget { publishLibraryVariants("release") }
    jvm("desktop")
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "MetroCore"; isStatic = true }
    }
    @OptIn(ExperimentalWasmDsl::class) wasmJs { browser() }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(compose.components.resources)
            }
        }
        // skiko-общий код (desktop + ios + wasm) — если понадобится
        val skikoMain by creating { dependsOn(commonMain) }
    }
}
```

---

## 9. Тестирование и тулинг

### 9.1 Скриншот-тесты

| Инструмент | Таргеты | Годится? |
|---|---|---|
| **Roborazzi** | Android (Robolectric) + **JVM/desktop** (`roborazzi-compose-desktop`) | **да** — основной инструмент; покрывает Android и desktop одним набором тестов |
| **Paparazzi** | только Android, только View/Compose-Android | нет для CMP; можно оставить как дополнительный Android-only слой, но дублирование с Roborazzi не оправдано |
| **androidx `screenshot` Gradle plugin** | Android Compose Previews | alpha; Android-only; полезен как быстрая проверка превью — `[проверить]` статус на 2026 |
| **JetBrains первопартийный screenshot testing для CMP** | — | по состоянию знаний **отсутствует / экспериментален**; **`[проверить]`** обязательно |
| **iOS** | XCUITest + снапшот `ComposeUIViewController` | своя обвязка; вероятно, отложить до P1 |

План: базовый набор — Roborazzi на `desktop` (быстрее всего, без эмулятора), плюс Roborazzi/Robolectric на Android для проверки Android-специфики (`includeFontPadding`, dynamic accent).

```kotlin
// metro-core/src/desktopTest/kotlin/.../ThemeScreenshotTest.kt
class ThemeScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun allAccentsDark() {
        compose.setContent {
            MetroTheme(colors = MetroColors.dark()) {
                Column {
                    MetroAccents.All.forEach { (name, color) ->
                        MetroAccentScope(color) { AccentSwatchRow(name) }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/screenshots/accents_dark.png")
    }
}
```

Обязательные скриншот-кейсы:
- 20 акцентов × dark/light — регресс палитры и `contrastOn`;
- вся типографическая рампа с русским и латинским текстом — детект отсутствующих глифов;
- baseline-grid overlay поверх Win8-рампы — проверка `lineHeightStyle`;
- tilt в 5 точках (центр, 4 угла) — фиксация геометрии `TiltIndication`;
- сравнение Metro-компонента и его Material-аналога под адаптером.

Дополнительно — **не-скриншотные** тесты:
```kotlin
@Test
fun everyAccentMeetsWcagAa() {
    MetroAccents.All.forEach { (name, accent) ->
        val fg = contrastOn(accent)
        assertTrue("$name: ${contrastRatio(accent, fg)}", contrastRatio(accent, fg) >= 4.5f)
    }
}
```
(этот тест почти наверняка **упадёт** на `lime`/`amber`/`yellow` даже с override — см. §10, вопрос 6).

### 9.2 Previews

`@Preview` для CMP — `org.jetbrains.compose.ui.tooling.preview.Preview` в `commonMain`, зависимость `compose.uiTooling` / `compose.components.uiToolingPreview`. Рендер common-превью поддерживается в IntelliJ IDEA и Android Studio (для Android-source-set — нативно; для common — через плагин Compose Multiplatform). **`[проверить]`** качество поддержки на 2026 и необходимость дублировать превью в `androidMain`.

Практика: галерея `sample-gallery` как «живое превью» важнее IDE-превью — она работает на всех таргетах и служит одновременно ручной приёмкой.

### 9.3 Binary compatibility

Два варианта:
1. **`org.jetbrains.kotlinx:binary-compatibility-validator`** (Gradle plugin, `apiDump`/`apiCheck`, файлы `api/*.api`) — проверенный путь, ровно как `api-dump` в Metro-Compose.
2. **Встроенный в KGP `abiValidation`** DSL (`kotlin { abiValidation { enabled.set(true) } }`) — новее, поддерживает KLib-таргеты «из коробки». **`[проверить]`** зрелость в Kotlin 2.4.

```kotlin
// build-logic
apiValidation {
    ignoredProjects += listOf("sample-gallery")
    nonPublicMarkers += "io.metro.theme.InternalMetroApi"
}
```

`apiCheck` — обязательный шаг в CI на каждом PR. Публичные `expect`-функции попадают в KLib-дампы, поэтому `abiValidation`/BCV с KLib-поддержкой предпочтительнее, если стабилен.

### 9.4 Публикация

`com.vanniktech.maven.publish` (0.3x) → Maven Central через Central Portal.

```kotlin
// build-logic/src/main/kotlin/metro.publish.gradle.kts
mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
    coordinates("io.github.<org>", project.name, project.version.toString())
    pom {
        name.set(project.name)
        description.set("Metro (Windows Phone 8 / Windows 8) design system for Compose Multiplatform")
        licenses { license { name.set("Apache-2.0"); url.set("https://www.apache.org/licenses/LICENSE-2.0") } }
        // metro-resources — отдельная секция: шрифт Selawik под SIL OFL-1.1
    }
}
```

Лицензионная гигиена: `metro-resources` содержит OFL-шрифт, поэтому в артефакт кладём `META-INF/licenses/OFL.txt` и указываем двойное лицензирование (код — Apache-2.0, ресурсы — OFL-1.1). Никакого Segoe в репозитории — даже в тестовых ассетах.

### 9.5 CI

| Джоба | Что делает |
|---|---|
| `check` | `apiCheck` + unit-тесты common/jvm |
| `screenshot` | `verifyRoborazziDesktop` + `verifyRoborazziDebug` |
| `build-all-targets` | компиляция всех таргетов, включая iOS и wasm |
| `sample` | сборка `sample-gallery` под android/desktop/wasm; wasm-артефакт публикуется на GitHub Pages как живая демка |
| `publish` | по тегу `v*`, только с `main` |

---

## 10. Открытые вопросы и решения до старта

| # | Вопрос | Варианты | Блокирует |
|---|---|---|---|
| 1 | ~~**Кириллица в Selawik**~~ — **ПРОВЕРЕНО: кириллицы нет** (code pages 1252/1250/1254/1257, 1251 отсутствует). Вопрос переформулирован: **чем набирать кириллицу?** | (a) Selawik + Inter в fallback — быстро, но разный ритм; (b) только Inter — единый ритм, но метрики не сегоевские; (c) форк Selawik + дорисовка глифов — отдельный шрифтовой проект; (d) системный Segoe на Windows + Selawik/Inter на остальных | §4.4, весь русскоязычный UI. **Блокер, спайк 3** |
| 2 | **Коэффициент 0.75** для перевода Metro-px в sp/dp — принимаем как канон? | (a) 0.75 везде; (b) 0.75 для текста, 1.0 для Win8-сетки (крупные экраны); (c) параметризовать `MetroMetrics.scale` и дать оба пресета | §4.1, все метрики |
| 3 | **`LayoutModifierNode` внутри `IndicationNodeFactory`** — поддерживаемый сценарий? | (a) работает → оставляем; (b) не работает → публичный `Modifier.metroTilt` + `NoIndication` в `LocalIndication` | §5.4/5.5 |
| 4 | **`translationZ` vs scale-эмуляция утапливания** — принимаем отклонение от оригинала? | (a) scale (рекомендуется, совпадает с WinJS 0.975); (b) точный порт через `graphicsLayer` с ручной матрицей | §5.3 |
| 5 | **Одна или две ветки Material-адаптера** (1.4.x и 1.5-alpha)? | (a) только 1.4.x до стабилизации 1.5; (b) обе сразу | §1.3, §6.4 |
| 6 | **Что делать с `lime`/`amber`/`yellow`**, где ни белый, ни чёрный текст не дают WCAG AA? | (a) чёрный + принять 3.9:1 (историческая достоверность); (b) затемнять акцент под текст; (c) исключить из дефолтного пресета; (d) сделать a11y-режим темы | §3.2/3.3, тест из §9.1 |
| 7 | **Абсолютный чёрный фон** (`#000`) в dark — оставляем? На OLED — аутентично, на LCD/десктопе — жёстко | (a) абсолютный (аутентично); (b) `#0A0A0A` на desktop/web | §3.1 |
| 8 | **`metro-icons`** — рисуем ~120 иконок с нуля? Кто и когда? | (a) минимальный набор (~30) в v0.1; (b) полный набор позже; (c) внешняя OFL-библиотека Fluent-подобных иконок, если найдётся совместимая по лицензии | §8.2, сроки |
| 9 | **Объём v0.1**: только тема + tilt, или сразу набор компонентов? | (a) v0.1 = `MetroTheme` + `TiltIndication` + `MetroText/Surface/Button` + адаптер; (b) сразу Pivot/Panorama/LongListSelector | планирование |
| 10 | **Win8-ветка** (крупные экраны, 120 px margin, Header 42 sp) — часть v1 или отдельный трек? | (a) только токены `win8` в типографике/метриках, компонентов нет; (b) полноценный desktop-профиль темы | §4.3, §8.3 |
| 11 | **Данные `MetroTypography.default()`** — composable (из-за `Font(...)` в compose-resources) или чистая функция? | зависит от API compose-resources 1.12 — `[проверить]` | §4.4, тестируемость |
| 12 | **Статус screenshot-testing для CMP на 2026** | Roborazzi (desktop+android) как база; iOS/wasm — своя обвязка или отложить | §9.1 |
| 13 | **Название и координаты артефактов** (`io.github.<org>` vs собственный домен через Central Portal) | — | §9.4 |
| 14 | **Аутентичность vs a11y** — общая политика. Metro нарушает несколько современных норм (мелкие таргеты, низкий контраст subtle-текста 40 %, отсутствие фокус-индикаторов) | (a) аутентичность по умолчанию + `MetroAccessibilityOverrides`; (b) a11y по умолчанию + `strictMetro = true` | сквозное решение |

---

## Источники

- Compose Multiplatform (JetBrains) — https://github.com/JetBrains/compose-multiplatform
- Compose Multiplatform releases — https://github.com/JetBrains/compose-multiplatform/releases
- Compose Multiplatform resources (документация) — https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-resources.html
- androidx Compose Material3 releases — https://developer.android.com/jetpack/androidx/releases/compose-material3
- Material 3 color roles — https://m3.material.io/styles/color/roles
- `Indication` / `IndicationNodeFactory` — https://developer.android.com/reference/kotlin/androidx/compose/foundation/IndicationNodeFactory
- `GraphicsLayerScope` (cameraDistance, rotationX/Y, translationZ) — https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/GraphicsLayerScope
- `LineHeightStyle` — https://developer.android.com/reference/kotlin/androidx/compose/ui/text/style/LineHeightStyle
- Fix Compose text vertical alignment (`includeFontPadding`) — https://android-developers.googleblog.com/2021/10/compose-text-fontpadding.html
- compose-fluent-ui — https://github.com/Konyaco/compose-fluent-ui
- Metro-Compose (louis993546) — https://github.com/louis993546/Metro-Compose
- compose-cupertino (alexzhirkevich) — https://github.com/alexzhirkevich/compose-cupertino
- Selawik (Microsoft, SIL OFL-1.1) — https://github.com/microsoft/Selawik
- SIL Open Font License 1.1 — https://openfontlicense.org/
- Windows Phone Toolkit `TiltEffect.cs` — https://github.com/microsoft-toolkit/WindowsPhoneToolkit (исторически: https://phone.codeplex.com/)
- Theme resources for Windows Phone 8 — https://learn.microsoft.com/en-us/previous-versions/windows/apps/ff769552(v=vs.105)
- Theme resources: fonts and text (WP8) — https://learn.microsoft.com/en-us/previous-versions/windows/apps/ff769552(v=vs.105)#BKMK_FontStyles
- Windows 8 typography guidelines — https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh465447(v=win.10)
- Windows 8 layout / silhouette & grid — https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh872191(v=win.10)
- WinJS Animation Library — https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh465165(v=win.10)
- Roborazzi — https://github.com/takahirom/roborazzi
- Paparazzi — https://github.com/cashapp/paparazzi
- Compose Preview Screenshot Testing (androidx) — https://developer.android.com/studio/preview/compose-screenshot-testing
- binary-compatibility-validator — https://github.com/Kotlin/binary-compatibility-validator
- vanniktech gradle-maven-publish-plugin — https://github.com/vanniktech/gradle-maven-publish-plugin
- Maven Central Portal — https://central.sonatype.org/publish-ea/publish-portal-gradle/
- Windows accent color (WinRT `UISettings`) — https://learn.microsoft.com/en-us/uwp/api/windows.ui.viewmanagement.uisettings.getcolorvalue
- Android dynamic color / system accent resources — https://developer.android.com/develop/ui/views/theming/dynamic-colors

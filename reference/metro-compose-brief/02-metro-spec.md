# Metro / Modern UI — числовая спецификация

Источник истины для реализации. Всё, что здесь, должно превратиться в токены `metro-core`.

**Легенда достоверности:**
`✅` — число прочитано в первоисточнике Microsoft (MSDN/learn.microsoft.com, PDF UX guidelines, исходники MS на GitHub).
`🟡` — арифметически выведено из `✅` либо взято из вторичного MS-источника (блог, картинка).
`❌` — публичного источника не существует, значение нужно снимать реверсом.

**Важно про происхождение чисел.** В Silverlight/WP размеры задаются **в пикселях**, `px = pt × 4/3`. Логический пиксель WP = физический на WVGA 480×800. Для Compose нужен множитель `px → dp`; см. §8.

---

## 1. Типографика

### 1.1 Windows Phone — ресурсы `PhoneFontSize*` ✅

`ThemeResources.xaml` из WP SDK 7.1 + MSDN `ff769552`.

| Ресурс | px | pt |
|---|---|---|
| `PhoneFontSizeSmall` | **18.667** | 14 |
| `PhoneFontSizeNormal` | **20** | 15 |
| `PhoneFontSizeMedium` | **22.667** | 17 |
| `PhoneFontSizeMediumLarge` | **25.333** | 19 |
| `PhoneFontSizeLarge` | **32** | 24 |
| `PhoneFontSizeExtraLarge` | **42.667** | 32 |
| `PhoneFontSizeExtraExtraLarge` | **72** | 54 |
| `PhoneFontSizeHuge` | **186.667** | 140 |

Семейства: `PhoneFontFamilyNormal` = Segoe WP, `…Light` = Segoe WP Light, `…SemiLight` = Segoe WP SemiLight, `…SemiBold` = Segoe WP Semibold.
**`PhoneFontFamilyBlack` как ресурса не существует** — начертание Segoe WP Black присутствует только в WP 7.1.

### 1.2 Именованные текстовые стили WP ✅

Все `BasedOn = PhoneTextBlockBase`.

| Стиль | Семейство | px | pt | Foreground | Margin |
|---|---|---|---|---|---|
| `PhoneTextBlockBase` / `PhoneTextNormalStyle` | Normal | 20 | 15 | Foreground | 12,0 |
| `PhoneTextSubtleStyle` | Normal | 20 | 15 | **Subtle** | 12,0 |
| `PhoneTextSmallStyle` | Normal | 18.667 | 14 | **Subtle** | 12,0 |
| `PhoneTextLargeStyle` | SemiLight | 32 | 24 | Foreground | 12,0 |
| `PhoneTextExtraLargeStyle` | SemiLight | 42.667 | 32 | Foreground | 12,0 |
| `PhoneTextTitle1Style` | SemiLight | **72** | 54 | Foreground | 12,0 |
| `PhoneTextTitle2Style` | SemiLight | 32 | 24 | Foreground | 12,0 |
| `PhoneTextTitle3Style` | SemiLight | 22.667 | 17 | Foreground | 12,0 |
| `PhoneTextGroupHeaderStyle` | SemiLight | 32 | 24 | **Subtle** | 12,0 |
| `PhoneTextAccentStyle` | SemiBold | 20 | 15 | **Accent** | 12,0 |
| `PhoneTextContrastStyle` | SemiBold | 20 | 15 | **ContrastForeground** | 12,0 |
| `PhoneTextHugeStyle` | **Light** | 186.667 | 140 | Foreground | 12,0 |

Наблюдения, критичные для порта:

- `Title2` ≡ `Large`; `GroupHeader` = `Large` + Subtle. Дублирование намеренное — сохранить оба имени.
- Margin у **всех** стилей `12,0` (гориз. 12, верт. 0).
- **Ни один стиль не задаёт `LineHeight`, `LineStackingStrategy` и `FontWeight`.** Вес выбирается исключительно семейством. `LineStackingStrategy` по умолчанию `MaxHeight` — межстрочный берётся из метрик шрифта. В Compose это означает `lineHeight = TextUnit.Unspecified` (см. §8.3 в `04-architecture.md`).
- Не существуют: `PhoneTextEmphasisStyle`, `PhoneTextHighContrastStyle`, `PhoneDisabledOpacity`.

### 1.3 Windows 8 / WinRT type ramp ✅

`StandardStyles.xaml` + `dn518235`. Слева имя 8.0, справа 8.1.

| Стиль | Роль | px | pt | Weight | LineHeight |
|---|---|---|---|---|---|
| `HeaderTextStyle` / `HeaderTextBlockStyle` | Page header | **56** | **42** | Light | **40** |
| `SubheaderTextStyle` / `SubheaderTextBlockStyle` | Page subheader | **26.667** | **20** | Light | **30** |
| `TitleTextStyle` / `TitleTextBlockStyle` | Item title | 14.667 | 11 | **SemiBold** | 20 |
| `SubtitleTextStyle` / `SubtitleTextBlockStyle` | Navigation | 14.667 | 11 | Normal | 20 |
| `BodyTextStyle` / `BodyTextBlockStyle` | Body | 14.667 | 11 | **SemiLight** | 20 |
| `CaptionTextStyle` / `CaptionTextBlockStyle` | Tertiary | **12** | **9** | Normal | 20 |
| `GroupHeaderTextStyle` | Group header | 26.667 | 20 | Light | 30 |

- База `BaselineTextStyle`: `LineHeight=20`, **`LineStackingStrategy=BlockLineHeight`**, `RenderTransform=Translate(-1, 4)` — принудительная посадка на 20px-сетку. Header: `Translate(-2, 8)`, Subheader: `Translate(-1, 6)`.
- `ControlContentThemeFontSize` = **14.667** (ровно 11pt), `ContentControlThemeFontFamily` = **Segoe UI**, `SymbolThemeFontFamily` = Segoe UI Symbol.
- Обязательные OpenType-фичи Win8: `Typography.StylisticSet20=True`, `DiscretionaryLigatures=True`, `CaseSensitiveForms=True`. Page header — **Segoe UI Stylistic Set 20, Light**.
- Отступы: page header `0,0,30,40`, subheader `0,0,0,40`, snapped header `0,0,18,40`.
- 8.1: `HubHeaderThemeFontSize=56`, `HubSectionHeaderThemeFontSize=26.667` (SemiLight, margin `0,0,0,20`), `SettingsFlyoutHeaderThemeFontSize=26.667`, `ToolTipContentThemeFontSize=12`, `SemanticZoomButtonFontSize=14.667`, `TextControlThemeMinHeight=32`.

**Ходовые ошибки, которые тут исправлены:** Title — **SemiBold**, не Bold. Caption 12px = **9pt**, не 12pt. Рампа в CSS-классах `win-type-xx-large`…`win-type-x-small` = h1…h6 = 42/20/11/11/11 pt ✅; их `line-height` и числовые веса ❌.

---

## 2. Сетка и layout

### 2.1 Windows 8 «силуэт» ✅ (MSDN `hh872191`, дословно)

> «One unit equals **20 × 20 pixels**. Each unit is further divided into sub-units of **5 × 5 pixels**. There are **16 sub-units** per square unit.»

| Параметр | px | units |
|---|---|---|
| Базовая линия page header от верха | **100** | 5 |
| Левое поле page header | **120** | 6 |
| Верхнее поле контент-региона | **140** | 7 |
| Левое поле контент-региона | **120** | 6 |
| Нижнее поле (горизонтальный панинг) | **50…130** | 2.5…6.5 |
| Паддинг hard-edged элемент ↔ текст | **10** | 2 sub |
| Между колонками списков | **40** | 2 |
| Между колонками hard-edged | **10** | 2 sub |
| Верт. паддинг между элементами (tile + text) | **20** | 1 |
| Верт. паддинг между элементами (hard-edged) | **10** | 2 sub |
| Между группами (гориз.) | **80** | 4 |

Реализация в HTML: `-ms-grid-columns: 120px 1fr; -ms-grid-rows: 100px 40px 1fr`.
Back-кнопка (`BackButtonStyle`): 48×48, `Margin="36,0,36,36"` — 36 + 48 + 36 = **120**, поле силуэта сходится. `FontSize=56`, глиф `&#xE071;`, Segoe UI Symbol. Snapped-вариант: 36×36, `FontSize=26.667`, глиф `&#xE0C4;`.

**View states.** Win8: `FullScreenLandscape / Filled / Snapped / FullScreenPortrait`; snapped = **320 px**; snap доступен только при ширине окна ≥ **1366**; минимум для Store-приложений **1024×768**. Win 8.1: min width **500 px** по умолчанию, opt-in **320 px** через `<ApplicationView MinWidth="width320"/>`. При ширине < 500 px левое поле **20 px** вместо 120, header **20pt** вместо 42pt, back-кнопка 30×30. Плато масштабирования: **100 / 140 / 180 %**.

### 2.2 Windows Phone

| Параметр | Значение | |
|---|---|---|
| Канва | **480 × 800** (768 при видимом трее) | ✅ |
| `TitlePanel` | `Margin="12,17,0,28"` | ✅ |
| `ApplicationTitle` | `PhoneTextNormalStyle`, без локального margin | ✅ |
| `PageTitle` | `Margin="9,-7,0,0"`, `PhoneTextTitle1Style` (72 px) | ✅ |
| `ContentPanel` | `Margin="12,0,12,0"` | ✅ |
| Откуда берётся «24 px» | 12 (ContentPanel) + 12 (`PhoneHorizontalMargin` в стиле) | ✅ |
| Status bar | **32 px** portrait / **72 px** landscape | ✅ |
| ApplicationBar | **72 px** fixed, `MiniSize` = **30.0 px** | ✅ |
| Клавиатура | 336 px portrait / 256 px landscape; полоса подсказок 65 px | ✅ |

**Thickness-ресурсы** ✅: `PhoneHorizontalMargin`=`12,0`; `PhoneVerticalMargin`=`0,12`; `PhoneMargin`=`12`; `PhoneTouchTargetOverhang`=`12`; `PhoneTouchTargetLargeOverhang`=`12,20`; `PhoneTextBoxInnerMargin`=`1,2`; `PhonePasswordBoxInnerMargin`=`3,2`; `PhoneBorderThickness`=`3`; `PhoneStrokeThickness`=`3` (Double, не Thickness).

**Тач-цели** ✅ (UI Design and Interaction Guide v2.0, стр. 75, дословно):
> «Touch targets should not be smaller than **9 mm or 34 pixels** square and provide at least **2 mm or 8 pixels** between touchable controls… but **never more than 7 mm or 26 pixels** square.»

Touch element ≥ **60 %** от touch target. Продолговатые элементы: высота до 7 мм при ширине ≥ 20 мм. Минимальный кегль — **15 pt**. Отсюда ≈ **3.78 px/мм** 🟡 (Microsoft эту константу принципиально не публиковал).

### 2.3 Pivot и Panorama

**Pivot** ✅: рекомендовано **≤ 4 страницы** (не 5), заголовки циклические, высота хедера «fixed and can't be changed».
**Числовых метрик хедера Pivot Microsoft не публиковал вообще** ❌ — шаблон вшит в `Microsoft.Phone.dll`. Снимать реверсом (ILSpy) либо через Blend «Edit a Copy» на машине с WP8 SDK.

**Panorama** ✅: **≤ 5 секций**, только portrait, wrap-around, фон **480×800 … 1024×800**, для четырёх секций пропорция 16:9.
`PanoramaItem` template ✅: корневой `Grid Margin="12,0,0,0"`; хедер `FontSize={PhoneFontSizeExtraExtraLarge}` = **72**, `PhoneFontFamilySemiLight`, `Margin="10,-2,0,26"`.
Параллакс описан только качественно (секции движутся 1:1 с пальцем, фон медленнее, title ещё медленнее); коэффициента нет ❌. Рабочая формула 🟡:

```
bgOffset = contentOffset × (bgWidth − screenWidth) / (contentWidth − screenWidth)
```

Peek следующей секции ❌ — ходовые «48 px» ничем не подтверждены.

### 2.4 LongListSelector ✅ (`jj244365`)

| Элемент | Метрика |
|---|---|
| Jump-list | `GridCellSize="113,113"`, плитка **113×113**, `Margin=6` → шаг 125, зазор 12 |
| Буква в jump-list | `FontSize=48`, SemiBold, `Padding=6` |
| Групповой заголовок в списке | квадрат **62×62**, `BorderThickness=2` (accent), `Margin="0,0,18,0"`, прозрачный padding 5 → хит 72×72 |
| Буква в заголовке | 48 px, SemiLight |
| Когда применять | при **≥ 8** элементах; при ≤ 4 — RadioButton |

Sticky-заголовки ❌ — нигде не заявлены как поведение WP.

---

## 3. Плитки

### 3.1 Windows Phone 8 ✅ (`hh202948`, `jj662924`)

Ассеты авторятся **только в WXGA**, ОС масштабирует на WVGA/720p.

| Размер | Flip / Cycle | Iconic | Логические px 🟡 |
|---|---|---|---|
| Small | **159 × 159** | 110×110 (best fit 70×110) | 99 × 99 |
| Medium | **336 × 336** | 202×202 (best fit 130×202) | 210 × 210 |
| Wide | **691 × 336** | — | 432 × 210 |

Множители ✅: WXGA ×1.6, 720p ×1.5.
Зазор между плитками **12 логических px** 🟡 — единственное значение, при котором 99 + 12 + 99 = 210 и 210 + 12 + 210 = 432.
Padding внутри ассетов 🟡 (официальные PSD): small 43, medium 101, wide 279×101. `ApplicationIcon` 99×99 с padding 14; `LockIcon` 38×38 ✅.
Лимиты текста ✅: medium Title 19 симв., wide Title 39, BackContent 3×13 / 3×27.
WP7 legacy: **173×173**, один-единственный размер ✅.
Позиция Title и «пузыря» счётчика ❌ — существует только на растровых схемах MSDN (`jj662925.ux_concept_tile_fliptemplatesizing.png`, `jj662924.ux_concept_tile_iconictemplatesizing.png`), их достаточно открыть глазами.

### 3.2 Windows 8 / 8.1 ✅ (`hh781198`)

| Ассет | 80 % | 100 % | 140 % | 180 % |
|---|---|---|---|---|
| Square150x150 | 120 | **150** | 210 | 270 |
| Wide310x150 | 248×120 | **310×150** | 434×210 | 558×270 |
| Square70x70 (8.1) | 56 | **70** | 98 | 126 |
| Square310x310 (8.1) | 248 | **310** | 434 | 558 |
| Square30x30 | 24 | **30** | 42 | 54 |
| Store logo | — | **50** | 70 | 90 |
| Badge | — | **24** | 33 | 43 |
| Splash | — | **620×300** | 868×420 | 1116×540 |

В Windows **8.0** существовали только 150×150 и 310×150; 70×70 и 310×310 — новинки **8.1** ✅.
Лимит любого изображения **≤ 1024×1024 и ≤ 200 KB** ✅.
Сетка Start: шаг строки **160 px** «including padding», фиксированные верх + низ **215 px**, максимум **6 строк** ✅ → **зазор 10 px @100 %** 🟡 (160 − 150; согласуется с 70 + 10 + 70 = 150 и 150 + 10 + 150 = 310).
Внутренние отступы плиток Windows 8/8.1 ❌. Единственная опубликованная таблица padding — WP8.1 ✅: 71×71 → `19,19`; 150×150 → `45,45`; 310×150 → `125,45`; 44×44 → `6,6`.

### 3.3 Тайминги live-плиток — подтверждённое отсутствие

`hh781199` дословно: «The amount of time each notification in the queue is displayed and the order… **cannot be controlled by apps**.»
`jj662925`: «the Tile flips at a **random** interval».
Что документировано ✅: очередь = **5**, FIFO; срок жизни push/periodic **3 дня**; periodic poll от 30 мин до Daily с задержкой до 15 мин.

**Вывод для библиотеки:** любые «плитка переворачивается раз в N секунд» — блогерские наблюдения. Мы задаём собственный дефолт и делаем его параметром API.

---

## 4. Цвет

### 4.1 Тема WP: dark / light ✅

`ThemeResources.xaml` SDK 7.1 + рантайм-дамп WP8 SDK.

| Ресурс | Dark | α | Light | α |
|---|---|---|---|---|
| `PhoneForegroundColor` | `#FFFFFFFF` | 100 % | **`#DE000000`** | **87 %** |
| `PhoneBackgroundColor` | `#FF000000` | 100 % | `#FFFFFFFF` | 100 % |
| `PhoneContrastForegroundColor` | `#FF000000` | 100 % | `#FFFFFFFF` | 100 % |
| `PhoneContrastBackgroundColor` | `#FFFFFFFF` | 100 % | `#DE000000` | 87 % |
| `PhoneDisabledColor` | `#66FFFFFF` | **40 %** | `#4D000000` | **30 %** |
| `PhoneSubtleColor` | `#99FFFFFF` | **60 %** | `#66000000` | **40 %** |
| `PhoneChromeColor` | `#FF1F1F1F` | 100 % | `#FFDDDDDD` | 100 % |
| `PhoneSemitransparentColor` | `#AA000000` | ≈66.7 % | `#AAFFFFFF` | ≈66.7 % |
| `PhoneBorderColor` | `#BFFFFFFF` | 75 % | `#BFFFFFFF` ⚠️ | 75 % |
| `PhoneInactiveColor` | `#33FFFFFF` | 20 % | `#33000000` | 20 % |
| `PhoneTextBoxColor` | `#BFFFFFFF` | 75 % | `#26000000` | 15 % |
| `PhoneTextBoxEditBackgroundColor` | `#FFFFFFFF` | 100 % | `#00000000` | 0 % |
| `PhoneTextBoxReadOnlyColor` | `#77000000` | ≈47 % | `#2E000000` | 18 % |

> **Ключевой инсайт: light-тема — не механическая инверсия dark.**
> Foreground = 87 % чёрного (а не 100 % белого), Subtle = 40 % (против 60 %), Disabled = 30 % (против 40 %).
> Если сгенерировать light инверсией, получится «не Metro» — и это первое, что заметит человек, который держал в руках Lumia.

Плюс ✅: `PhoneDarkThemeOpacity` = 1/0, `PhoneLightThemeOpacity` = 0/1, `PhoneDarkThemeVisibility` = Visible/Collapsed.
⚠️ `PhoneBorderColor` в дампе одинаков в обеих темах — вероятно артефакт дампа; перепроверить перед фиксацией токена.

### 4.2 Акцентные цвета WP8

Дефолт системы — **тёмный фон + синий акцент** ✅.
Первичными источниками подтверждены только два значения: `#FF1BA1E2` (Cyan/Blue) ✅ и `#FFE51400` (Red) ✅.
**Полную палитру Microsoft публиковал исключительно картинкой** (`themes_concept_accentcolors.png`), поэтому вся таблица ниже — 🟡, но она консистентна между всеми независимыми источниками:

| Имя | Hex | | Имя | Hex |
|---|---|---|---|---|
| Lime | `#A4C400` | | Orange | `#FA6800` |
| Green | `#60A917` | | Amber | `#F0A30A` |
| Emerald | `#008A00` | | Yellow | `#E3C800` |
| Teal | `#00ABA9` | | Brown | `#825A2C` |
| **Cyan** | **`#1BA1E2`** ✅ | | Olive | `#6D8764` |
| Cobalt | `#0050EF` | | Steel | `#647687` |
| Indigo | `#6A00FF` | | Mauve | `#76608A` |
| Violet | `#AA00FF` | | Taupe | `#87794E` |
| Pink | `#F472D0` | | Magenta | `#D80073` |
| Crimson | `#A20025` | | **Red** | **`#E51400`** ✅ |

Ранняя палитра WP7 ✅ (UI Guide v2.0: 10 акцентов × 2 фона = 20 тем): Blue `1BA1E2`, Orange `F09609`, Green `339933`, Red `E51400`, Veridian `00ABA9`, Pink `E671B8`, Purple `A200FF`, Brown `996600`, Lime `8CBF26`, Magenta `FF0097`.

### 4.3 Windows 8.1 ✅ (`dn518235`)

`AppBarBackgroundThemeBrush` `#FF000000` / `#FFF0F0F0`; `AppBarSeparatorForegroundThemeBrush` `#FF7B7B7B` (обе темы); `FlyoutBackgroundThemeBrush` `#FF000000` / `#FFFFFFFF`; `SettingsFlyoutHeaderBackgroundThemeBrush` `#FF464646`.
AppBarButton: PointerOver `#21FFFFFF`, Pressed — эллипс `#FFFFFFFF` + глиф `#FF000000`, Disabled `#66FFFFFF`.

---

## 5. Моушн

### 5.1 Windows Phone Toolkit — page transitions ✅

Исходники `microsoftarchive/WindowsPhoneToolkit`, `Transitions/Storyboards/*.xaml`.

**Turnstile** — `PlaneProjection.CenterOfRotationX = 0`, ось вращения = левый край экрана.

| Mode | RotationY | Duration | Easing |
|---|---|---|---|
| ForwardIn | −80° → 0° | **350 ms** | ExponentialEase **EaseOut, Exp = 6** |
| ForwardOut | 0° → **+50°** | **250 ms** | ExponentialEase EaseIn, Exp = 6 |
| BackwardIn | **+50°** → 0° | **350 ms** | ExponentialEase EaseOut, Exp = 6 |
| BackwardOut | 0° → **−80°** | **250 ms** | ExponentialEase EaseIn, Exp = 6 |

Opacity — ступенька: In на 10 ms, Out на 240 → 250 ms.

**TurnstileFeather** — строится в коде, `TurnstileFeatherEffect.cs`:

- stagger = **40 ms × index** для ForwardIn / BackwardOut, **50 ms × index** для ForwardOut / BackwardIn;
- геометрия пера: `CenterOfRotationX = −0.2`, `GlobalOffsetY = −offset` + `TranslateTransform.Y = +offset`, где `offset = rootHeight/2 − (elementTop + elementHeight/2)`;
- следствие: **все элементы вращаются вокруг одной общей оси, проходящей через вертикальный центр экрана** — тот самый эффект жалюзи. Сортировка по `FeatheringIndex`.

**Slide** — дистанция везде **200 px**, easing везде `ExponentialEase EaseOut Exp = 6`:
LeftFadeIn / RightFadeIn **500 ms**, LeftFadeOut / RightFadeOut **300 ms**, UpFadeIn / DownFadeIn **350 ms**, UpFadeOut / DownFadeOut **250 ms**.

**Swivel** — `RotationX`, центр 0.5/0.5:
ForwardIn −45° → 0°, 350 ms (EaseOut Exp = 6);
ForwardOut 0° → +90°, 250 ms (**EaseIn Exp = 15** — уникальное значение во всём тулките);
BackwardIn −45° → −30° (200 ms, линейно) → 0° (350 ms);
BackwardOut 0° → +60°, 250 ms Exp = 6;
FullScreenIn −30° → 0°, 350 ms; FullScreenOut 0° → +30°, 250 ms (**Exp = 1**).

**Rotate** — все 8 режимов **250 ms**, `ExponentialEase Exp = 1` для трансформа и **`SineEase EaseOut`** для Opacity.
**Roll** — 0° → 45° за 300 ms (EaseOut Exp = 6), затем 45° → 90° за 300 ms линейно; итого **600 ms**, opacity не анимируется.

### 5.2 Tilt effect ✅ (`TiltEffect.cs` целиком)

```
MaxAngle       = 0.3 рад = 17.188°
MaxDepression  = 25 px
ReturnDelay    = 200 ms
ReturnDuration = 100 ms

angleMagnitude = |nx − 0.5| + |ny − 0.5|
angle          = angleMagnitude * 0.3 * 180/π         // 0 … 17.188°
depression     = (1 − angleMagnitude) * 25            // 25 … 0 px
RotationY      = angle * xContrib       * (−sign(nx − 0.5))
RotationX      = angle * (1 − xContrib) * (+sign(ny − 0.5))
GlobalOffsetZ  = −depression
```

Что почти все воспроизводят неправильно:

- **Нажатие вниз анимации не имеет** — свойства выставляются мгновенно на каждом `ManipulationDelta`. Анимируется только возврат: 3 × `DoubleAnimation` (RotationX / RotationY / GlobalOffsetZ → 0), линейно по умолчанию либо `LogarithmicEase = ln(t+1)/ln 2`.
- **Scale не используется вообще** — только `PlaneProjection`. Эффект «утапливания» даёт перспектива, а не масштаб.
- `CacheMode = BitmapCache` на время наклона.
- Тач ровно в центр = 0° поворота + полные 25 px утапливания; тач в угол = 17.2° + 0 px.

### 5.3 Windows 8 / WinJS animation library ✅

MSDN чисел не публикует («preconfigured durations», параметр `Duration` игнорируется) — единственный источник истины — `winjs/src/js/WinJS/Animations.js`.

| Анимация | XAML-эквивалент | Значения | Duration | Easing |
|---|---|---|---|---|
| `fadeIn` | FadeInThemeAnimation | opacity 0→1 | **250 ms** | linear |
| `fadeOut` | FadeOutThemeAnimation | opacity →0 | **167 ms** | linear |
| `pointerDown` | PointerDownThemeAnimation | **scale(0.975)** | **167 ms** | `cubic-bezier(0.1,0.9,0.2,1)` |
| `pointerUp` | PointerUpThemeAnimation | scale → none | **167 ms** | `(0.1,0.9,0.2,1)` |
| `enterPage` | EntranceThemeTransition | translate(0,**28px**)→0 | **1000 ms**, opacity 170 ms | stagger **83 ms**, кэп **333 ms** |
| `exitPage` | — | → offset, → 0 | **117 ms** | linear |
| `enterContent` | ContentThemeTransition | translate(0,28px)→0 | **550 ms**, opacity 170 ms | `(0.1,0.9,0.2,1)` |
| `createRepositionAnimation` | RepositionThemeTransition | offset → 0 | **367 ms** | stagger 33 ms, кэп 250 ms |
| `createAddToListAnimation` | AddDeleteThemeTransition | scale(0.85) → none | **120 ms** (+240 ms delay); affected 400 ms | `(0.1,0.9,0.2,1)` |
| `createDeleteFromListAnimation` | AddDeleteThemeTransition | → scale(0.85) | **120 ms**; remaining 400 ms / delay 60 | `(0.11,0.5,0.24,0.96)` |
| `showEdgeUI` / `hideEdgeUI` | EdgeUIThemeTransition | translate(0,**−70px**) | **367 ms** | `(0.1,0.9,0.2,1)` |
| `showPanel` / `hidePanel` | PaneThemeTransition | translate(**364px**,0) | **550 ms** | `(0.1,0.9,0.2,1)` |
| `showPopup` | PopupThemeTransition | translate(0,50px) | **367 ms**; opacity 83 ms / delay 83 | `(0.1,0.9,0.2,1)` |
| `dragBetweenEnter/Leave` | DragOverThemeAnimation | — | **200 ms** | `(0.1,0.9,0.2,1)` |
| `swipeSelect/Deselect/Reveal` | — | — | **300 ms** | `(0.1,0.9,0.2,1)` |
| resize grow / shrink | ResizeThemeTransition | — | **350 / 120 ms** | `(0.1,0.9,0.2,1)` |

`staggerDelay(init, extra, factor, cap)` → `delay(i) = min(init + extra·i, cap)`.

**Continuum** (WinJS 2.0 ✅; в WP8 Silverlight числа ❌):
`continuumForwardIn` — 3 слоя по **350 ms**: page `scale(0.5)→1` `(0.33,0.18,0.11,1)`; itemRoot `translate(0,225px)→0` `(0.24,1.15,0.11,1.1575)`; itemContent `rotateX(80deg) scale(1.5)→0` `(0,0.62,0.8225,0.9625)`; все opacity `(0,2,0,2)`.
`continuumForwardOut` — page **120 ms** `scale→1.1`, item **152 ms** `rotateX(80deg) scale(1.5) translate(0,150px)`, easing `(0.3825,0.0025,0.8775,−0.1075)`.
`continuumBackwardIn` — page **200 ms** `scale(1.25)→1`, item **250 ms**.

**WinJS Turnstile:** In **300 ms** `rotateY(80deg→0)` `(0.01,0.975,0.4775,0.9775)`; Out **128 ms** `rotateY(0→−50deg)` `(0.4925,0.01,0.7675,−0.01)`; stagger 50 ms, кэп 1000 ms; `perspective(600px)`; origin `X = −(40 + item.left)`, `Y = innerHeight/2 − item.top`.

### 5.4 Каталог кривых для Compose

| Роль | cubic-bezier | Примечание |
|---|---|---|
| **Основная Metro-кривая** | `(0.1, 0.9, 0.2, 1)` | ≈ 80 % всей библиотеки WinJS |
| Exit / уход со сцены | `(0.3825, 0.0025, 0.8775, −0.1075)` | отрицательный y2 — лёгкий «подсос» |
| Opacity snap-in | `(0, 2, 0, 2)` | фактически мгновенное появление |
| Opacity snap-out | `(1, −0.42, 0.995, −0.425)` | |
| Slide-in с овершутом | `(0.17, 0.79, 0.215, 1.0025)` | |
| WP `ExponentialEase EaseOut Exp = 6` | нет прямого эквивалента | `f(t) = (1 − e^(−6t)) / (1 − e^(−6))`; реализовать как `Easing { }` |

### 5.5 ProgressBar indeterminate ✅ (`gg442303` + Jeff Wilcox)

Это **5 прямоугольников 4×4 px**, не круги. `Fill = PhoneAccentBrush`, `Opacity = 0`, `CacheMode = BitmapCache`, у каждого свой `TranslateTransform`.

- Цикл `Duration="00:00:04.4"`, `RepeatBehavior="Forever"`.
- `BeginTime` = **0 / 0.2 / 0.4 / 0.6 / 0.8 s** (stagger 200 ms).
- Кейфреймы X: 0 s → 0 %; 0.5 s → **33 %** (`ExponentialEase Exp = 1`); 2.0 s → **66 %** (linear); 2.5 s → **100 %** (`ExponentialEase Exp = 1`), затем `DiscreteDoubleKeyFrame Opacity = 0`.
- В XAML значения записаны как `0.1 / 33.1 / 66.1 / 100.1`: суффикс `.1` = «процент ширины», `.2` = «процент высоты»; пересчёт делает `RelativeAnimatingContentControl`.

⚠️ Расхождение источников: у Wilcox финальный easing = **EaseIn**, в MSDN-сэмпле — EaseOut. Авторский вариант вероятнее.

---

## 6. Контролы — ключевые метрики

**ApplicationBar (WP)** ✅
Высота **72 px** fixed; ≤ **4** иконки, ≤ **5** пунктов меню (14–20 символов). Иконка **48×48 px**, рисунок **26×26 по центру**, белый на прозрачном — **круг рисует сам бар**. Mode = `default | mini` (Minimized = 30 px); «hidden» — это `IsVisible = false`, а не режим. Текст пунктов меню **автоматически переводится в lowercase**. Дословно: «**No text-only buttons are permitted**». Opacity принимает только 0.0 / 0.5 / 1.0; при 1.0 **страница ужимается**, иначе бар лежит поверх. Высота строки меню и кегль подписей ❌.

**ToggleSwitch (Toolkit)** ✅
Хит-область **136×95**, трек **89×34**, off-заливка **77×20**, thumb **28×38** (`Margin = -4,0`, `BorderThickness = 4,0`), ход `TranslateX = 69 px`, снап **50 ms** (`VisualTransition GeneratedDuration="0:0:0.05"`), easing **`ExponentialEase EaseOut Exp = 15`**, Disabled → `Opacity 0.3`. Header = `PhoneSubtleBrush` + Normal; Content = `PhoneFontFamilyLight` + Large. Чистый тап всегда переключает; драг переключает только если ушли с края.

**ListPicker (Toolkit)** ✅
`ItemCountThreshold` default = **5**. Режимы `Normal / Expanded / Full`. Анимация раскрытия **200 ms**, `ExponentialEase EaseInOut Exp = 4`, анимируются `Height` + `TranslateTransform`. Всегда есть активный выбор — пустого состояния нет.

**DatePicker / TimePicker (Toolkit)** ✅
Открытие — **реальная навигация фрейма** с занулёнными транзишенами, не popup. Анимация страницы: Open `RotationX −50° → 0°` за **200 ms** без easing; Closed `→ 90°` за **200 ms** `ExponentialEase EaseIn`. Раскладка: трей 32 px; `HeaderTitle` SemiBold Medium `Margin="24,16,24,24"`; три `LoopingSelector Width=148 ItemSize="148,148" ItemMargin="6"`; число `FontSize=54` SemiBold `Margin="0,-8"`, подпись `FontSize=20` Subtle. AppBar: check = DONE слева, X = CANCEL справа.

**ContextMenu (Toolkit)** ✅
Открывается по `GestureListener.Hold`, позиционируется в точке касания. Фон **масштабируется 1.0 → 0.94 за 420 ms, `ExponentialEase EaseInOut`** + накладывается слой `PhoneBackgroundBrush`. **Блюра в исходниках нет вообще** — распространённое заблуждение. `IsFadeEnabled` default = `true`.

**TextBox (WP)** ✅
Fixed-height; дословно: «**It isn't possible to scroll in a TextBox control in Windows Phone**». Tap = фокус/выделение, tap-and-hold = точное позиционирование каретки. `PhoneTextBox.Hint` виден **только когда текст пуст И нет фокуса**, `Foreground = PhoneTextBoxReadOnlyBrush`. PasswordBox: введённый символ виден и маскируется **при следующем нажатии либо через 2 секунды**.

**MessageBox (WP)** ✅
`MessageBoxButton` = ровно `{OK, OKCancel}` — API физически не допускает больше двух кнопок и не знает Yes/No.
Порядок: «**OK или позитивное действие — слева, Cancel — справа**» ✅ — **противоположно Windows 8**, где «Put the safest, most conservative choice on the rightmost position» ✅. При портировании обеих систем это надо развести флагом.

**Жесты WP**
**Числовых порогов в design-документации Microsoft нет.** UI Guide даёт только определения («touch and hold — a single finger down within a bounded area **for a defined period of time**»). Единственное опубликованное число — Petzold, MSDN Magazine 03/2011 про `GestureListener`: Tap и Hold — «**about 1.1 seconds**» 🟡. Double-tap interval, pan slop, минимальная скорость флика — ❌.
Прочие тайминги ✅: скролл-индикаторы гаснут через **1 с**; индикаторы status bar — **≈8 с**; toast — **10 с**; маскирование пароля — **2 с**.

**Windows 8 AppBar / CommandBar** ✅
**88 px** с подписями (14 + 40 + 5 + 16 + 13) 🟡; `AppBarThemeMinHeight = 68` (8.1); бордер `0,2,0,0` + padding. `AppBarButton`: FullSize **100 px**, Compact **60 px**; иконка 40×40, `OutlineEllipse StrokeThickness = 2`; label `FontSize = 12`, `Width = 88`; content margin `0,14,0,13`. Явного margin между кнопками нет — зазор получается из 100 − 40 = 60. Вызов: right-click / **Win+Z** / edge swipe, показывает **оба** бара сразу; light-dismiss по умолчанию, `IsSticky = true` отключает. Верхний бар = навигация, нижний = команды. Back-кнопка вне бара: `NavigationBackButtonNormalStyle` **41×41**, Small **30×30**.

**Hub (8.1)** ✅ — горизонтальный панинг; `HubSection.isHeaderStatic` default `false` → заголовок кликабелен (`headerinvoked`) и рисует **chevron**; `Hub.zoomableView` поддерживает Semantic Zoom.

**Semantic Zoom** ✅ — pinch/stretch, Ctrl + колесо, Ctrl +/−. Переход — **cross-fade + scale, «cannot be customized»**, длительность не документирована ❌. Тап в zoomed-out скроллит к точке. «**Limit the number of pages in the zoomed-out mode to three.**» Шаблон: `MinZoomFactor=0.5`, `MaxZoomFactor=1.0`, `IsZoomInertiaEnabled=False`, `Padding=3`; кнопка zoom-out 21×21, `Margin="0,0,7,24"`, глиф `&#xE0B8;` 14.667 px, **гаснет через 3 с**.

**Settings (Win8)** ✅ — Settings pane «**always 346 pixels wide**», до **7** команд. Settings flyout — во всю высоту экрана, **narrow 346 px или wide 646 px**, «**don't create custom sizes**»; выезжает с той же стороны, что и charms; хедер = back + имя точки входа + иконка приложения; фон хедера = цвет плитки, бордер **на 20 % темнее**; контент на белом; ≤ **4** точки входа; скролл ≤ 2× высоты экрана. `SettingsFlyoutSectionStyle` — **39 px** нижний margin. Ширина charms bar ❌.

**Flyout / Menu (8.1)** ✅ — `FlyoutThemeMaxWidth = 450`, `MaxHeight = 718`, `MinHeight = 54`, `MinWidth = 70`, бордер 2, `FlyoutContentThemePadding = "20,17,20,20"`; `MenuFlyoutItemThemePadding = "20,10,20,12"` → высота пункта ≈ **42 px** 🟡; сепаратор 1 px, padding `20,9,20,10`. `MessageDialog` — **максимум 3 `UICommand`**, иначе исключение; фон всегда белый; длинные заголовки обрезаются без переноса.

---

## 7. Принципы

### 7.1 WP7 — пять принципов ✅ (UI Design and Interaction Guide v2.0, стр. 11, дословно)

> «The Metro design was developed using the five following principles:
> **1) Clean, light, open, and fast** — visually distinctive, contains ample white space, reduces clutter and **elevates typography as a key design element**.
> **2) Content, not chrome** — accentuates focus on the content that the user cares most about.
> **3) Integrated hardware and software.**
> **4) World-class motion** — hardware-accelerated animations and transitions.
> **5) Soulful and alive**…
> These design principles are based around the concept that UI elements should be **authentically digital**.»

Оттуда же: «**The content is the interface.**» (стр. 15); «Developers should use digital metaphors… and **should not necessarily try to mimic real world interaction**» (стр. 13). «Red Threads» (стр. 9): **Personal / Relevant / Connected**. Транзишены: «Built-in screen transitions and animations are **system-reserved**… but **may mimic** them.»

### 7.2 Канонический набор «Codename Metro» 🟡 (MSDN Magazine 01/2012, Mark Hopkins)

**Clean, Light, Open and Fast** · **Celebrate Typography** («Type is beautiful… The right balance of weight and positioning can create a visual hierarchy») · **Alive in Motion** («Motion is life… A good transition gives the user clues about context») · **Content, Not Chrome** («**The content is the UI**… By removing as much chrome as possible, you bring the content into focus») · **Authentically Digital** («**Don't try to simulate analog controls such as knobs**»).

⚠️ «Do More With Less» в набор **Windows Phone не входит** — это принцип Windows 8.

### 7.3 WP8 — «Be»-принципы ✅ (`hh202906`)

Be Modern («the content is the interface… undecorated, free of chrome») · Be "On the Go" Capable · Be Clean · Be in Motion · Be Simple, Readable, and Minimalistic · Be Consistent · Be Authentic · Be Innovative.

### 7.4 Windows 8.1 — пять принципов Microsoft Design ✅ (официальный PDF, © 2014, стр. 7–10)

1. **Pride in craftsmanship** — «Engineer the experience to be complete, thorough, and polished at every stage.» (+ «Use balance, symmetry, and hierarchy», «Align your app layout to the grid»)
2. **Be fast and fluid** — «Let people **interact directly with content**. Respond to actions quickly with matching energy… creating a **sense of continuity** and telling a story through meaningful use of motion.»
3. **Authentically digital** — «**Take full advantage of the digital medium**… embracing the fact that apps are pixels on a screen.» (+ «Use typography beautifully», «Use bold, vibrant colors»)
4. **Do more with less** — «**reducing your design to its essence**… leaving only the most relevant elements on screen.» (+ «**Put content before chrome**»)
5. **Win as one** — «Take advantage of what people already know, like standard touch gestures and charms.»

---

## 8. Пересчёт в Compose

### 8.1 Множитель px → dp/sp

Принимаем **× 0.75** (обоснование в `04-architecture.md`, §4). Две независимые проверки:

- WP `PhoneFontSizeNormal` 20 px × 0.75 = **15 sp** = ровно официальные 15 pt;
- WP status bar 32 px × 0.75 = **24 dp** — ровно стандартная высота статус-бара Android.

Win8-рампа при том же множителе: 56 → 42, 26.667 → 20, 14.667 → 11, 12 → 9 — **ровно официальная рампа в пунктах**. Совпадение по трём независимым осям, множитель можно считать каноном.

### 8.2 Производные значения для токенов

| Токен | Metro px | dp/sp |
|---|---|---|
| `Metrics.pageMargin` | 12 | 9 dp *(округлять? см. открытые вопросы)* |
| `Metrics.appBarHeight` | 72 | 54 dp |
| `Metrics.appBarMiniHeight` | 30 | 22.5 dp |
| `Metrics.statusBarHeight` | 32 | 24 dp |
| `Metrics.touchTargetMin` | 34 | 25.5 dp *(конфликт с Material 48 dp — см. ниже)* |
| `Metrics.gridUnit` (Win8) | 20 | 15 dp |
| `Metrics.gridSubUnit` (Win8) | 5 | 3.75 dp |
| `Metrics.silhouetteMargin` (Win8) | 120 | 90 dp |
| `Metrics.tileSmall` | 99 | 74.25 dp |
| `Metrics.tileMedium` | 210 | 157.5 dp |
| `Metrics.tileWide` | 432 × 210 | 324 × 157.5 dp |
| `Metrics.tileGap` | 12 | 9 dp |

### 8.3 Конфликт «аутентичность vs доступность»

Три места, где канон Metro нарушает современные нормы:

| Что | Metro | Норма 2026 | Предлагаемое решение |
|---|---|---|---|
| Минимальный тач-таргет | 34 px ≈ 25.5 dp | 48 dp (Material) / 44 pt (HIG) | Флаг `MetroTheme(strictMetrics = false)`: визуал остаётся 25.5 dp, хит-область расширяется до 48 dp невидимым padding'ом |
| Контраст subtle-текста | 60 % белого на чёрном ≈ 4.4:1, но 40 % чёрного на белом ≈ 2.8:1 | WCAG AA 4.5:1 | `MetroColors.subtleAccessible` как opt-in |
| Акценты lime / amber / yellow | контраст с белым текстом ~2.2:1 | AA 4.5:1 | правило luminance ≥ 0.5 переключает в чёрный только `yellow`; см. открытый вопрос №6 в `04-architecture.md` |

Сквозная политика по умолчанию: **аутентичный визуал + расширенные хит-области + opt-in для контраста.**

---

## 9. Незакрытые пробелы

Требуют реверса (`ILSpy` по `Microsoft.Phone.dll`, Blend «Edit a Copy» на машине с WP8 SDK) либо просмотра растровых схем MSDN:

1. Все числовые метрики хедера **Pivot** — высота, кегль, отступы, ширина «выглядывающего» соседнего заголовка.
2. **Panorama**: FontSize/Margin заголовка панорамы (не секции), величина peek следующей секции, точный коэффициент параллакса.
3. Позиция Title и «пузыря» счётчика на WP-плитке — есть только на растровых схемах MSDN.
4. Внутренние отступы плиток Windows 8 / 8.1.
5. Высота строки меню `ApplicationBar` и кегль подписей.
6. `line-height` и числовые веса в `ui-dark.css` WinJS.
7. Тайминги **Continuum** в WP8 Silverlight (в WinJS 2.0 они известны, см. §5.3).
8. Длительность перехода Semantic Zoom.

---

## Источники

**Первичные PDF Microsoft**
- [UI Design and Interaction Guide for Windows Phone 7, v2.0 (July 2010)](http://tableless.github.io/exemplos/pdf/guidelines-interface-mobiles/UI%20Design%20and%20Interaction%20Guide%20for%20Windows%20Phone%207%20v2.0.pdf) · [зеркало archive.org](https://archive.org/details/ui-design-and-interaction-guide-for-windows-phone-7-v-2.0)
- [Windows 8.1 User experience guidelines (© 2014)](https://tilsgee.github.io/DesignGuidelinesArchive/uwp8.1.pdf) · [Windows 8 UX guidelines (© 2012)](https://tilsgee.github.io/DesignGuidelinesArchive/uwp8.pdf) · [индекс архива](https://tilsgee.github.io/DesignGuidelinesArchive/)

**Тема и типографика**
- [Theme resources for Windows Phone (ff769552)](https://learn.microsoft.com/en-us/previous-versions/windows/apps/ff769552(v=vs.105)) · [Themes (ff402557)](https://learn.microsoft.com/en-us/previous-versions/windows/apps/ff402557(v=vs.105)) · [Text and fonts (cc189010)](https://learn.microsoft.com/en-us/previous-versions/windows/apps/cc189010(v=vs.105))
- [Сырой ThemeResources.xaml из WP SDK 7.1](https://raw.githubusercontent.com/MvvmCross/MvvmCross-Samples/master/OldSamples/CirriousConference/Cirrious.Conference.UI.WP7/ThemeResources.xaml) · [рантайм-дамп WP8 dark+light](https://win8rants.wordpress.com/2012/11/13/stuck-with-the-theme/)
- [StandardStyles.xaml (Win 8.0)](https://raw.githubusercontent.com/microsoft/cpprestsdk/master/Release/samples/BlackJack/BlackJack_UIClient/Common/StandardStyles.xaml) · [XAML theme resources reference (dn518235)](https://learn.microsoft.com/en-us/previous-versions/windows/apps/dn518235(v=win.10)) · [Win 8.1 themeresources.xaml](https://gist.github.com/longzheng/2308859/raw)
- [WinJS typography classes (hh770582)](https://learn.microsoft.com/nb-no/previous-versions/windows/hh770582(v=win.10))

**Layout**
- [Laying out an app page (hh872191)](https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh872191(v=win.10)) · [Narrow layouts (hh465371)](https://learn.microsoft.com/tr-tr/previous-versions/windows/hh465371(v=win.10)) · [Scaling plateaus (dn263244)](https://learn.microsoft.com/tr-tr/previous-versions/windows/apps/dn263244(v=win.10))
- [First look at Windows Phone (hh202905)](https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh202905(v=vs.105)) · [Walkthrough с TitlePanel (hh394038)](https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh394038(v=vs.105)) · [Multi-resolution apps (jj206974)](https://learn.microsoft.com/en-us/previous-versions/windows/apps/jj206974(v=vs.105))

**Плитки**
- [Tiles for Windows Phone 8 (hh202948)](https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh202948(v=vs.105)) · [Iconic Tile (jj662924)](https://learn.microsoft.com/en-us/previous-versions/windows/apps/jj662924(v=vs.105)) · [Flip Tile (jj662925)](https://learn.microsoft.com/en-us/previous-versions/windows/apps/jj662925(v=vs.105))
- [Tile and toast visual assets (hh781198)](https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh781198(v=win.10)) · [Notification queue (hh781199)](https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh781199(v=win.10)) · [Start tiles, 160 px pitch (jj552650)](https://learn.microsoft.com/en-us/previous-versions/windows/it-pro/windows-8.1-and-8/jj552650(v=win.10))

**Моушн (исходники)**
- [WindowsPhoneToolkit](https://github.com/microsoftarchive/WindowsPhoneToolkit) · [TiltEffect.cs](https://raw.githubusercontent.com/microsoftarchive/WindowsPhoneToolkit/master/Microsoft.Phone.Controls.Toolkit/Effects/TiltEffect.cs) · [TurnstileFeatherEffect.cs](https://raw.githubusercontent.com/microsoftarchive/WindowsPhoneToolkit/master/Microsoft.Phone.Controls.Toolkit/Effects/TurnstileFeatherEffect.cs)
- [WinJS Animations.js](https://raw.githubusercontent.com/winjs/winjs/master/src/js/WinJS/Animations.js) · [_TransitionAnimation.js](https://raw.githubusercontent.com/winjs/winjs/master/src/js/WinJS/Animations/_TransitionAnimation.js)
- [ProgressBar (gg442303)](https://learn.microsoft.com/en-us/previous-versions/windows/apps/gg442303(v=vs.105)) · [Jeff Wilcox — PerformanceProgressBar](https://jeffwilcox.blog/2010/08/performanceprogressbar/)

**Контролы**
- WP: [ff431813 App bar](https://learn.microsoft.com/en-us/previous-versions/windows/apps/ff431813(v=vs.105)) · [ff431806 иконки 48/26](https://learn.microsoft.com/en-us/previous-versions/windows/apps/ff431806(v=vs.105)) · [hh312716 MiniSize](https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh312716(v=vs.105)) · [hh202919 Pivot](https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh202919(v=vs.105)) · [hh202912 Panorama](https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh202912(v=vs.105)) · [ff941126 Panorama architecture](https://learn.microsoft.com/en-us/previous-versions/windows/apps/ff941126(v=vs.105)) · [jj244365 LongListSelector](https://learn.microsoft.com/en-us/previous-versions/windows/apps/jj244365(v=vs.105)) · [hh202889 touch targets](https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh202889(v=vs.105))
- Toolkit: [ToggleSwitch.cs](https://raw.githubusercontent.com/microsoftarchive/WindowsPhoneToolkit/master/Microsoft.Phone.Controls.Toolkit/ToggleSwitch/ToggleSwitch.cs) · [ListPicker.cs](https://raw.githubusercontent.com/microsoftarchive/WindowsPhoneToolkit/master/Microsoft.Phone.Controls.Toolkit/ListPicker/ListPicker.cs) · [ContextMenu.cs](https://raw.githubusercontent.com/microsoftarchive/WindowsPhoneToolkit/master/Microsoft.Phone.Controls.Toolkit/ContextMenu/ContextMenu.cs) · [DatePickerPage.xaml](https://raw.githubusercontent.com/microsoftarchive/WindowsPhoneToolkit/master/Microsoft.Phone.Controls.Toolkit/DateTimePickers/DatePickerPage.xaml) · [PhoneTextBox.cs](https://raw.githubusercontent.com/microsoftarchive/WindowsPhoneToolkit/master/Microsoft.Phone.Controls.Toolkit/PhoneTextBox/PhoneTextBox.cs)
- Win8: [hh781232 App bars](https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh781232(v=win.10)) · [dn481531 AppBarButton](https://learn.microsoft.com/en-us/previous-versions/windows/apps/dn481531(v=win.10)) · [dn481537 CommandBar](https://learn.microsoft.com/en-us/previous-versions/windows/apps/dn481537(v=win.10)) · [jj709928 SemanticZoom](https://learn.microsoft.com/en-us/previous-versions/windows/apps/jj709928(v=win.10)) · [hh770543 Settings pane](https://learn.microsoft.com/en-us/previous-versions/windows/apps/hh770543(v=win.10)) · [dn255137 Hub](https://learn.microsoft.com/en-us/previous-versions/windows/apps/dn255137(v=win.10)) · [hh738363 MessageDialog](https://learn.microsoft.com/en-us/previous-versions/windows/hh738363(v=win.10))

**Принципы**
- [WP8 «Be» principles (hh202906)](https://learn.microsoft.com/en-us/previous-versions/visualstudio/hh202906(v=vs.105)) · [MSDN Magazine 01/2012 — пять принципов Metro](https://learn.microsoft.com/en-us/archive/msdn-magazine/2012/january/windows-phone-design-your-windows-phone-apps-to-sell) · [MSDN Magazine 03/2011 Petzold — жесты](https://learn.microsoft.com/en-us/archive/msdn-magazine/2011/march/msdn-magazine-ui-frontiers-touch-gestures-on-windows-phone)

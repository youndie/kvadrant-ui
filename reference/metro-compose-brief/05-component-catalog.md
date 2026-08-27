# Каталог компонентов

Инвентарь того, что предстоит написать. Для каждого — источник истины по метрикам, оценка сложности, приоритет.

**Сложность:** `S` ≈ 0.5–1 день · `M` ≈ 2–4 дня · `L` ≈ 1–2 недели · `XL` ≈ месяц+
**Приоритет:** `P0` — без этого библиотеки нет · `P1` — нужен для первого релиза · `P2` — расширение · `P3` — экзотика

---

## 1. Фундамент

| # | Компонент | Что это | Источник метрик | Слож. | Приор. |
|---|---|---|---|---|---|
| F1 | `MetroTheme` | тема + CompositionLocals + объект-аксессор | `04-architecture.md` §2 | M | **P0** |
| F2 | `MetroColors` | 13 токенов × 2 темы + 20 акцентов + `contrastOn` | spec §4 | S | **P0** |
| F3 | `MetroTypography` | WP-рампа (12 стилей) + Win8-рампа (6 стилей) | spec §1 | M | **P0** |
| F4 | `MetroMetrics` | отступы, высоты, тач-таргеты, сетка | spec §2, §8.2 | S | **P0** |
| F5 | `MetroMotion` | кривые + длительности как токены | spec §5 | M | **P0** |
| F6 | Шрифтовой стек | Selawik + Inter fallback через compose-resources | `03-existing-solutions.md` §5 | M | **P0** |
| F7 | `TiltIndication` | замена ripple, `IndicationNodeFactory` | spec §5.2 | **L** | **P0** |
| F8 | `MetroText` / `ProvideTextStyle` | обёртка `BasicText` + `LocalTextStyle` | — | S | **P0** |
| F9 | `MetroSurface` | плоская поверхность без elevation и tonal | — | S | **P0** |

> **F7 — главный технический риск проекта.** `IndicationNodeFactory` стабилен, а нода может реализовать `LayoutModifierNode` (проверено: `Modifier.indication` оборачивает её в `DelegatingNode`, а `AbstractClickableNode` layout-делегатов не имеет — слот свободен). Но `translationZ` в Compose не даёт перспективного уменьшения, поэтому «утапливание» придётся эмулировать масштабом. Прототип нужен **до** оценки всего остального.

---

## 2. Навигация — то, ради чего всё затевается

| # | Компонент | Заметки | Источник | Слож. | Приор. |
|---|---|---|---|---|---|
| N1 | **`Pivot`** | горизонтальные страницы, циклические заголовки, ≤ 4 страницы, «выглядывающий» следующий заголовок | ❌ метрик нет — **реверс `Microsoft.Phone.dll`** либо семантика из WinJS `Pivot` | **L** | **P0** |
| N2 | **`Panorama`** | ≤ 5 секций, параллакс фона, wrap-around, заголовок 72 px SemiLight | частично ✅ (`PanoramaItem` template), параллакс ❌ | **L** | **P1** |
| N3 | `ApplicationBar` | 72 px, ≤ 4 иконки 48×48 с глифом 26×26, круг рисует бар, меню в lowercase, mini 30 px | ✅ полностью | M | **P0** |
| N4 | `PageHeader` (WP) | ApplicationTitle 20 px + PageTitle 72 px, margin `12,17,0,28` / `9,-7,0,0` | ✅ | S | **P0** |
| N5 | `PageHeader` (Win8) | силуэт: baseline 100 px, левое поле 120 px, back-кнопка 48×48 | ✅ | S | P2 |
| N6 | `Hub` (Win8) | горизонтальный панинг + кликабельные заголовки секций с chevron | ✅ семантика, ❌ метрики | L | P2 |
| N7 | `SemanticZoom` | pinch-переход между zoomed-in/out, ≤ 3 страницы в zoomed-out | ✅ семантика, ❌ длительность | **XL** | P3 |

> **N1 блокирует релиз.** Pivot — визитная карточка Metro, и это единственный компонент, метрик которого Microsoft не публиковал **вообще**. Без реверса `Microsoft.Phone.dll` (ILSpy) или Blend «Edit a Copy» на машине с WP8 SDK числа взять неоткуда. Заложить в план отдельной задачей на исследование — до старта разработки.

---

## 3. Плитки

| # | Компонент | Заметки | Слож. | Приор. |
|---|---|---|---|---|
| T1 | `Tile` | базовая плитка, 4 размера, `TileSize` enum | S | **P0** |
| T2 | `TileGrid` | укладка small/medium/wide/large в сетку с зазором 9 dp | M | **P0** |
| T3 | `FlipTile` | переворот лицевой/оборотной стороны | M | **P1** |
| T4 | `IconicTile` | иконка + счётчик + подпись, WP-специфика | S | P2 |
| T5 | `CycleTile` | циклическая смена до 9 изображений | S | P2 |
| T6 | `TileBadge` | «пузырь» счётчика | S | **P1** |
| T7 | Drag-reorder плиток | перетаскивание с реорганизацией сетки | **L** | P3 |

> Тайминги переворота Microsoft не публиковал и не давал контролировать. Значит `flipIntervalMillis` — **наш** параметр API с разумным дефолтом (предложение: 6000 мс с рандомизацией ±25 %, как ощущается на живом устройстве).

---

## 4. Базовые контролы

| # | Компонент | Источник метрик | Слож. | Приор. | Комментарий |
|---|---|---|---|---|---|
| C1 | `MetroButton` | ✅ border 3 px, chrome-фон | S | **P0** | плоский, прямоугольный, `BorderThickness = 3` |
| C2 | `MetroTextBox` | ✅ hint виден только при пустом + без фокуса | M | **P0** | скролла внутри нет — это канон, не баг |
| C3 | `MetroPasswordBox` | ✅ маскирование через 2 с или по следующему нажатию | S | **P1** | |
| C4 | `ToggleSwitch` | ✅ полностью: 136×95 / 89×34 / thumb 28×38 / ход 69 px / снап 50 мс `ExpOut15` | M | **P0** | самый детально задокументированный контрол |
| C5 | `MetroCheckBox` | ⚠️ частично | S | **P1** | квадрат, без скруглений |
| C6 | `MetroRadioButton` | ⚠️ частично | S | **P1** | |
| C7 | `MetroSlider` | ⚠️ частично | M | **P1** | |
| C8 | `MetroProgressBar` (determinate) | ✅ | S | **P0** | |
| C9 | `MetroProgressBar` (indeterminate) | ✅ 5 прямоугольников 4×4, цикл 4.4 с | M | **P0** | подпись эффекта Metro; сделать точно |
| C10 | `ListPicker` | ✅ threshold 5, раскрытие 200 мс `ExpInOut4` | M | **P1** | три режима: Normal / Expanded / Full |
| C11 | `MetroDatePicker` / `TimePicker` | ✅ LoopingSelector 148×148, число 54 px | **L** | P2 | в оригинале — полноценная навигация страницы |
| C12 | `ContextMenu` | ✅ scale 1.0→0.94 за 420 мс, **без блюра** | M | **P1** | открытие по long-press |
| C13 | `MessageBox` | ✅ только OK / OKCancel, позитив слева | S | **P0** | флаг для Win8-порядка кнопок |
| C14 | `MetroToast` | ✅ 10 с | S | P2 | |

---

## 5. Списки

| # | Компонент | Заметки | Слож. | Приор. |
|---|---|---|---|---|
| L1 | `MetroListItem` | 2 строки: Normal 20 px + Subtle 18.667 px | S | **P0** |
| L2 | `LongListSelector` | группировка + заголовки-квадраты 62×62 с бордером 2 px | M | **P1** |
| L3 | `JumpList` | сетка 113×113, буква 48 px SemiBold, порог ≥ 8 элементов | M | **P1** |
| L4 | `AcronymIcon` | цветной квадрат с инициалами (есть в Metro-Compose, MIT) | S | P2 |

---

## 6. Моушн и переходы

| # | Компонент | Заметки | Слож. | Приор. |
|---|---|---|---|---|
| M1 | `MetroEasing` | `metroPrimary`, `exit`, `exponentialOut(6)`, `exponentialIn(15)` как `Easing` | S | **P0** |
| M2 | `TurnstileTransition` | ±80° / +50°, ось = левый край, 350/250 мс | M | **P1** |
| M3 | `TurnstileFeather` | stagger 40/50 мс, общая ось через центр экрана — эффект жалюзи | **L** | **P1** |
| M4 | `SlideTransition` | 200 px, `ExpOut6`, 4 направления | S | P2 |
| M5 | `SwivelTransition` | `RotationX`, ForwardOut с уникальным `Exp = 15` | M | P2 |
| M6 | `ContinuumTransition` | 3-слойная, только WinJS-числа известны | **L** | P3 |
| M7 | `EntranceTransition` (Win8) | translateY 28 px, stagger 83 мс с кэпом 333 мс | M | P2 |

> **M3 (TurnstileFeather) — самый узнаваемый эффект Metro** после tilt. Стоит вложиться: именно он создаёт ощущение «вылетающего» списка. Ключ — все элементы вращаются вокруг **одной общей оси**, проходящей через вертикальный центр экрана, а не каждый вокруг своей.

---

## 7. Иконки

Segoe MDL2 Assets / Segoe Fluent Icons копировать нельзя (см. `03-existing-solutions.md` §5.1), готовой открытой замены нет.

| Что | Объём | Слож. | Приор. |
|---|---|---|---|
| Базовый набор ApplicationBar (back, add, delete, edit, save, cancel, check, search, refresh, share, settings, …) | ~40 глифов | **L** | **P1** |
| Расширенный набор | ~120 глифов | **XL** | P2 |
| Генератор `ImageVector` из SVG | тулинг | M | **P1** |

> Иконки — самая недооцениваемая часть подобных проектов. 40 глифов в едином стиле, отрисованных вручную под сетку 26×26 внутри круга 48×48 — это реальная неделя работы дизайнера, не программиста.

---

## 8. Слой Material-адаптера

Детальная таблица «что переживает решейп темой, а что нет» — в `04-architecture.md` §7. Сводка:

| Категория | Кол-во компонентов M3 | Что делаем |
|---|---|---|
| Решается одной темой | ~4 | `Text`, `Surface` (при `surfaceTint = Transparent`), `Divider`, `Icon` |
| Нужна тонкая обёртка | ~10 | `Button`, `Card`, `TextField`, `TopAppBar`, `Checkbox`, `RadioButton`, `Tab`, `AlertDialog`, `LinearProgressIndicator`, `Snackbar` |
| Нужна полная замена | ~11 | `Switch`, `Slider`, `FloatingActionButton`, `NavigationBar`, `Chip`, `CircularProgressIndicator`, `BottomSheet`, `DatePicker`, `TimePicker`, `SearchBar`, `NavigationRail` |

Причины замены сгруппированы так: вшитая elevation / tonal surface, принудительно скруглённые `Shape`, ripple внутри реализации (не через `LocalIndication`), захардкоженные анимации Material Motion.

**Компоненты адаптера:**

| # | Компонент | Приор. |
|---|---|---|
| A1 | `MetroMaterialAdapter { }` — маппинг Metro → `ColorScheme`/`Typography`/`Shapes` | **P0** |
| A2 | `MetroTheme.fromMaterial()` — обратный адаптер для встраивания в Material-приложение | **P1** |
| A3 | `AdaptiveWidget(metro = {}, material = {})` — примитив по образцу compose-cupertino | **P1** |
| A4 | Шим ripple 1.4 ↔ 1.5 (`LocalRippleConfiguration provides null`) | **P0** |

---

## 9. Сводка объёма

| Блок | P0 | P1 | P2+ | Итого позиций |
|---|---|---|---|---|
| Фундамент | 9 | — | — | 9 |
| Навигация | 2 | 2 | 3 | 7 |
| Плитки | 2 | 2 | 3 | 7 |
| Контролы | 6 | 6 | 2 | 14 |
| Списки | 1 | 2 | 1 | 4 |
| Моушн | 1 | 2 | 4 | 7 |
| Иконки | — | 2 | 1 | 3 |
| Адаптер | 2 | 2 | — | 4 |
| **Всего** | **23** | **18** | **14** | **55** |

Грубая оценка **P0-объёма: 8–10 недель** одного разработчика, при условии что риск `TiltIndication` (F7) и пробел по метрикам `Pivot` (N1) сняты заранее. Без этого оценка не имеет смысла.

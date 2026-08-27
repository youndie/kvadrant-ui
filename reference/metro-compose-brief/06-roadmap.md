# Дорожная карта и открытые решения

---

## Фаза 0 — Спайки (2 недели, до любых оценок)

Три технических риска, каждый из которых способен переопределить архитектуру. Пока они не сняты, планировать остальное бессмысленно.

### Спайк 1 — `TiltIndication` (5 дней)

**Вопрос:** воспроизводится ли tilt-эффект WP на `IndicationNodeFactory` без падения производительности и без хаков?

Что проверить:
- нода из `IndicationNodeFactory` реализует `LayoutModifierNode` и вешает слой через `placeWithLayer` — теоретически можно (`Modifier.indication` оборачивает её в `DelegatingNode`, а `AbstractClickableNode` layout-делегатов не имеет), практически не проверено;
- `Press.pressPosition` даёт координаты касания в нужной системе координат;
- `translationZ` не даёт перспективного уменьшения → эмуляция масштабом. Проверить, что при `cameraDistance = 8 × density` формула перспективы даёт ≈ 0.975 — ровно значение `pointerDown` из WinJS. Если совпадает — это красивое подтверждение того, что мы попали в оригинальную геометрию;
- поведение на всех таргетах: Android, JVM desktop, iOS, wasm.

**Критерий успеха:** плитка 210×210 наклоняется без кадропадов на 60 fps на среднем Android-устройстве, визуально совпадает с записью экрана Lumia.
**План Б:** если `LayoutModifierNode` внутри Indication не работает — выносим tilt в отдельный `Modifier.metroTilt()`, который пользователь вешает явно. Хуже по эргономике, но рабочий вариант.

### Спайк 2 — метрики `Pivot` (3 дня)

**Вопрос:** откуда взять числа, которых Microsoft не публиковал?

Порядок попыток:
1. ILSpy по `Microsoft.Phone.dll` из WP8 SDK — извлечь `PivotItem`/`Pivot` `ControlTemplate`;
2. Blend «Edit a Copy» на машине с установленным WP8 SDK;
3. покадровый анализ записи экрана живого устройства / эмулятора;
4. семантика (не метрики) — исходники WinJS `Pivot`.

**Критерий успеха:** есть числа для высоты хедера, кегля активного и неактивного заголовка, отступов, величины «выглядывания» соседнего заголовка, длительности анимации переключения.
**План Б:** зафиксировать наши значения как «интерпретацию», честно пометить в KDoc и дать параметрами API.

### Спайк 3 — шрифт (2 дня)

**Вопрос:** чем набирать кириллицу?

Selawik кириллицы **не содержит** (`Script Tags: dlng:'Latn' slng:'Latn'`, code pages 1252/1250/1254/1257 — 1251 отсутствует). Варианты:

| Вариант | Плюсы | Минусы |
|---|---|---|
| **A.** Selawik + Inter в fallback-стеке | быстро, обе OFL | разный ритм латиницы и кириллицы в одном интерфейсе |
| **B.** Только Inter | единый ритм, полный Unicode | не Segoe-метрики → вся числовая рампа поедет |
| **C.** Форк Selawik + дорисовка кириллицы | идеально | отдельный шрифтовой проект, месяцы |
| **D.** Подхватывать системный Segoe на Windows, Selawik/Inter на остальных | аутентичность там, где она возможна | три разных рендеринга одного UI |

**Рекомендация:** A на старте, D как улучшение для Compose Desktop, C — если проект вырастет.
**Критерий успеха:** решение принято и зафиксировано, скриншот-эталон русского и английского экрана рядом.

---

## Фаза 1 — Ядро (4 недели)

**Цель:** `metro-core` собирается на всех таргетах, есть тема, типографика, tilt и десяток базовых компонентов.

| Неделя | Что |
|---|---|
| 1 | Скелет проекта: 7 gradle-модулей, таргеты, CI, `abiValidation` (`@OptIn(ExperimentalAbiValidation::class)`), публикация снапшотов |
| 1–2 | F1–F5: `MetroTheme`, `MetroColors` (13 токенов × 2 + 20 акцентов), `MetroTypography` (две рампы), `MetroMetrics`, `MetroMotion` |
| 2 | F6: шрифтовой стек через compose-resources; F8–F9: `MetroText`, `MetroSurface` |
| 3 | F7: `TiltIndication` в продакшн-качестве (по результатам спайка 1) |
| 3–4 | C1, C2, C4, C8, C9, C13, L1: Button, TextBox, ToggleSwitch, ProgressBar (обе формы), MessageBox, ListItem |
| 4 | T1–T2: `Tile` + `TileGrid` |

**Выход:** `0.1.0-alpha01` в Maven Central. Sample-приложение — один экран со всеми компонентами.

---

## Фаза 2 — Навигация и адаптер (4 недели)

| Неделя | Что |
|---|---|
| 5 | N3, N4: `ApplicationBar`, `PageHeader` |
| 5–6 | N1: `Pivot` (по результатам спайка 2) |
| 6–7 | A1, A4: `MetroMaterialAdapter` + шим ripple; таблица маппинга из `04-architecture.md` §6 |
| 7 | M1, M2: `MetroEasing`, `TurnstileTransition` |
| 8 | Скриншот-тесты на Roborazzi (экспериментальная поддержка desktop/iOS), обязательный набор кейсов: dark/light × 4 акцента × 10 компонентов |

**Выход:** `0.1.0` — первый релиз, о котором можно писать публично.

---

## Фаза 3 — Полнота (6–8 недель)

- N2 `Panorama` с параллаксом · M3 `TurnstileFeather` · T3, T6 `FlipTile`, `TileBadge`
- L2, L3 `LongListSelector` + `JumpList` · C10, C12 `ListPicker`, `ContextMenu`
- C5–C7 Checkbox / RadioButton / Slider
- Иконки: базовые ~40 глифов ApplicationBar + генератор `ImageVector`
- A2, A3: обратный адаптер и `AdaptiveWidget`
- Документация: сайт на Dokka + галерея

**Выход:** `0.2.0`.

---

## Фаза 4 — Windows 8 ветка (опционально)

Win8-рампа, силуэт, `Hub`, `AppBar`/`CommandBar`, `SemanticZoom`, Settings flyout. Отдельный модуль `metro-win8`, потому что у WP и Win8 конфликтуют даже такие базовые вещи, как порядок кнопок в диалоге.

---

## Решения, которые нужно принять до старта

| # | Вопрос | Варианты | Рекомендация |
|---|---|---|---|
| 1 | **Шрифт для кириллицы** | Selawik+Inter / только Inter / форк Selawik / системный Segoe | Selawik + Inter, решать в спайке 3 |
| 2 | Множитель px → dp = **0.75** как единый канон | принять / считать пер-платформенно | принять: подтверждается тремя независимыми проверками |
| 3 | Абсолютный `#000000` в dark-теме | сохранить / смягчить до `#0A0A0A` | сохранить — это часть идентичности Metro |
| 4 | Тач-таргеты: 25.5 dp (канон) vs 48 dp (норма) | канон / норма / визуал канона + расширенная хит-область | третье, флагом `strictMetrics` |
| 5 | Что делать с lime / amber / yellow при провале WCAG AA | игнорировать / затемнять акцент / opt-in контрастный режим | opt-in `MetroColors.accessible` |
| 6 | Одна или две ветки Material-адаптера (M3 1.4 stable vs 1.5 alpha) | одна на 1.5 / две / отложить адаптер | две ветки: ядро без Material, адаптер отдельным артефактом |
| 7 | Стратегия интеропа: A (Metro поднимает Material) / B (отдельный `MetroMaterialAdapter`) / C (обратный) | — | **B** как основной + сахар `MetroMaterialTheme`, C как дополнение |
| 8 | Объём v0.1 | 23 позиции P0 / урезать | урезать до Pivot + 10 контролов, если спайк 2 провалится |
| 9 | Судьба `metro-icons` | рисовать / без иконок / шрифтовой фолбэк | базовые 40 глифов в фазе 3, до этого — слот `content` у ApplicationBar |
| 10 | Лицензия | Apache-2.0 (код) + OFL (шрифт) двойным пакетом | да, как у Metro-Compose/compose-fluent |
| 11 | Аутентичность vs a11y как сквозная политика | — | аутентичный визуал по умолчанию, расширенные хит-области всегда, контраст — opt-in |
| 12 | `flipIntervalMillis` для live-плиток | наш дефолт | 6000 мс ± 25 % рандомизации; Microsoft это не специфицировал |

---

## Что требует перепроверки перед стартом кодирования

Помечено `[проверить]` в `04-architecture.md`; часть уже закрыта верификацией (см. ниже), часть — нет.

| Вопрос | Статус |
|---|---|
| Кириллица в Selawik | **REFUTED** — кириллицы нет. Блокер, решается в спайке 3 |
| `Font(Res.font.x, weight, style)` из `org.jetbrains.compose.resources`, `@Composable` | **VERIFIED** для CMP 1.12 |
| Состав `ColorScheme` | **VERIFIED**: 48 ролей; `surfaceContainer*` и fixed-роли есть уже в 1.4.x, это не новинка 1.5-alpha |
| `RippleThemeConfiguration` / `LocalRippleConfiguration` | **VERIFIED**. Отключение ripple = `LocalRippleConfiguration provides null`. `rememberRipple` — `DeprecationLevel.ERROR` |
| `IndicationNodeFactory` + `LayoutModifierNode` | **VERIFIED** теоретически, слот свободен — но требует практической проверки в спайке 1 |
| Roborazzi для не-Android CMP | **VERIFIED**: есть, но помечено экспериментальным (iOS и Compose Desktop) |
| Paparazzi | только Android — подтверждено |
| Официальное решение для скриншот-тестов CMP | нет; Google Compose Preview Screenshot Testing прямо заявляет «don't support non-Android targets in KMP projects» |
| `@Preview` в commonMain | с CMP 1.10 канон — `androidx.compose.ui.tooling.preview.Preview`, jetbrains-вариант deprecated. **Какие IDE рендерят — UNKNOWN**, проверить руками |
| `abiValidation` в Kotlin 2.4 | работает, но под `@ExperimentalAbiValidation` — гарантий совместимости DSL нет |
| Системный акцент Windows из JVM | **VERIFIED**: реестр через JNA. `AccentColor` лежит под `DWM` (не под `Explorer\Accent`), формат **ABGR**; `ColorizationColor` — ARGB со «старшим байтом не-альфа». Лучший источник — `AccentPalette` (REG_BINARY, 8×4 байта, вся рампа Light3→Dark3). Готовой библиотеки нет: jSystemThemeDetector и skiko дают только dark/light |

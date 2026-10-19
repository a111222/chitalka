# Читалка

Оффлайн-читалка электронных книг для Android с премиальным дизайном: резной шкаф с подсветкой полок,
бумага с фактурой, три темы (светлый дуб, тёмный орех с латунью, E-Ink), хаптика и морфинг
телефон → планшет без растягивания.

- Архитектура: [документ «Архитектура читалки»](https://claude.ai/code/artifact/1801e249-1fe5-4b8a-b139-1caac411cb60)
- Дизайн: [макет v2](https://claude.ai/artifact/VfhpD1LdkmWJ67vtwX7nnL)

## Как собрать

Нужны Android Studio (или JDK 17 + Android SDK). Откройте папку проекта в Android Studio и нажмите Run,
либо из терминала:

```bash
./gradlew test               # все unit-тесты
./gradlew :app:assembleDebug # APK в app/build/outputs/apk/debug/
```

Каждый PR собирается в GitHub Actions; готовый APK можно скачать во вкладке Actions → артефакт
`chitalka-debug-apk`.

## Как устроен проект

Код разрезан на модули Gradle. Зависимости идут только сверху вниз: экран знает про domain,
domain не знает ни про Compose, ни про базу.

| Модуль | Что внутри |
|---|---|
| `app` | `MainActivity`, тема, навигация между экранами, адаптивная панель (снизу на телефоне, сбоку на планшете) |
| `feature:library` | Шкаф: экран, `ShelfViewModel`, сортировка |
| `feature:reader` | Чтение: страница или разворот, листание тапом по краю |
| `feature:settings` | Оформление: тема, шрифт, кегль, интервал, вибрация |
| `core:designsystem` | Токены макета v2: цвета трёх тем, анимации, шрифты, хаптика, шкаф на Canvas |
| `core:domain` | Интерфейсы `BookRepository`, `SettingsRepository` (чистый Kotlin) |
| `core:model` | Модели: `Book`, `ReadingPosition`, `ReaderSettings` (чистый Kotlin) |
| `core:data` | Реализации репозиториев поверх базы и DataStore |
| `core:database` | Room: 8 таблиц из документа архитектуры, FTS4-поиск |
| `core:datastore` | Настройки чтения в Preferences DataStore |
| `core:bookparser` | Единая модель книги и определение формата; парсеры EPUB/FB2 — следующий этап |
| `core:testing` | Фейковые репозитории и правило для тестов ViewModel |
| `build-logic` | Convention plugins: общая настройка модулей в одном месте |

Все версии библиотек — в [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

### Шаблон экрана

Каждый экран устроен одинаково, и этот шаблон легко повторять (в том числе с помощью LLM):

1. `XxxUiState` — неизменяемый data class со всем, что нужно нарисовать.
2. `XxxEvent` — sealed interface с действиями пользователя.
3. `XxxViewModel` отдаёт `StateFlow<XxxUiState>` и принимает `onEvent(event)`.
4. `XxxScreen(state, onEvent)` только рисует; цвета берёт из `ReaderTheme.colors`, никогда не пишет руками.

## Этапы

1. **Каркас** — модули, convention plugins, CI, экраны с навигацией. ← мы здесь
2. Дизайн-система — шрифты в APK, текстуры, резьба шкафа, скриншот-тесты (Roborazzi).
3. Библиотека — импорт EPUB и FB2, настоящие книги на полках, поиск.
4. Чтение — пагинация через TextMeasurer, прогресс, оглавление.
5. Премиум-эффекты — 3D-листание (AGSL), бумага, подсветка полок, переходы.
6. Планшет и доводка — разворот, закладки, Baseline Profile, бета в Google Play.

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
| `feature:library` | Шкаф: импорт книг с телефона, обложки, сортировка, удаление долгим нажатием |
| `feature:reader` | Чтение: пагинация через TextMeasurer, страница или разворот, листание тапом и свайпом, оглавление |
| `feature:settings` | Оформление: тема, шрифт, кегль, интервал, вибрация |
| `core:designsystem` | Токены макета v2: цвета трёх тем, анимации, шрифты, хаптика, шкаф на Canvas |
| `core:domain` | Интерфейсы `BookRepository`, `BookContentRepository`, `BookImporter`, `SettingsRepository` (чистый Kotlin) |
| `core:model` | Модели: `Book`, `Chapter` и блоки текста, `ReadingPosition`, `ReaderSettings` (чистый Kotlin) |
| `core:data` | Реализации репозиториев поверх базы и DataStore, импорт файлов (копия + SHA-256 против дублей) |
| `core:database` | Room: 8 таблиц из документа архитектуры, FTS4-поиск |
| `core:datastore` | Настройки чтения в Preferences DataStore |
| `core:bookparser` | Определение формата по содержимому, свои парсеры EPUB (spine, nav/NCX) и FB2 (в т. ч. .fb2.zip, windows-1251) |
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

1. **Каркас** — модули, convention plugins, CI, экраны с навигацией. ✓
2. **Импорт и чтение** — EPUB и FB2 с телефона, пагинация через TextMeasurer, прогресс, оглавление. ← мы здесь
3. Дизайн-система — шрифты в APK, текстуры, резьба шкафа, скриншот-тесты (Roborazzi).
4. Библиотека — поиск, полки, серии, сноски и картинки внутри текста.
5. Премиум-эффекты — 3D-листание (AGSL), бумага, подсветка полок, переходы.
6. Планшет и доводка — разворот, закладки, Baseline Profile, бета в Google Play.

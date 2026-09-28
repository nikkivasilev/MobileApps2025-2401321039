# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

MacroSnap is a single-user Android macro tracker: photograph a meal (or scan a barcode, describe it in words, reuse a past food) and Gemini estimates items, grams and nutrition; the user corrects it on a review screen and logs it. Also tracks vitamins/minerals, supplements, water, sleep, weight and gym workouts. Everything is stored on the phone; only photos/text sent for analysis leave it.

Stack: Kotlin 2.4, Jetpack Compose (Material 3), Room, DataStore, Retrofit + kotlinx.serialization, Coil, ML Kit code scanner. Single `:app` module, minSdk 26, JDK 17.

## Commands

`gradlew` is not executable in this checkout (committed from Windows), so invoke it through `sh`:

```bash
sh ./gradlew :app:compileDebugKotlin        # fastest type-check
sh ./gradlew :app:assembleDebug             # APK at app/build/outputs/apk/debug/app-debug.apk
sh ./gradlew :app:testDebugUnitTest         # all JVM unit tests
sh ./gradlew :app:testDebugUnitTest --tests "com.macrosnap.app.model.FoodHistoryTest"
sh ./gradlew :app:testDebugUnitTest --tests "*FoodHistoryTest.hidden*"   # one test (names use backticks with spaces)
sh ./gradlew :app:lintDebug
```

There is no CI and no instrumented (androidTest) suite. Unit tests live in `app/src/test` and must stay free of Android framework classes: test pure logic in `model/` and repositories through fakes of their interfaces (see `ProductRepositoryTest`, which fakes `OpenFoodFactsApi`, `ProductMicrosDao` and `FoodAnalyzer`).

`adb` and the emulator are not on PATH here: `~/Library/Android/sdk/platform-tools/adb`, `~/Library/Android/sdk/emulator/emulator -avd firepaw_shots`. `adb shell uiautomator dump` + `input tap` is how UI flows have been verified.

### Installing on the user's phone

`adb install -r` fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` when the APK is signed with a different debug keystore than the installed app. **Never uninstall to get past this without backing up first**: the manifest sets `allowBackup="false"`, so uninstalling destroys the user's log. The app is debuggable, so its data can be copied with `adb exec-out run-as com.macrosnap.app tar cf - databases files` and pushed back with `... | adb shell run-as com.macrosnap.app tar xf -` after reinstalling (create the tar with `COPYFILE_DISABLE=1` on macOS, or `._*` files end up in the app's storage).

## Architecture

### Wiring and screen pattern

- `AppContainer` is hand-rolled DI (one Room DB, one DataStore, Retrofit clients for Gemini and Open Food Facts sharing an OkHttp client). ViewModels reach it through `CreationExtras.appContainer` inside a `companion object { val Factory = viewModelFactory { ... } }`.
- Every screen follows the same shape: a `XxxUiState` data class (derived values as getters), a `@Stable interface XxxActions` the ViewModel implements, one-off `XxxEffect`s sent over a `Channel` and collected with `repeatOnLifecycle(STARTED)`, and a stateless `XxxContent(uiState, actions, …)` composable.
- Navigation uses type-safe `@Serializable` routes in `ui/Routes.kt`; `MacroSnapNavHost` shows the bottom bar only on the five top-level routes (Today, Gym, Recipes, Progress, Settings). Screens own their own top bar and insets.
- Writes from editor screens go through `trySave { … }` (`ui/Saving.kt`); on failure reset `isSaving` and emit a `ShowMessage(SAVE_FAILED_MESSAGE)` effect to the screen's snackbar.
- Anything tied to "today" must follow the clock: use `currentDates()` (`ui/CurrentDate.kt`), not a `LocalDate.now()` captured once in a ViewModel.

### Food logging flow

Every way of adding food (camera, gallery, barcode, describe, manual, recipe, past food) navigates to `ReviewRoute`; `ReviewViewModel` turns the source into editable `DraftItem`s and saves them with `FoodRepository.addAll`. Details worth knowing:

- Photos wait for an optional hint before the first analysis; re-analysis replaces only `fromPhoto` drafts. `ImageEncoder` shrinks the file in place to ≤1024 px and skips re-encoding an already-shrunk JPEG.
- Review state survives process death via `ReviewDraft.kt` (JSON in `SavedStateHandle`, saved only while `status == Ready`).
- An unsaved photo is deleted in `onCleared`; one photo can back several entries and is deleted with the last one; `MacroSnapApp` sweeps unreferenced photos older than two days on start.
- When a photo analysis fails with a `DataError.isTemporary` error (Server, RateLimited, Network), `ReviewViewModel` keeps the photo in `pending_photos` along with its day, meal and hint. `ReviewUiState.savedForLater` then stops `onCleared` from deleting the file.
  - Today lists these photos under "Waiting to analyze". Resubmitting opens `ReviewRoute(resumePending = true)`, which sends the photo straight away.
  - `FoodRepository.addAll` removes the pending row when the photo is logged, and the orphan sweep counts pending photos as referenced.
- "Your foods" (`model/PastFood.kt`) is derived from `food_entries`, not stored: grouped by `foodKey(name)`, nutrition from the newest entry, grams = most common of the last 10, ranked by recency-decayed frequency. Hidden foods are a key→timestamp map in DataStore and reappear once logged again. The same history feeds the describe-field suggestions and the Add food sheet.

### Nutrition model

- `FoodItem` stores nutrition **per 100 g** plus grams eaten; totals are computed. Keep this invariant when adding sources.
- `Micros = Map<Nutrient, Double>`; an absent key means "unknown", not zero. Persisted as JSON keyed by `Nutrient.name` via `MicrosJson`.
- Adding a `Nutrient`: the enum entry (label, unit, Gemini `schemaKey`, Open Food Facts `offKey`/`offScale`, RDA) is enough for storage, UI, targets and schemas, which all iterate `Nutrient.entries`, **but** the unit lists in the Gemini prompts (`MICROS_PROMPT`, `SUPPLEMENT_PROMPT`, `PRODUCT_MICROS_PROMPT` in `GeminiFoodAnalyzer`) are hand-written and must be updated too.
- Supplement doses (`supplement_logs.micros`) snapshot the supplement's nutrients when taken, so editing or deactivating a supplement doesn't rewrite past days.

### AI and external data

- `FoodAnalyzer` is the seam for the recognition backend; `GeminiFoodAnalyzer` is the only implementation. Requests use structured output (`responseSchema` built with `buildJsonObject`) and the model list from Settings, falling through to the next model only on `RateLimited`, `ModelUnavailable` or `Server`. Parsed values are clamped (kcal ≤ 900/100 g, macros ≤ 100 g/100 g).
- `DataError` + `Outcome` carry failures to the UI; `DataError.userMessage()` in `ui/Formatting.kt` is the user-facing text. `RateLimited(daily)` distinguishes per-minute from daily quota using the 429 body.
- Prompts accept English and Bulgarian input (units like гр/мл/с.л.); the UI itself is English-only with hardcoded strings.
- `ProductRepository` (Open Food Facts) treats listings as untrustworthy: it repairs kJ-typed-as-kcal, resolves kcal/kJ conflicts with the macros, flags impossible values and servings, and returns `ScannedProduct.warnings` for the review screen. Nutrients a listing lacks are estimated by Gemini and cached per barcode in `product_micros`; label values always win.
- Free-tier quota is scarce, so avoid adding Gemini calls on paths that run often; prefer caching or reusing history.

### Persistence

- Room database `macrosnap.db`, currently **version 8**, `exportSchema = false`, no destructive fallback. Every schema change needs a hand-written `Migration` appended to `AppDatabase.MIGRATIONS`. A column added with `DEFAULT x` in SQL needs a matching `@ColumnInfo(defaultValue = "x")` on the entity, or Room's schema validation fails at startup. Enums are stored as their `name` strings.
- DataStore (`SettingsStore`/`AppSettings`) holds the Gemini API key (plaintext), model list, goals, profile, nutrient-target overrides (only values differing from the RDA), wellness goals and hidden foods.
- Exercises are seeded once from `ExerciseLibrary` when the table is empty, so new defaults there don't reach existing installs.

### Design system

- The theme (`ui/theme/Theme.kt`) is a fixed "ink and data" palette with no dynamic colour. `primary` is near-black in light mode and near-white in dark mode. Hue is kept for tracked quantities, read through `AppTheme.colors` (`protein`, `carbs`, `fat`, `water`, `sleep`, `micros`, `calories`, plus `card`, `tile` and `track` surfaces). `MacroColors.*` are theme-aware getters over the same values.
- Build screens from the shared pieces, not raw Material cards:
  - `AppCard`, `CardHeader`, `IconBadge`, `SectionTitle`, `MacroDots`, `Tag` and `appChipColors()` in `ui/components/Surfaces.kt`
  - `ProgressRing`, `PillBar`, `CalorieHero` and `MacroTile` in `MacroSummary.kt`
- `AppCard` is flat (no elevation), with 24dp corners. Sheets that hold cards use `containerColor = MaterialTheme.colorScheme.background`.
- Typography lives in `ui/theme/Type.kt`. It uses Inter, bundled as four static weights in `res/font` (Regular, Medium, SemiBold, Bold; OFL licence in `third_party/inter/`). Number-heavy styles use tabular figures (`tnum`). A new weight needs its own TTF, or Android synthesizes it.

### UI gotchas

- Only `material-icons-core` is included (the extended set is too heavy for the unminified build). Missing icons are hand-built `ImageVector`s in `ui/theme/AppIcons.kt`. Note `Icons.Outlined.Star` renders solid; use `AppIcons.StarBorder` for an empty star.
- Charts in Progress are hand-drawn on `Canvas`; there is no chart library.

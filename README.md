# MacroSnap

A personal Android macro tracker. Photograph a meal and an AI model estimates the items, portion
weights and macros; you correct anything that's off and log it. Everything you log stays on the phone.

## Features

- **Photo logging** - camera or gallery. Gemini (free tier) returns each food with grams and
  per-100 g nutrition; you adjust grams with +/- or edit any value before saving. If Gemini is
  overloaded, out of quota or unreachable, the photo is kept under **Waiting to analyze** on Today
  with its meal and hint, so you can resubmit it later (or log it by hand, or discard it).
- **Your foods** - everything you've logged before, most frequent first, at the top of the Add food
  sheet with a search box. Tap one to log it again at your usual amount - no AI request. While you
  describe a meal, matching past foods are suggested too, and any logged entry has a **Log again** button.
- **Barcode scanning** - label values for packaged food from Open Food Facts (no key needed). Listings
  are crowd-sourced, so the app checks them: kJ typed as kcal is converted, calories that don't fit
  the macros or a serving that can't be right are flagged for you to check against the label.
  Vitamins and minerals a listing leaves out are estimated by Gemini once per barcode and remembered.
- **Describe in words** - type "50 g olive oil, 2 eggs, a cup of cooked rice" and Gemini turns it into
  items with weights and macros. English and Bulgarian both work. Available on the review screen and
  in the recipe editor.
- **Recipes** - build a dish from ingredients (described or typed), enter the cooked weight and number
  of servings, then log a serving from the Today add-sheet or the Recipes tab.
- **Manual entry** - per 100 g or "whole portion" values; calories auto-fill from macros.
- **Vitamins & minerals** - vitamin D, B12, B6, B3 (niacin), B5, C, A, iron, magnesium, zinc, calcium,
  potassium, omega-3, creatine and caffeine (a limit). Every AI analysis estimates them per 100 g;
  barcode items get them from the label or a Gemini estimate. Today shows them against RDA-based
  targets (editable in Settings).
- **Supplements** - list what you take (dose, times per day, nutrient content per dose - or describe
  it and let Gemini fill it in). Today shows a checklist; ticked doses count toward your nutrients,
  and past days keep what each dose contained even if you later edit the supplement.
- **Daily view** - calorie ring, protein/carbs/fat bars against your goals, entries grouped by meal.
- **Water & sleep** - quick-add water against a daily goal; log bedtime, wake time and a 1-5 star rating.
- **Gym** - start a workout (blank or from a saved routine), log sets as weight × reps (or time/distance
  for cardio), rest timer between sets, personal-record detection on finish, and per-exercise progress
  charts (estimated 1RM per session). Routines pre-fill sets with what you lifted last time.
- **Exercises** - Gym tab > Exercises: add your own exercises and edit any exercise's muscle group,
  type and default sets × reps, which routines and new workouts start from.
- **Goal calculator** - Mifflin-St Jeor from sex, age, height, weight, activity and goal.
- **Progress** - strength chart per exercise, 7-day calorie/sleep/water charts, body-weight trend
  (weigh-ins can be logged for earlier days).
- **English or Bulgarian** - Settings > Language (or the phone's per-app language setting on Android 13+).
  Photos analyzed without a hint get item names in the app's language.

## Install on your phone

1. On the phone: Settings > About phone > Software information > tap **Build number** 7 times,
   then Settings > Developer options > enable **USB debugging**.
2. Plug the phone into the computer and accept the "Allow USB debugging" prompt.
3. Either open this folder in Android Studio and press **Run**, or from a terminal here:

   ```
   gradlew.bat installDebug        # Windows
   sh ./gradlew installDebug       # macOS / Linux
   ```

   The APK is also left at `app/build/outputs/apk/debug/app-debug.apk` - you can copy it to the
   phone and open it there instead (allow "install unknown apps" when asked).

**Updating from a different computer:** each computer signs debug builds with its own key, and Android
refuses to update an app whose key changed. Don't uninstall to get around it - the app is excluded from
backup, so uninstalling deletes your log. Either copy `~/.android/debug.keystore` (on Windows
`C:\Users\<you>\.android\debug.keystore`) from the computer that installed it, or copy the app's data
off the phone first (`adb exec-out run-as com.macrosnap.app tar cf - databases files > backup.tar`),
reinstall, and put it back.

## One-time setup in the app

1. Settings tab > **Get a free key** > sign in to Google AI Studio > create an API key.
2. Paste it into **Gemini API key** > **Save and test**. You should see "Key works".
3. Set your goals, or fill in the goal calculator and tap **Use these goals**.

The key is stored only in the app's private storage (and the app is excluded from cloud backup).

## About the free tier

- Limits are per Google project and change over time; your live numbers are at
  https://aistudio.google.com/rate-limit. At the time of writing the full Flash model allows only
  a couple of dozen requests a day while Flash-Lite allows hundreds, so the app tries the models
  in the order listed in Settings and falls through when one runs out of quota. The error message
  tells you whether you hit the daily quota or only a per-minute limit, and the photo waits on
  Today until you resubmit it.
- Reusing past foods costs no requests, and barcode vitamin estimates are cached per product, so
  the quota mostly goes to new photos and descriptions.
- On the free tier Google may use submitted content to improve its products. Meal photos, text you
  ask Gemini to analyze, and the names of scanned products it fills in are sent to Google; barcodes are
  looked up on Open Food Facts. Nothing else leaves the phone.
- Photo estimates are estimates. Identification is usually good; portion weight is the weak spot
  (expect +/-20-30%), which is why every scan lands on an edit screen first. For packaged food,
  scan the barcode instead.

## Development

```
sh ./gradlew :app:assembleDebug        # build
sh ./gradlew :app:testDebugUnitTest    # JVM unit tests (app/src/test)
```

See `CLAUDE.md` for architecture notes and conventions.

## Project layout

```
app/src/main/java/com/macrosnap/app/
  model/      plain domain types (FoodItem stores nutrition per 100 g + grams eaten),
              past-food history and matching
  domain/     GoalCalculator, CalorieBurnEstimator
  data/       Room database (v8, migrations in AppDatabase), DataStore settings,
              Gemini + Open Food Facts clients, repositories
              FoodAnalyzer is the seam for swapping the recognition backend
  ui/         Compose screens: today, workouts, review, recipes, supplements, progress, settings (+ shared components)
  AppContainer.kt   manual dependency wiring
```

Stack: Kotlin 2.4, Jetpack Compose (Material 3), Room, DataStore, Retrofit + kotlinx.serialization,
Coil, Google code scanner. Min SDK 26.

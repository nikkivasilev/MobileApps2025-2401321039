# MacroSnap

A personal Android macro tracker. Photograph a meal and an AI model estimates the items, portion
weights and macros; you correct anything that's off and log it. Everything you log stays on the phone.

## Features

- **Photo logging** - camera or gallery. Gemini (free tier) returns each food with grams and
  per-100 g nutrition; you adjust grams with +/- or edit any value before saving.
- **Barcode scanning** - exact label values for packaged food from Open Food Facts (no key needed).
- **Describe in words** - type "50 g olive oil, 2 eggs, a cup of cooked rice" and Gemini turns it into
  items with weights and macros. Available on the review screen and in the recipe editor.
- **Recipes** - build a dish from ingredients (described or typed), enter the cooked weight and number
  of servings, then log a serving from the Today add-sheet or the Recipes tab.
- **Manual entry** - per 100 g or "whole portion" values; calories auto-fill from macros.
- **Vitamins & minerals** - vitamin D, B12, B6, C, A, iron, magnesium, zinc, calcium, potassium, omega-3,
  creatine and caffeine (a limit). Every AI analysis estimates them per 100 g; barcode items get
  them from Open Food Facts. Today shows them against RDA-based targets (editable in Settings).
- **Supplements** - list what you take (dose, times per day, nutrient content per dose - or describe
  it and let Gemini fill it in). Today shows a checklist; ticked doses count toward your nutrients.
- **Daily view** - calorie ring, protein/carbs/fat bars against your goals, entries grouped by meal.
- **Water & sleep** - quick-add water against a daily goal; log bedtime, wake time and a quality rating.
- **Gym** - start a workout (blank or from a saved routine), log sets as weight × reps (or time/distance
  for cardio), rest timer between sets, personal-record detection on finish, and per-exercise progress
  charts (estimated 1RM per session). Routines pre-fill sets with what you lifted last time.
- **Goal calculator** - Mifflin-St Jeor from sex, age, height, weight, activity and goal.
- **Progress** - strength chart per exercise, 7-day calorie/sleep/water charts, body-weight trend.

## Install on your phone

1. On the phone: Settings > About phone > Software information > tap **Build number** 7 times,
   then Settings > Developer options > enable **USB debugging**.
2. Plug the phone into the PC and accept the "Allow USB debugging" prompt.
3. Either open this folder in Android Studio and press **Run**, or from a terminal here:

   ```
   gradlew.bat installDebug
   ```

   Building also leaves a `MacroSnap-debug.apk` in this folder (not committed to git) - you can
   copy it to the phone and open it there instead (allow "install unknown apps" when asked).

## One-time setup in the app

1. Settings tab > **Get a free key** > sign in to Google AI Studio > create an API key.
2. Paste it into **Gemini API key** > **Save and test**. You should see "Key works".
3. Set your goals, or fill in the goal calculator and tap **Use these goals**.

The key is stored only in the app's private storage (and the app is excluded from cloud backup).

## About the free tier

- Limits are per Google project and change over time; your live numbers are at
  https://aistudio.google.com/rate-limit. At the time of writing the full Flash model allows only
  a couple of dozen requests a day while Flash-Lite allows hundreds, so the app tries the models
  in the order listed in Settings and falls through when one runs out of quota.
- On the free tier Google may use submitted content to improve its products. Your meal photos are
  sent to Google for analysis; nothing else leaves the phone.
- Photo estimates are estimates. Identification is usually good; portion weight is the weak spot
  (expect +/-20-30%), which is why every scan lands on an edit screen first. For packaged food,
  scan the barcode instead.

## Project layout

```
app/src/main/java/com/macrosnap/app/
  model/      plain domain types (FoodItem stores nutrition per 100 g + grams eaten)
  domain/     GoalCalculator
  data/       Room database (v4, migrations in AppDatabase), DataStore settings,
              Gemini + Open Food Facts clients, repositories
              FoodAnalyzer (photo + text) is the seam for swapping the recognition backend
  ui/         Compose screens: today, workouts, review, recipes, supplements, progress, settings (+ shared components)
  AppContainer.kt   manual dependency wiring
```

Stack: Kotlin 2.4, Jetpack Compose (Material 3), Room, DataStore, Retrofit + kotlinx.serialization,
Coil, Google code scanner. Min SDK 26.

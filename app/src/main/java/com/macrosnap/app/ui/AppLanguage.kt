package com.macrosnap.app.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.macrosnap.app.R
import java.util.Locale

/** The languages the UI is translated into. [SYSTEM] follows the phone's language. */
enum class AppLanguage(val tag: String, @StringRes val labelRes: Int) {
    SYSTEM("", R.string.language_system),
    ENGLISH("en", R.string.language_english),
    BULGARIAN("bg", R.string.language_bulgarian);

    companion object {
        /** The in-app choice, stored by AppCompat (not DataStore) so it applies before the first frame. */
        fun current(): AppLanguage {
            val tag = AppCompatDelegate.getApplicationLocales()[0]?.language ?: return SYSTEM
            return entries.firstOrNull { it.tag == tag } ?: SYSTEM
        }

        /** Recreates the activity in the new language. */
        fun set(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(
                if (language == SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(language.tag),
            )
        }

        /**
         * The locale the UI is showing, for code outside composition (Gemini prompts). Resolves
         * [SYSTEM] to the phone's language, falling back to English when it isn't one we translate.
         */
        fun effectiveLocale(): Locale {
            val chosen = AppCompatDelegate.getApplicationLocales()[0]
                ?: Resources.getSystem().configuration.locales[0]
            return if (chosen?.language == BULGARIAN.tag) Locale.forLanguageTag(BULGARIAN.tag) else Locale.ENGLISH
        }
    }
}

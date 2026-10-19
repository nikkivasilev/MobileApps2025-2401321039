package com.macrosnap.app.ui

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * Text produced outside composition (ViewModels, repositories) that is only turned into a string
 * on screen, so it follows the current language. Arguments may themselves be [UiText].
 */
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = listOf(count)) : UiText
    /** Text that needs no translation, such as a server's own error detail or a food name. */
    data class Raw(val text: String) : UiText
}

fun uiText(@StringRes id: Int, vararg args: Any): UiText = UiText.Res(id, args.toList())

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Res -> stringResource(id, *args.map { it.resolved() }.toTypedArray())
    is UiText.Plural -> pluralStringResource(id, count, *args.map { it.resolved() }.toTypedArray())
    is UiText.Raw -> text
}

/** Outside composition, e.g. in a snackbar collector; pass an activity context so the in-app language applies. */
fun UiText.asString(context: Context): String = when (this) {
    is UiText.Res -> context.getString(id, *args.map { it.resolved(context) }.toTypedArray())
    is UiText.Plural -> context.resources.getQuantityString(id, count, *args.map { it.resolved(context) }.toTypedArray())
    is UiText.Raw -> text
}

@Composable
private fun Any.resolved(): Any = if (this is UiText) asString() else this

private fun Any.resolved(context: Context): Any = if (this is UiText) asString(context) else this

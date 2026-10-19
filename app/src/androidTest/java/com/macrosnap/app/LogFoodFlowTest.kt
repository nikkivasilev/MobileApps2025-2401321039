package com.macrosnap.app

import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The main scenario end to end: log a food by hand, see it on Today, change its amount and delete
 * it. Runs against an in-memory database (see [MacroSnapTestRunner]) and needs no Gemini key.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class LogFoodFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun str(@StringRes id: Int, vararg args: Any): String = composeRule.activity.getString(id, *args)

    private fun textField(@StringRes label: Int): SemanticsNodeInteraction =
        composeRule.onNode(hasSetTextAction() and hasText(str(label)))

    private fun saveButton(): SemanticsNodeInteraction =
        composeRule.onNode(hasText(str(R.string.action_save)) and hasClickAction())

    /** The entry's row on Today, which shows its name, amount and calories. */
    private fun entryRow(grams: String, kcal: String): SemanticsNodeInteraction = composeRule.onNode(
        hasText(FOOD_NAME) and hasText(str(R.string.amount_g, grams)) and hasText(kcal),
    )

    private fun waitForText(text: String) =
        composeRule.waitUntilAtLeastOneExists(hasText(text), timeoutMillis = TIMEOUT_MILLIS)

    @Test
    fun logEditAndDeleteAFoodByHand() {
        // Create: Today → Add food → Manual → fill in the item → Log it. Values are for the whole
        // portion, the editor's default.
        composeRule.onNodeWithContentDescription(str(R.string.today_add_food)).performClick()
        composeRule.onNodeWithText(str(R.string.today_method_manual)).performClick()

        textField(R.string.review_food_name).performTextInput(FOOD_NAME)
        textField(R.string.review_amount_eaten).performTextInput("150")
        textField(R.string.calories).performTextInput("200")
        textField(R.string.review_field_protein).performTextInput("10")
        Espresso.closeSoftKeyboard()
        saveButton().performScrollTo().performClick()
        composeRule.onNodeWithText(str(R.string.review_log_it)).performClick()

        // Read: the entry is listed on Today with its amount.
        waitForText(FOOD_NAME)
        entryRow(grams = "150", kcal = "200").assertExists()

        // Update: open it, edit the amount, save.
        composeRule.onNodeWithText(FOOD_NAME).performClick()
        composeRule.onNodeWithText(str(R.string.action_edit)).performClick()
        textField(R.string.review_amount_eaten).performTextClearance()
        textField(R.string.review_amount_eaten).performTextInput("300")
        Espresso.closeSoftKeyboard()
        saveButton().performScrollTo().performClick()
        waitForText(str(R.string.amount_g, "300"))
        // Twice the portion, twice the calories.
        entryRow(grams = "300", kcal = "400").assertExists()

        // Delete: open it again, delete and confirm.
        composeRule.onNodeWithText(FOOD_NAME).performClick()
        composeRule.onNode(hasText(str(R.string.action_delete)) and hasClickAction()).performClick()
        val confirmDialog = isDialog() and hasAnyDescendant(hasText(str(R.string.review_delete_entry_title)))
        composeRule.onNode(hasText(str(R.string.action_delete)) and hasAnyAncestor(confirmDialog)).performClick()
        composeRule.waitUntilDoesNotExist(hasText(FOOD_NAME), timeoutMillis = TIMEOUT_MILLIS)
    }

    private companion object {
        const val FOOD_NAME = "Test oatmeal"
        const val TIMEOUT_MILLIS = 5_000L
    }
}

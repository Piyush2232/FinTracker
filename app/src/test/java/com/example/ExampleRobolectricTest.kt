package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app name matches FinTracker`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FinTracker", appName)
    }

    @Test
    fun `first launch goal onboarding, scroll to recent spending, and drill down to categories`() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("confirm_monthly_goal_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("confirm_monthly_goal_button")
            .performScrollTo()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("confirm_monthly_goal_button").fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("streak_button").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_button").assertIsDisplayed()
        composeRule.onNodeWithTag("concentric_spending_circle").assertIsDisplayed()

        // Scroll down on main screen to reveal "Recent Spending"
        composeRule.onNodeWithTag("home_scroll_container").performScrollToIndex(1)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("recent_spending_section").assertIsDisplayed()
        composeRule.onNodeWithTag("simulate_sms_home_button").assertIsDisplayed()

        // Scroll back to top and tap the Concentric Circle to transition to Level 2 Categories
        composeRule.onNodeWithTag("home_scroll_container").performScrollToIndex(0)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("concentric_spending_circle").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("breakdown_back_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("breakdown_back_button").assertIsDisplayed()

        // Drill down to Level 3 Food & Dining full-screen curated view
        composeRule.onNodeWithTag("category_card_food").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("category_detail_back").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("category_detail_back").assertIsDisplayed()
    }
}

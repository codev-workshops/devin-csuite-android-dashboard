package com.devin.csuite.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.devin.csuite.presentation.theme.AccentPrimary
import com.devin.csuite.presentation.theme.AccentSecondary
import com.devin.csuite.presentation.theme.AccentTertiary
import com.devin.csuite.presentation.theme.DarkBackground
import com.devin.csuite.presentation.theme.DarkOnBackground
import com.devin.csuite.presentation.theme.DarkOnSurface
import com.devin.csuite.presentation.theme.DarkOnSurfaceVariant
import com.devin.csuite.presentation.theme.DarkSurface
import com.devin.csuite.presentation.theme.DarkSurfaceVariant
import com.devin.csuite.presentation.theme.ErrorRed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    instrumentedPackages = ["androidx.loader.content"],
    sdk = [33]
)
class AcuGaugeChartTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Composable
    private fun TestTheme(content: @Composable () -> Unit) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = AccentPrimary,
                secondary = AccentSecondary,
                tertiary = AccentTertiary,
                background = DarkBackground,
                surface = DarkSurface,
                surfaceVariant = DarkSurfaceVariant,
                onBackground = DarkOnBackground,
                onSurface = DarkOnSurface,
                onSurfaceVariant = DarkOnSurfaceVariant,
                error = ErrorRed,
                onPrimary = DarkOnBackground,
                onSecondary = DarkOnBackground,
                onTertiary = DarkOnBackground,
                onError = DarkOnBackground
            ),
            content = content
        )
    }

    // --- Gauge at 0% ---

    @Test
    fun `gauge at 0 percent displays 0 percent text`() {
        composeTestRule.setContent {
            TestTheme {
                AcuGaugeChart(used = 0.0, limit = 1000.0)
            }
        }

        composeTestRule.onNodeWithText("0%").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 / 1.0K").assertIsDisplayed()
    }

    // --- Gauge at 50% ---

    @Test
    fun `gauge at 50 percent displays correct text`() {
        composeTestRule.setContent {
            TestTheme {
                AcuGaugeChart(used = 500.0, limit = 1000.0)
            }
        }

        composeTestRule.onNodeWithText("50%").assertIsDisplayed()
        composeTestRule.onNodeWithText("500 / 1.0K").assertIsDisplayed()
    }

    // --- Gauge at 80% ---

    @Test
    fun `gauge at 80 percent displays correct text`() {
        composeTestRule.setContent {
            TestTheme {
                AcuGaugeChart(used = 800.0, limit = 1000.0)
            }
        }

        composeTestRule.onNodeWithText("80%").assertIsDisplayed()
        composeTestRule.onNodeWithText("800 / 1.0K").assertIsDisplayed()
    }

    // --- Gauge at 100% ---

    @Test
    fun `gauge at 100 percent displays correct text`() {
        composeTestRule.setContent {
            TestTheme {
                AcuGaugeChart(used = 1000.0, limit = 1000.0)
            }
        }

        composeTestRule.onNodeWithText("100%").assertIsDisplayed()
        composeTestRule.onNodeWithText("1.0K / 1.0K").assertIsDisplayed()
    }

    // --- Edge cases ---

    @Test
    fun `gauge over 100 percent clamps to 100 percent`() {
        composeTestRule.setContent {
            TestTheme {
                AcuGaugeChart(used = 1500.0, limit = 1000.0)
            }
        }

        composeTestRule.onNodeWithText("100%").assertIsDisplayed()
    }

    @Test
    fun `gauge with zero limit shows 0 percent`() {
        composeTestRule.setContent {
            TestTheme {
                AcuGaugeChart(used = 500.0, limit = 0.0)
            }
        }

        composeTestRule.onNodeWithText("0%").assertIsDisplayed()
    }

    @Test
    fun `gauge with large values formats correctly`() {
        composeTestRule.setContent {
            TestTheme {
                AcuGaugeChart(used = 5000000.0, limit = 10000000.0)
            }
        }

        composeTestRule.onNodeWithText("50%").assertIsDisplayed()
        composeTestRule.onNodeWithText("5.0M / 10.0M").assertIsDisplayed()
    }

    // --- Animation Tests ---

    @Test
    fun `gauge initial composition renders without crash at 0 percent`() {
        composeTestRule.setContent {
            TestTheme {
                AcuGaugeChart(used = 0.0, limit = 1000.0)
            }
        }

        composeTestRule.onNodeWithText("0%").assertIsDisplayed()
    }

    @Test
    fun `gauge initial composition renders without crash at 85 percent`() {
        composeTestRule.setContent {
            TestTheme {
                AcuGaugeChart(used = 850.0, limit = 1000.0)
            }
        }

        composeTestRule.onNodeWithText("85%").assertIsDisplayed()
        composeTestRule.onNodeWithText("850 / 1.0K").assertIsDisplayed()
    }

    @Test
    fun `gauge animates from 0 to target - settled state shows final value`() {
        composeTestRule.setContent {
            TestTheme {
                AcuGaugeChart(used = 750.0, limit = 1000.0)
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1500)

        composeTestRule.onNodeWithText("75%").assertIsDisplayed()
        composeTestRule.onNodeWithText("750 / 1.0K").assertIsDisplayed()
    }

    @Test
    fun `gauge at initial composition vs settled state`() {
        composeTestRule.setContent {
            TestTheme {
                AcuGaugeChart(used = 500.0, limit = 1000.0)
            }
        }

        composeTestRule.onNodeWithText("50%").assertIsDisplayed()

        composeTestRule.mainClock.advanceTimeBy(1500)

        composeTestRule.onNodeWithText("50%").assertIsDisplayed()
    }

    // --- Threshold boundary tests ---

    @Test
    fun `gauge renders at green threshold boundary - 79 percent`() {
        composeTestRule.setContent {
            TestTheme {
                AcuGaugeChart(used = 790.0, limit = 1000.0)
            }
        }

        composeTestRule.onNodeWithText("79%").assertIsDisplayed()
    }

    @Test
    fun `gauge renders at amber threshold boundary - 81 percent`() {
        composeTestRule.setContent {
            TestTheme {
                AcuGaugeChart(used = 810.0, limit = 1000.0)
            }
        }

        composeTestRule.onNodeWithText("81%").assertIsDisplayed()
    }

    @Test
    fun `gauge renders at red threshold boundary - 96 percent`() {
        composeTestRule.setContent {
            TestTheme {
                AcuGaugeChart(used = 960.0, limit = 1000.0)
            }
        }

        composeTestRule.onNodeWithText("96%").assertIsDisplayed()
    }
}

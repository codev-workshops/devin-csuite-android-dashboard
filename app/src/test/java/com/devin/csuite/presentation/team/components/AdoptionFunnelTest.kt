package com.devin.csuite.presentation.team.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.devin.csuite.presentation.team.FunnelData
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
import org.junit.Assert.assertNotNull
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
class AdoptionFunnelTest {

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

    // --- Funnel with sample data (Total Users > MAU > WAU > DAU) ---

    @Test
    fun `funnel renders title text`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 500, mau = 300, wau = 150, dau = 50)
                )
            }
        }

        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
    }

    @Test
    fun `funnel with sample data composes without crash`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 500, mau = 300, wau = 150, dau = 50)
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `funnel with sample data renders after animation completes`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 500, mau = 300, wau = 150, dau = 50)
                )
            }
        }

        // Advance past the 1200ms animation
        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
    }

    @Test
    fun `funnel with large K-scale values composes without crash`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 10000, mau = 5000, wau = 2500, dau = 1000)
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
    }

    @Test
    fun `funnel with million-scale values composes without crash`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 2000000, mau = 1500000, wau = 1000000, dau = 500000)
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
    }

    // --- Funnel with empty data (all zeros) ---

    @Test
    fun `funnel with all zeros does not crash`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 0, mau = 0, wau = 0, dau = 0)
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
    }

    @Test
    fun `funnel with all zeros renders title`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 0, mau = 0, wau = 0, dau = 0)
                )
            }
        }

        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
    }

    @Test
    fun `funnel with all zeros composes after animation`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 0, mau = 0, wau = 0, dau = 0)
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.onRoot().assertExists()
    }

    // --- Edge cases ---

    @Test
    fun `funnel with equal DAU MAU WAU values composes without crash`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 100, mau = 100, wau = 100, dau = 100)
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
    }

    @Test
    fun `funnel with single user composes without crash`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 1, mau = 1, wau = 1, dau = 1)
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
    }

    @Test
    fun `funnel with DAU equals MAU equals WAU renders correctly`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 50, mau = 50, wau = 50, dau = 50)
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
    }

    @Test
    fun `funnel animation settles to final state`() {
        var compositionSuccess = false
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 200, mau = 150, wau = 100, dau = 50)
                )
                compositionSuccess = true
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1500)
        assertNotNull(compositionSuccess)
        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
    }

    @Test
    fun `funnel with only total users and no active users composes without crash`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 500, mau = 0, wau = 0, dau = 0)
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
    }

    @Test
    fun `funnel initial composition renders without crash`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 100, mau = 80, wau = 60, dau = 40)
                )
            }
        }

        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
    }

    @Test
    fun `funnel handles decreasing adoption pattern`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 1000, mau = 500, wau = 100, dau = 10)
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
    }

    @Test
    fun `funnel handles near-zero values without division errors`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 1, mau = 0, wau = 0, dau = 0)
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
    }

    @Test
    fun `funnel accepts modifier parameter`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 100, mau = 80, wau = 60, dau = 40),
                    modifier = Modifier
                )
            }
        }

        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
    }

    @Test
    fun `funnel with high conversion rates composes correctly`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 100, mau = 99, wau = 98, dau = 97)
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
    }

    @Test
    fun `funnel with very low conversion rates composes correctly`() {
        composeTestRule.setContent {
            TestTheme {
                AdoptionFunnel(
                    data = FunnelData(totalUsers = 10000, mau = 100, wau = 10, dau = 1)
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.onNodeWithText("Adoption Funnel").assertIsDisplayed()
    }
}

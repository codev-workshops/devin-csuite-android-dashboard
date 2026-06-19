package com.devin.csuite.presentation.components.animations

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun rememberHaptic(): HapticHelper {
    val haptic = LocalHapticFeedback.current
    return HapticHelper(haptic)
}

class HapticHelper(private val haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    fun performClick() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun performConfirm() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

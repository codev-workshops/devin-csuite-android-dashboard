package com.devin.csuite.presentation.components.animations

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun animatedIntCounter(
    targetValue: Int,
    durationMillis: Int = 1000,
    label: String = "counter"
): Int {
    var triggered by remember { mutableStateOf(false) }
    val animated by animateIntAsState(
        targetValue = if (triggered) targetValue else 0,
        animationSpec = tween(durationMillis = durationMillis),
        label = label
    )
    LaunchedEffect(targetValue) { triggered = true }
    return animated
}

@Composable
fun animatedFloatCounter(
    targetValue: Float,
    durationMillis: Int = 1000,
    label: String = "float_counter"
): Float {
    var triggered by remember { mutableStateOf(false) }
    val animated by animateFloatAsState(
        targetValue = if (triggered) targetValue else 0f,
        animationSpec = tween(durationMillis = durationMillis),
        label = label
    )
    LaunchedEffect(targetValue) { triggered = true }
    return animated
}

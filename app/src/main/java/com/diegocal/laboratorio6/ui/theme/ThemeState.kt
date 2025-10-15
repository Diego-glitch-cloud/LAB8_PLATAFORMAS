package com.diegocal.laboratorio6.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

class ThemeState(val isDark: MutableState<Boolean>) {
    fun toggleTheme() {
        isDark.value = !isDark.value
    }
}

@Composable
fun rememberThemeState(initialIsDark: Boolean = false): ThemeState {
    val isDark = remember { mutableStateOf(initialIsDark) }
    return remember { ThemeState(isDark) }
}
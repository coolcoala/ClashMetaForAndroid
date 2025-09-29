package com.github.kr328.clash.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.github.kr328.clash.design.model.DarkMode
import com.github.kr328.clash.design.store.UiStore

@Composable
fun AppTheme(
    useDarkTheme: Boolean = run {
        val ctx = LocalContext.current
        when (UiStore(ctx).darkMode) {
            DarkMode.Auto -> isSystemInDarkTheme()
            DarkMode.ForceLight -> false
            DarkMode.ForceDark -> true
        }
    },
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        dynamicColor && useDarkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && !useDarkTheme -> dynamicLightColorScheme(context)
        else -> if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}



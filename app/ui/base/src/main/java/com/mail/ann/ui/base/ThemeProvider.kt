package com.mail.ann.ui.base

import androidx.annotation.StyleRes

interface ThemeProvider {
    @get:StyleRes
    val appThemeResourceId: Int

    @get:StyleRes
    val appLightThemeResourceId: Int

    @get:StyleRes
    val appDarkThemeResourceId: Int

    @get:StyleRes
    val dialogThemeResourceId: Int

    @get:StyleRes
    val translucentDialogThemeResourceId: Int
}
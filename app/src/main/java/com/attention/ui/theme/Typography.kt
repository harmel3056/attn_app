package com.attention.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.attention.R

val dmSerifDisplay = FontFamily(
    Font(R.font.dmserifdisplay_regular, FontWeight.Normal)
)

val AppTypography = Typography(
    displayLarge = Typography().displayLarge.copy(fontFamily = dmSerifDisplay),
    displayMedium = Typography().displayMedium.copy(fontFamily = dmSerifDisplay),
    displaySmall = Typography().displaySmall.copy(fontFamily = dmSerifDisplay),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = dmSerifDisplay),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = dmSerifDisplay),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = dmSerifDisplay),
    titleLarge = Typography().titleLarge.copy(fontFamily = dmSerifDisplay)
)
package com.epicmems.stride.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.epicmems.stride.R

/** Chunky dot-matrix face for the big numerals (the "28" / "08:08:35" look). */
val DotFont = FontFamily(Font(R.font.dotgothic16))

/** Flowing handwritten face for accent lines and quotes. */
val HandFont = FontFamily(Font(R.font.caveat))

/** Friendly marker face for labels and small UI text. */
val MarkerFont = FontFamily(Font(R.font.patrick_hand))

val StrideTypography = Typography(
    displayLarge = TextStyle(fontFamily = DotFont, fontWeight = FontWeight.Normal, fontSize = 52.sp, lineHeight = 56.sp),
    displayMedium = TextStyle(fontFamily = DotFont, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 44.sp),
    displaySmall = TextStyle(fontFamily = DotFont, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = MarkerFont, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = MarkerFont, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = MarkerFont, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = MarkerFont, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = MarkerFont, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 16.sp)
)

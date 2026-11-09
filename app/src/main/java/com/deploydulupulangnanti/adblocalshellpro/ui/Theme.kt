package com.deploydulupulangnanti.adblocalshellpro.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.sp
import com.deploydulupulangnanti.adblocalshellpro.R

val Ink = Color(0xFF101410)
val Panel = Color(0xFF191F19)
val Raised = Color(0xFF222A22)
val Line = Color(0xFF333E32)
val Lime = Color(0xFFD5F58B)
val Paper = Color(0xFFF1F3E9)
val Muted = Color(0xFFA5AFA1)
val Warm = Color(0xFFFFC792)

@Composable fun LocalShellTheme(content: @Composable () -> Unit) {
    val heading = FontFamily(Font(R.font.space_medium, FontWeight.Medium), Font(R.font.space_bold, FontWeight.Bold))
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Lime, onPrimary = Ink,
            primaryContainer = Raised, onPrimaryContainer = Lime,
            secondary = Lime, onSecondary = Ink, secondaryContainer = Raised, onSecondaryContainer = Lime,
            tertiary = Lime, onTertiary = Ink, tertiaryContainer = Raised, onTertiaryContainer = Lime,
            background = Ink, onBackground = Paper, surface = Panel, onSurface = Paper,
            surfaceContainerLowest = Ink, surfaceContainerLow = Panel, surfaceContainer = Panel,
            surfaceContainerHigh = Raised, surfaceContainerHighest = Raised,
            surfaceVariant = Raised, onSurfaceVariant = Muted, outline = Line, outlineVariant = Line,
            error = Color(0xFFFFB4AB)),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = heading, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-1.4).sp),
            headlineMedium = TextStyle(fontFamily = heading, fontWeight = FontWeight.Bold, fontSize = 29.sp, lineHeight = 34.sp, letterSpacing = (-0.8).sp),
            titleLarge = TextStyle(fontFamily = heading, fontWeight = FontWeight.Medium, fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = (-0.4).sp),
            titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 23.sp),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
            labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 1.5.sp),
            labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        ), content = content
    )
}

package com.himo.facerecon

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9BB8FF),
    onPrimary = Color(0xFF002B6B),
    primaryContainer = Color(0xFF16408F),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFF9CC9FF),
    onSecondary = Color(0xFF003257),
    secondaryContainer = Color(0xFF154B70),
    onSecondaryContainer = Color(0xFFD6ECFF),
    tertiary = Color(0xFF8EDDEC),
    onTertiary = Color(0xFF00343D),
    tertiaryContainer = Color(0xFF164C57),
    onTertiaryContainer = Color(0xFFD1F2FF),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE2E8F3),
    surface = Color(0xFF121C2D),
    onSurface = Color(0xFFE2E8F3),
    surfaceVariant = Color(0xFF253448),
    onSurfaceVariant = Color(0xFFB4C3D9),
    outline = Color(0xFF8496AD),
    outlineVariant = Color(0xFF3A4A5F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E63FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF001B48),
    secondary = Color(0xFF168DDB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6ECFF),
    onSecondaryContainer = Color(0xFF082F45),
    tertiary = Color(0xFF1F9EBB),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1F2FF),
    onTertiaryContainer = Color(0xFF00343D),
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF142540),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF142540),
    surfaceVariant = Color(0xFFE4EAF7),
    onSurfaceVariant = Color(0xFF465D7E),
    outline = Color(0xFF6C7F9B),
    outlineVariant = Color(0xFFC4CEDD),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val AppTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun FaceRecognitionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

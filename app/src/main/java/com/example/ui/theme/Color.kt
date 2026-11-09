package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Primary & Secondary Brand Tokens
val IndigoPrimary = Color(0xFF1A237E)
val IndigoLight = Color(0xFF534BAE)
val IndigoContainer = Color(0xFFE8EAF6)
val OnIndigoContainer = Color(0xFF0D1B54)

val RoyalBlueSecondary = Color(0xFF2979FF)
val RoyalBlueContainer = Color(0xFFE3F2FD)
val OnRoyalBlueContainer = Color(0xFF0039CB)

// Amber Premium Accent
val AmberGold = Color(0xFFFF8F00)
val AmberContainer = Color(0xFFFFF8E1)
val OnAmberContainer = Color(0xFFE65100)

// Neutral surfaces
val SurfaceLight = Color(0xFFF8F9FE)
val SurfaceCardLight = Color(0xFFFFFFFF)
val SurfaceBorderLight = Color(0xFFE0E0E0)
val TextPrimaryLight = Color(0xFF1E293B)
val TextSecondaryLight = Color(0xFF64748B)

// Dark Palette
val IndigoDark = Color(0xFF9FA8DA)
val IndigoDarkContainer = Color(0xFF1E284A)
val SurfaceDark = Color(0xFF0F172A)
val SurfaceCardDark = Color(0xFF1E293B)
val TextPrimaryDark = Color(0xFFF1F5F9)
val TextSecondaryDark = Color(0xFF94A3B8)

// Subject Badge Specific Colors
object SubjectColors {
    val Math = Color(0xFF3F51B5)
    val Physics = Color(0xFF00897B)
    val Chemistry = Color(0xFFE64A19)
    val Hindi = Color(0xFFD84315)
    val English = Color(0xFF7B1FA2)
    val Default = Color(0xFF455A64)

    fun colorForSubject(subject: String): Color {
        return when (subject.trim().lowercase()) {
            "math", "mathematics" -> Math
            "physics" -> Physics
            "chemistry" -> Chemistry
            "hindi" -> Hindi
            "english" -> English
            else -> Default
        }
    }
}

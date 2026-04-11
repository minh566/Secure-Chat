package com.securechat.ui.screens.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

internal data class HomeChatListTokens(
    val screenBackground: Color,
    val title: Color,
    val headerIcon: Color,
    val searchContainer: Color,
    val searchText: Color,
    val sectionLabel: Color,
    val divider: Color,
    val listCard: Color,
    val featuredCard: Color,
    val nameText: Color,
    val bodyText: Color,
    val metaText: Color,
    val badge: Color,
    val accent: Color,
    val activeNowCard: Color,
    val emptyIcon: Color,
    val infoContainer: Color,
    val infoText: Color
)

@Composable
internal fun homeChatListTokens(): HomeChatListTokens {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.45f

    return if (isDark) {
        HomeChatListTokens(
            screenBackground = Color(0xFF111423),
            title = Color(0xFFEAF0FF),
            headerIcon = Color(0xFF9CB2FF),
            searchContainer = Color(0xFF1A2036),
            searchText = Color(0xFFAEB7D9),
            sectionLabel = Color(0xFF9099BC),
            divider = Color(0xFF222A45),
            listCard = Color(0xFF181E33),
            featuredCard = Color(0xFF1A2240),
            nameText = Color(0xFFF4F7FF),
            bodyText = Color(0xFFABB5DA),
            metaText = Color(0xFF8D97BE),
            badge = Color(0xFF7391FF),
            accent = Color(0xFF7A96FF),
            activeNowCard = Color(0xFF1A2240),
            emptyIcon = Color(0xFF717DAA),
            infoContainer = Color(0xFF1D2744),
            infoText = Color(0xFFD2DEFF)
        )
    } else {
        HomeChatListTokens(
            screenBackground = Color(0xFFF4F5FF),
            title = Color(0xFF1F2A5A),
            headerIcon = Color(0xFF5B72F2),
            searchContainer = Color(0xFFE8EAFE),
            searchText = Color(0xFF8A90B5),
            sectionLabel = Color(0xFF7A81A7),
            divider = Color(0xFFE8EBFB),
            listCard = Color(0xFFF8F9FF),
            featuredCard = Color(0xFFE8EAFE),
            nameText = Color(0xFF222A57),
            bodyText = Color(0xFF6D769E),
            metaText = Color(0xFF8A90B5),
            badge = Color(0xFF5B72F2),
            accent = Color(0xFF5B72F2),
            activeNowCard = Color(0xFFE8EAFE),
            emptyIcon = Color(0xFF9EA5C8),
            infoContainer = Color(0xFFDEE8FF),
            infoText = Color(0xFF22305A)
        )
    }
}


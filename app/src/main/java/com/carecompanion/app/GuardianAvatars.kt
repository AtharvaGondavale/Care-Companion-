package com.carecompanion.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector

object GuardianAvatars {
    private val pairs: List<Pair<ImageVector, Color>> = listOf(
        Icons.Outlined.Person to Color(0xFFE8F4FD),
        Icons.Outlined.Favorite to Color(0xFFFDF2F8),
        Icons.Outlined.LocalHospital to Color(0xFFE8F5E9),
        Icons.Outlined.Call to Color(0xFFFFF8E1),
        Icons.Outlined.Phone to Color(0xFFE0F7FA),
        Icons.Outlined.Home to Color(0xFFF3E5F5),
    )

    private val keys = listOf("person", "favorite", "hospital", "call", "phone", "home")

    fun options(): List<Pair<ImageVector, Color>> = pairs

    fun keyFromIndex(selectedIndex: Int): String =
        keys.getOrElse(selectedIndex.coerceIn(0, keys.lastIndex)) { keys[0] }

    fun indexFromKey(key: String?): Int =
        keys.indexOf(key ?: "").takeIf { it >= 0 } ?: 0

    fun pairFromKey(key: String?): Pair<ImageVector, Color> =
        pairs.getOrElse(indexFromKey(key)) { pairs[0] }

    fun argbFromColor(c: Color): Int = c.toArgb()
}

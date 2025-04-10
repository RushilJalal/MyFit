package com.example.myfit_new

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Snowshoeing
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavigationItem(val route: String, val title: String, val icon: ImageVector) {
    data object Steps : NavigationItem("steps", "Steps", Icons.Default.Snowshoeing)
    data object Journal : NavigationItem("journal", "Journal", Icons.Default.Book)
    data object Profile : NavigationItem("profile", "Profile", Icons.Default.AccountCircle)
}
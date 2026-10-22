package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.ui.graphics.vector.ImageVector

enum class SolarScreen(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    DASHBOARD("Overview", Icons.Default.Dashboard, "nav_dashboard"),
    TASKS("Work Order", Icons.Default.Assignment, "nav_tasks"),
    ADMIN("Admin", Icons.Default.AdminPanelSettings, "nav_admin"),
    EXPENSES("Expenses", Icons.Default.ReceiptLong, "nav_expenses"),
    REPORTS("Reports", Icons.Default.Assessment, "nav_reports")
}

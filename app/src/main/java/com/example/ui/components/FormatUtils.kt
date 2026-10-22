package com.example.ui.components

import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.util.Locale

object FormatUtils {

    fun formatInr(amount: Double, withDecimals: Boolean = false): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        format.maximumFractionDigits = if (withDecimals) 2 else 0
        return format.format(amount).replace("INR", "₹").trim()
    }

    fun formatLakhs(amount: Double): String {
        return if (amount >= 10000000) {
            String.format(Locale.US, "₹%.2f Cr", amount / 10000000.0)
        } else if (amount >= 100000) {
            String.format(Locale.US, "₹%.2f L", amount / 100000.0)
        } else if (amount >= 1000) {
            String.format(Locale.US, "₹%.1f k", amount / 1000.0)
        } else {
            String.format(Locale.US, "₹%.0f", amount)
        }
    }

    fun getCategoryColor(category: String): Color {
        return when (category) {
            "Civil & Foundation" -> Color(0xFFD97706) // Amber
            "MMS & Modules" -> Color(0xFF0284C7) // Cyan/Sky
            "DC Cabling" -> Color(0xFF7C3AED) // Purple
            "Earthing & LA" -> Color(0xFF10B981) // Emerald
            "Inverters & Panels" -> Color(0xFFEA580C) // Orange
            "AC Cabling & Trench" -> Color(0xFF2563EB) // Blue
            "HT Cabling" -> Color(0xFFDC2626) // Red/Crimson
            "Aux & Control Cables" -> Color(0xFF0D9488) // Teal
            "WMS & Ancillary" -> Color(0xFF8B5CF6) // Violet
            else -> Color(0xFF64748B) // Slate
        }
    }
}

package com.example.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.AppIcons
import com.example.ui.theme.RingBills
import com.example.ui.theme.RingEntertainment
import com.example.ui.theme.RingFood
import com.example.ui.theme.RingGroceries
import com.example.ui.theme.RingHealth
import com.example.ui.theme.RingRemittance
import com.example.ui.theme.RingShopping
import com.example.ui.theme.RingTransport

enum class ExpenseCategory(
    val displayName: String,
    val shortName: String,
    val ringColor: Color,
    val icon: ImageVector,
    val defaultBudgetRatio: Double,
) {
    FOOD_DINING(
        displayName = "Food & Dining",
        shortName = "Food",
        ringColor = RingFood,
        icon = AppIcons.Restaurant,
        defaultBudgetRatio = 0.22,
    ),
    GROCERIES_KIRANA(
        displayName = "Groceries & Kirana",
        shortName = "Groceries",
        ringColor = RingGroceries,
        icon = AppIcons.LocalGroceryStore,
        defaultBudgetRatio = 0.16,
    ),
    TRANSPORT_TRAVEL(
        displayName = "Transport & Travel",
        shortName = "Transport",
        ringColor = RingTransport,
        icon = AppIcons.DirectionsCar,
        defaultBudgetRatio = 0.14,
    ),
    BILLS_UTILITIES(
        displayName = "Bills & Utilities",
        shortName = "Bills",
        ringColor = RingBills,
        icon = AppIcons.ReceiptLong,
        defaultBudgetRatio = 0.16,
    ),
    SHOPPING_LIFESTYLE(
        displayName = "Shopping & Lifestyle",
        shortName = "Shopping",
        ringColor = RingShopping,
        icon = AppIcons.ShoppingBag,
        defaultBudgetRatio = 0.12,
    ),
    REMITTANCE_TRANSFERS(
        displayName = "Remittance & Transfers",
        shortName = "Transfers",
        ringColor = RingRemittance,
        icon = AppIcons.AccountBalance,
        defaultBudgetRatio = 0.10,
    ),
    HEALTH_WELLNESS(
        displayName = "Health & Wellness",
        shortName = "Health",
        ringColor = RingHealth,
        icon = Icons.Outlined.FavoriteBorder,
        defaultBudgetRatio = 0.05,
    ),
    ENTERTAINMENT(
        displayName = "Entertainment & Media",
        shortName = "Media",
        ringColor = RingEntertainment,
        icon = AppIcons.Movie,
        defaultBudgetRatio = 0.05,
    );

    companion object {
        fun fromName(name: String): ExpenseCategory =
            entries.firstOrNull { it.name == name } ?: SHOPPING_LIFESTYLE
    }
}

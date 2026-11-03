package com.example.poultryapp.ui.screens.golden_breeding.components

import androidx.compose.runtime.Composable

/**
 * این کامپوننت به درخواست کاربر به دلیل حذف کامل سیستم سکه‌ها و فروشگاه منسوخ شده است.
 */
@Deprecated("فروشگاه و سکه‌ها به طور کامل از بازی حذف گردیده‌اند.")
@Composable
fun FarmBoosterShopDialog(
    onDismiss: () -> Unit = {},
    onBalanceChanged: () -> Unit = {}
) {
    // خالی - فروشگاه به صورت کامل حذف شده است
}

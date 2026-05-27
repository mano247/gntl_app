package com.gentlemanstore.core.util

object CurrencyFormatter {

    private const val EUR_RATE = 0.0085
    private const val USD_RATE = 0.0092

    fun format(amountRsd: Double, currency: String): String{
        return when (currency) {
            Constants.CURRENCY_EUR -> {
                val converted = amountRsd * EUR_RATE
                "€ ${"%.2f".format(converted)}"
            }
            Constants.CURRENCY_USD -> {
                val converted = amountRsd * USD_RATE
                "$ ${"%.2f".format(converted)}"
            }
            else -> {
                "${"%.2f".format(amountRsd)} din"
            }
        }
    }

    fun formatWithDiscount(
        amountRsd: Double,
        discountPercent: Double,
        currency: String
    ): Pair<String, String> {
        val discounted = amountRsd * (1 - discountPercent / 100)
        return Pair(
            format(amountRsd, currency),
            format(discounted, currency)
        )
    }

}
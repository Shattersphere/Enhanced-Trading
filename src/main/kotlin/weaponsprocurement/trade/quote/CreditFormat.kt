package weaponsprocurement.trade.quote

import com.shattersphere.shatterlib.format.CreditFormat as ShatterCreditFormat
import com.shattersphere.shatterlib.format.NumberFormat as ShatterNumberFormat

object CreditFormat {
    const val CREDIT_SYMBOL: String = "\u00a2"

    @JvmStatic
    fun credits(credits: Int): String = credits(credits.toLong())

    @JvmStatic
    fun credits(credits: Long): String = ShatterCreditFormat.short(credits)

    @JvmStatic
    fun creditsLong(credits: Int): String = creditsLong(credits.toLong())

    @JvmStatic
    fun creditsLong(credits: Long): String = ShatterCreditFormat.long(credits)

    @JvmStatic
    fun grouped(value: Int): String = grouped(value.toLong())

    @JvmStatic
    fun grouped(value: Long): String = ShatterNumberFormat.grouped(value)
}

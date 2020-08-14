package com.absinthe.libraries

import android.content.Context
import com.drakeet.about.Contributor

object Absinthe {

    const val ME = "Absinthe"
    const val EMAIL = "absinthe@absinthe.life"
    const val PERSONAL_EMAIL = "zhaobozhen2025@gmail.com"
    const val GITHUB_HOME_PAGE = "https://github.com/zhaobozhen"
    const val WEBSITE = "https://absinthe.life"
    const val MARKET_DETAIL_SCHEME = "market://details?id="
    const val COOLAPK_HOME_PAGE = "coolmarket://u/482045"

    const val ANYWHERE_ = "com.absinthe.anywhere_"
    const val KAGE = "com.absinthe.kage"
    const val LIBCHECKER = "com.absinthe.libchecker"
    const val TAMASHII = "com.absinthe.tamashii"

    fun getAboutPageRecommendedApps(context: Context, packageName: String = ""): List<Contributor> {

        val list = mutableListOf(
                Contributor(R.drawable.anywhere_icon, "Anywhere-", context.getString(R.string.anywhere_intro), "$MARKET_DETAIL_SCHEME$ANYWHERE_"),
                Contributor(R.drawable.kage_icon, "Kage(Beta)", context.getString(R.string.kage_intro), "$MARKET_DETAIL_SCHEME$KAGE"),
                Contributor(R.drawable.libchecker_icon, "LibChecker", context.getString(R.string.lc_intro), "$MARKET_DETAIL_SCHEME$LIBCHECKER"),
                Contributor(R.drawable.tamashii_icon, "Tamashii", context.getString(R.string.tamashii_intro))
        )

        if (packageName.isNotBlank()) {
            when (packageName) {
                ANYWHERE_ -> list.removeAt(0)
                KAGE -> list.removeAt(1)
                LIBCHECKER -> list.removeAt(2)
                TAMASHII -> list.removeAt(3)
            }
        }

        return list
    }
}
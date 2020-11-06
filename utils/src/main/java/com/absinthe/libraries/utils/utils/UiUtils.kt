@file:Suppress("DEPRECATION")

package com.absinthe.libraries.utils.utils

import android.app.Activity
import android.content.ContentResolver
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.absinthe.libraries.utils.R
import com.absinthe.libraries.utils.extensions.dp

object UiUtils {

    fun setSystemBarStyle(window: Window) {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        if (!isDarkMode()) {
            window.decorView.systemUiVisibility =
                    window.decorView.systemUiVisibility or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = (
                        window.decorView.systemUiVisibility
                                or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            }
            if (getNavBarHeight(Utility.getAppContext().contentResolver) > 20.dp) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    window.decorView.systemUiVisibility = (
                            window.decorView.systemUiVisibility
                                    or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
                }
            }
        }
        setSystemBarTransparent(window)
    }

    fun setSystemBarTransparent(window: Window) {
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
        }
    }

    fun isDarkMode(): Boolean {
        return when (Utility.getAppContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            else -> false
        }
    }

    fun getNavBarHeight(contentResolver: ContentResolver): Int {
        //Full screen adaption
        if (Settings.Global.getInt(contentResolver, "force_fsg_nav_bar", 0) != 0) {
            return 20.dp
        }

        val res = Resources.getSystem()
        val resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android")

        return if (resourceId != 0) {
            res.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    fun getActionBarSize(activity: Activity): Int {
        val tv = TypedValue()

        return if (activity.theme.resolveAttribute(R.attr.actionBarSize, tv, true)) {
            TypedValue.complexToDimensionPixelSize(
                    tv.data,
                    activity.resources.displayMetrics
            )
        } else {
            0
        }
    }
}
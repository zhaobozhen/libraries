package life.absinthe.libraries.utils.manager

import android.graphics.Rect
import android.util.Log
import android.view.ViewTreeObserver
import android.view.Window
import life.absinthe.libraries.utils.utils.UiUtils

const val NOT_MEASURED = -1

object SystemBarManager {

    var statusBarSize: Int = NOT_MEASURED
        private set

    var navigationBarSize: Int = NOT_MEASURED
        private set

    fun measureSystemBar(window: Window) {
        if (statusBarSize == NOT_MEASURED) {
            statusBarSize = UiUtils.getStatusBarHeight()
        }

        if (navigationBarSize == NOT_MEASURED) {
            window.decorView.let {
                it.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        val r = Rect()
                        val rootViewHeight = it.rootView.height
                        if (rootViewHeight <= 0) { return }

                        it.getWindowVisibleDisplayFrame(r)
                        val navigationBarHeight: Int = rootViewHeight - r.bottom

                        if (navigationBarHeight > 0) {
                            Log.d("SystemBarManager", "navigationBarSize = $navigationBarHeight")
                            navigationBarSize = navigationBarHeight
                        }
                        it.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
            }
        }
    }
}
package life.absinthe.libraries.utils.utils

import android.annotation.SuppressLint
import android.app.Application

/**
 * <pre>
 * author : Absinthe
 * time : 2020/08/31
 * </pre>
 */
object Utility {

    private var application: Application? = null

    fun init(application: Application) {
        this.application = application
    }

    fun getAppContext(): Application {
        return application ?: throw NullPointerException("you must call init method in Application#onCreate")
    }

    @SuppressLint("PrivateApi")
    fun getSystemProperty(key: String): String? {
        try {
            val props = Class.forName("android.os.SystemProperties")
            return props.getMethod("get", String::class.java).invoke(null, key) as String
        } catch (ignore: Exception) {
        }
        return null
    }

}
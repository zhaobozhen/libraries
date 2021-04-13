package life.absinthe.libraries.utils.manager

import android.app.Activity
import android.os.Process
import androidx.appcompat.app.AppCompatActivity
import java.lang.ref.WeakReference
import java.util.*

object ActivityStackManager {
    /***
     * Get Activity Stack
     *
     * @return Activity stack
     */
    private val stack: Stack<WeakReference<AppCompatActivity>> = Stack()

    /***
     * Size of Activities
     *
     * @return Size of Activities
     */
    val stackSize = stack.size

    /***
     * Get top stack Activity
     *
     * @return Activity
     */
    val topActivity = stack.lastElement().get()

    /**
     * Add Activity to stack
     *
     * @param activity Weak Reference of Activity
     */
    fun addActivity(activity: WeakReference<AppCompatActivity>) {
        stack.add(activity)
    }

    /**
     * Delete Activity
     *
     * @param activity Weak Reference of Activity
     */
    fun removeActivity(activity: WeakReference<AppCompatActivity>) {
        stack.remove(activity)
    }

    /***
     * Get Activity by class
     *
     * @param cls Activity class
     * @return Activity
     */
    fun getActivity(cls: Class<*>): AppCompatActivity? {
        return stack.find { it.get()?.javaClass == cls }?.get()
    }

    /**
     * Kill top stack Activity
     */
    fun killTopActivity() {
        try {
            killActivity(stack.lastElement())
        } catch (ignore: NoSuchElementException) {

        }
    }

    /***
     * Kill Activity
     *
     * @param activity Activity want to kill
     */
    private fun killActivity(activity: WeakReference<AppCompatActivity>) {
        try {
            val iterator = stack.iterator()
            while (iterator.hasNext()) {
                val stackActivity = iterator.next()
                if (stackActivity.get() == null) {
                    iterator.remove()
                    continue
                }
                stackActivity.get()?.let {
                    if (it.javaClass.name == activity.get()?.javaClass?.name) {
                        iterator.remove()
                        it.finish()
                        return
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    /***
     * Kill Activity by class
     *
     * @param cls class
     */
    fun killActivity(cls: Class<*>) {
        try {
            val listIterator = stack.listIterator()
            while (listIterator.hasNext()) {
                val activity: Activity? = listIterator.next().get()
                if (activity == null) {
                    listIterator.remove()
                    continue
                }
                if (activity.javaClass == cls) {
                    listIterator.remove()
                    activity.finish()
                    break
                }
            }
        } catch (e: Exception) {
        }
    }

    /**
     * Kill all Activity
     */
    private fun killAllActivity() {
        try {
            val listIterator = stack.listIterator()
            while (listIterator.hasNext()) {
                listIterator.next().get()?.finish()
                listIterator.remove()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Exit application
     */
    fun exitApplication() {
        killAllActivity()
        Process.killProcess(Process.myPid())
    }
}
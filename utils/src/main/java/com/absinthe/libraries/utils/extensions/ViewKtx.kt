package com.absinthe.libraries.utils.extensions

import android.content.res.Resources
import android.view.View

val Number.dp: Int get() = (toInt() * Resources.getSystem().displayMetrics.density).toInt()

var View.paddingStartCompat: Int
    set(value) {
        setPadding(value, paddingTop, paddingEnd, paddingBottom)
    }
    get() = paddingStart

fun View.addPaddingStart(padding: Int) {
    setPadding(paddingStart + padding, paddingTop, paddingEnd, paddingBottom)
}

var View.paddingTopCompat: Int
    set(value) {
        setPadding(paddingStart, value, paddingEnd, paddingBottom)
    }
    get() = paddingTop

fun View.addPaddingTop(padding: Int) {
    setPadding(paddingStart, paddingTop + padding, paddingEnd, paddingBottom)
}

var View.paddingEndCompat: Int
    set(value) {
        setPadding(paddingStart, paddingTop, value, paddingBottom)
    }
    get() = paddingEnd

fun View.addPaddingEnd(padding: Int) {
    setPadding(paddingStart, paddingTop, paddingEnd + padding, paddingBottom)
}

var View.paddingBottomCompat: Int
    set(value) {
        setPadding(paddingStart, paddingTop, paddingEnd, value)
    }
    get() = paddingBottom

fun View.addPaddingBottom(padding: Int) {
    setPadding(paddingStart, paddingTop, paddingEnd, paddingBottom + padding)
}
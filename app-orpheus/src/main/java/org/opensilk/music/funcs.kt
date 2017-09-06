package org.opensilk.music

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View

/**
 * Created by drew on 9/5/17.
 */
fun View.findActivity(): Activity = this.context.findActivity()

fun Context.findActivity(): Activity = when {
    this is Activity -> this
    this is ContextWrapper -> this.findActivity()
    else -> throw AssertionError("Unknown context type " + javaClass.name)
}
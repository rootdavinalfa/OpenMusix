/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.base

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
            WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
        )*/
    }

    /**
     * Change status bar color by using following parameter
     *
     * [activity] Fill with requester Activity eg:MainActivity or using 'this'
     *
     * [color] Fill with ID
     *
     * [lightThemeIcon] Statement using light icon or not (Default True)
     *
     * */
    protected open fun changeStatusBar(
        activity: Activity,
        color: Int,
        lightThemeIcon: Boolean = true
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = window.decorView.systemUiVisibility
            if (lightThemeIcon) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            window.decorView.systemUiVisibility = flags
            activity.window.statusBarColor = getColor(color)
        }
    }
}
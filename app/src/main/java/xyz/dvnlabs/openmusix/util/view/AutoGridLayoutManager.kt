/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.util.view

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView


class AutoGridLayoutManager(context: Context?) : GridLayoutManager(context, 2) {
    private var columnWidth = 0
    private var columnWidthChanged = true
    private fun setColumnWidth(newColumnWidth: Int) {
        if (newColumnWidth > 0 && newColumnWidth != columnWidth) {
            columnWidth = newColumnWidth
            columnWidthChanged = true
        }
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        if (columnWidthChanged && columnWidth > 0) {
            val totalSpace: Int = if (orientation == RecyclerView.VERTICAL) {
                width - paddingRight - paddingLeft
            } else {
                height - paddingTop - paddingBottom
            }
            val spanCount = 1.coerceAtLeast(totalSpace / columnWidth)
            setSpanCount(spanCount)
            columnWidthChanged = false
        }
        super.onLayoutChildren(recycler, state)
    }

    init {
        setColumnWidth(columnWidth)
    }
}
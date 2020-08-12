/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import com.yarolegovich.discretescrollview.DiscreteScrollView

class NewDiscreteScrollView(context: Context, attributeSet: AttributeSet) :
    DiscreteScrollView(context, attributeSet) {

    var onItemMove: (Int, Int, Boolean) -> Unit = { currentPosition, oldPosition, byUser -> Unit }
    private var isTouched = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        isTouched = true
        return super.onTouchEvent(e)
    }

    override fun setLayoutManager(layout: LayoutManager?) {
        super.setLayoutManager(layout)
    }

    override fun scrollToPosition(position: Int) {
        isTouched = false
        super.scrollToPosition(position)
    }

    override fun smoothScrollToPosition(position: Int) {
        isTouched = false
        super.smoothScrollToPosition(position)
    }

    override fun onScrollStateChanged(state: Int) {
        when (state) {
            RecyclerView.SCROLL_STATE_SETTLING -> {
                isTouched = true
            }
            RecyclerView.SCROLL_STATE_IDLE -> {
                val vh = this.getViewHolder(this.currentItem)
                onItemMove(vh?.absoluteAdapterPosition!!, vh.oldPosition, isTouched)
            }
            RecyclerView.SCROLL_STATE_DRAGGING -> {
                isTouched = true
            }
        }
        super.onScrollStateChanged(state)
    }
}
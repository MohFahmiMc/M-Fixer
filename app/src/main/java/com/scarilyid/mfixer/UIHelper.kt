package com.scarilyid.mfixer

import android.view.View
import android.view.animation.AnimationUtils

object UIHelper {
    // Animasi munculin list file satu per satu (tidak kaku)
    fun animateList(view: View, position: Int) {
        val animation = AnimationUtils.loadAnimation(view.context, android.R.anim.slide_in_left)
        animation.startOffset = (position * 50).toLong() // Efek delay per baris
        view.startAnimation(animation)
    }

    // Efek klik tombol agar terasa responsif
    fun applyClickEffect(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> v.alpha = 0.5f
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> v.alpha = 1.0f
            }
            false
        }
    }
}

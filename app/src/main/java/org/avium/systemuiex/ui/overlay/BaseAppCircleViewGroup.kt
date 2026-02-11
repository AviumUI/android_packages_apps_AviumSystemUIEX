/*
 *
 * Copyright (C) 2025 The AviumUI Project
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.avium.systemuiex.ui.overlay

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import org.avium.systemuiex.util.Config.CIRCLE_CENTER_Y_LAND
import org.avium.systemuiex.util.Config.CIRCLE_CENTER_Y_PORT
import org.avium.systemuiex.util.Config.CIRCLE_OFFSET_X_LAND
import org.avium.systemuiex.util.Config.CIRCLE_OFFSET_X_PORT
import org.avium.systemuiex.util.Config.ICON_SIZE_RATIO
import org.avium.systemuiex.util.Config.ICON_SPACING_MULTIPLIER
import org.avium.systemuiex.util.Config.getCircleRadiusRatioArray
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.core.graphics.drawable.toDrawable

abstract class BaseAppCircleViewGroup(
    context: Context,
) : ViewGroup(context) {

    private var hasAnimated = false
    private val animationDuration = 180L
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    
    private var selectedChildIndex: Int = -1
    private var lastVibratedIndex: Int = -1
    private var isTouching: Boolean = false
    private var hasReceivedTouch: Boolean = false
    private var isFirstTouchUp: Boolean = true
    
    private var initialTouchX: Float = -1f
    private var initialTouchY: Float = -1f
    private var hasInitialTouchPoint: Boolean = false
    
    private val iconLaunchListeners = mutableListOf<(Int) -> Unit>()
    private val dismissListeners = mutableListOf<() -> Unit>()

    init {
        setWillNotDraw(false)
        background = Color.BLACK.toDrawable().apply { alpha = 0 }
        isClickable = true
        isFocusable = true
    }

    abstract fun isLeft(): Boolean
    
    fun setOnIconLaunchListener(listener: (Int) -> Unit) {
        iconLaunchListeners.add(listener)
    }
    
    fun setOnDismissListener(listener: () -> Unit) {
        dismissListeners.add(listener)
    }
    
    fun setInitialTouchPoint(x: Float, y: Float) {
        initialTouchX = x
        initialTouchY = y
        hasInitialTouchPoint = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(widthSize, heightSize)
    }

    private fun getNavbarHeight(): Int {
        return windowManager.currentWindowMetrics.windowInsets.getInsets(WindowInsets.Type.navigationBars()).bottom
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        if (count == 0) return

        val bounds = windowManager.currentWindowMetrics.bounds
        val screenWidth = bounds.width()
        val screenHeight = bounds.height()
        val navbarHeight = getNavbarHeight()

        val iconRadius = min(screenWidth, screenHeight) * ICON_SIZE_RATIO / 2
        val circleXOffset = if (screenWidth > screenHeight) CIRCLE_OFFSET_X_LAND else CIRCLE_OFFSET_X_PORT
        val circleCenterY = if (screenWidth > screenHeight) CIRCLE_CENTER_Y_LAND else CIRCLE_CENTER_Y_PORT

        val circleRadiusRatioArray = getCircleRadiusRatioArray(count)

        for (i in 0 until count) {
            val currentCircleTotal = count
            val currentCircleIdx = i + 1
            val circleIdx = 0
            val radius = min(screenWidth, screenHeight) * circleRadiusRatioArray[circleIdx]
            val angle = 45f + ((currentCircleTotal + 1) / 2f - currentCircleIdx) * (90f / (currentCircleTotal + 1)) * ICON_SPACING_MULTIPLIER

            val x = (if (isLeft()) {
                (screenWidth * circleXOffset + radius * cos(angle * Math.PI / 180) + iconRadius).toInt()
            } else {
                (screenWidth * (1f - circleXOffset) - radius * cos(angle * Math.PI / 180) - iconRadius).toInt()
            })
            val y = (screenHeight * circleCenterY - radius * sin(angle * Math.PI / 180) - iconRadius).toInt() - navbarHeight

            getChildAt(i).layout(
                (x - iconRadius).toInt(),
                (y - iconRadius).toInt(),
                (x + iconRadius).toInt(),
                (y + iconRadius).toInt()
            )
        }

        if (!hasAnimated) {
            startIntroAnimation()
            hasAnimated = true
        }
    }

    private fun startIntroAnimation() {
        val backgroundAnimator = ObjectAnimator.ofInt(background, "alpha", 0, 128)
        backgroundAnimator.duration = animationDuration
        backgroundAnimator.start()

        val bounds = windowManager.currentWindowMetrics.bounds
        val screenWidth = bounds.width()
        val screenHeight = bounds.height()
        val navbarHeight = getNavbarHeight()

        val circleXOffset = if (screenWidth > screenHeight) CIRCLE_OFFSET_X_LAND else CIRCLE_OFFSET_X_PORT
        val circleCenterY = if (screenWidth > screenHeight) CIRCLE_CENTER_Y_LAND else CIRCLE_CENTER_Y_PORT

        val centerX = if (isLeft()) screenWidth * circleXOffset else screenWidth * (1f - circleXOffset)
        val centerY = screenHeight * circleCenterY - navbarHeight

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.alpha = 0f

            val startX = centerX - (child.left + child.width / 2)
            val startY = centerY - (child.top + child.height / 2)

            child.translationX = startX
            child.translationY = startY

            val translationXAnimator = ObjectAnimator.ofFloat(child, "translationX", startX, 0f)
            val translationYAnimator = ObjectAnimator.ofFloat(child, "translationY", startY, 0f)
            val alphaAnimator = ObjectAnimator.ofFloat(child, "alpha", 0f, 1f)

            val animatorSet = AnimatorSet()
            animatorSet.playTogether(translationXAnimator, translationYAnimator, alphaAnimator)
            animatorSet.interpolator = DecelerateInterpolator(1.5f)
            animatorSet.duration = animationDuration + i * 15
            animatorSet.start()
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isTouching = true
                hasReceivedTouch = true
                if (hasInitialTouchPoint) {
                    updateSelectedIcon(event.x, event.y)
                    if (selectedChildIndex < 0) {
                        checkIconAlongPath(initialTouchX, initialTouchY, event.x, event.y)
                    }
                } else {
                    updateSelectedIcon(event.x, event.y)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isTouching) {
                    updateSelectedIcon(event.x, event.y)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isTouching) {
                    if (selectedChildIndex >= 0 && selectedChildIndex < childCount) {
                        iconLaunchListeners.forEach { it(selectedChildIndex) }
                    } else if (hasReceivedTouch && !isFirstTouchUp) {
                        dismissListeners.forEach { it() }
                    }
                    isFirstTouchUp = false
                    resetTouchState()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (isTouching) {
                    resetTouchState()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun checkIconAlongPath(startX: Float, startY: Float, endX: Float, endY: Float) {
        val steps = 5
        for (i in 1 until steps) {
            val ratio = i.toFloat() / steps
            val checkX = startX + (endX - startX) * ratio
            val checkY = startY + (endY - startY) * ratio
            
            for (j in 0 until childCount) {
                val child = getChildAt(j)
                if (checkX >= child.left && checkX <= child.right && 
                    checkY >= child.top && checkY <= child.bottom) {
                    selectedChildIndex = j
                    updateIconScales()
                    performHapticFeedback()
                    lastVibratedIndex = j
                    return
                }
            }
        }
    }
    
    private fun resetTouchState() {
        isTouching = false
        selectedChildIndex = -1
        lastVibratedIndex = -1
    }
    
    private fun updateSelectedIcon(x: Float, y: Float) {
        var newSelectedIndex = -1
        
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (x >= child.left && x <= child.right && y >= child.top && y <= child.bottom) {
                newSelectedIndex = i
                break
            }
        }
        
        if (newSelectedIndex != selectedChildIndex) {
            selectedChildIndex = newSelectedIndex
            updateIconScales()
        }
        
        if (selectedChildIndex >= 0 && selectedChildIndex != lastVibratedIndex) {
            performHapticFeedback()
            lastVibratedIndex = selectedChildIndex
        }
    }
    
    private fun updateIconScales() {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (i == selectedChildIndex) {
                child.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .setDuration(100)
                    .start()
            } else {
                child.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start()
            }
        }
    }
    
    private fun resetIconScales() {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(100)
                .start()
        }
    }
    
    private fun performHapticFeedback() {
        val effect = VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(effect)
    }
    
    fun dispatchTouchCoordinates(x: Float, y: Float, isUp: Boolean) {
        when {
            !isTouching && !isUp -> {
                isTouching = true
                hasReceivedTouch = true
                updateSelectedIcon(x, y)
            }
            isTouching && !isUp -> {
                updateSelectedIcon(x, y)
            }
            isUp -> {
                if (selectedChildIndex >= 0 && selectedChildIndex < childCount) {
                    iconLaunchListeners.forEach { it(selectedChildIndex) }
                } else if (hasReceivedTouch && !isFirstTouchUp) {
                    dismissListeners.forEach { it() }
                }
                isFirstTouchUp = false
                resetTouchState()
            }
        }
    }

    companion object {
        val groupLayoutParams = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            format = PixelFormat.RGBA_8888
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            
            systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }
}
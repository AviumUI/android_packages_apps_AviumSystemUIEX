package org.avium.systemuiex.ui.overlay

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import org.avium.systemuiex.util.Config.CIRCLE_CENTER_Y_LAND
import org.avium.systemuiex.util.Config.CIRCLE_CENTER_Y_PORT
import org.avium.systemuiex.util.Config.CIRCLE_OFFSET_X_LAND
import org.avium.systemuiex.util.Config.CIRCLE_OFFSET_X_PORT
import org.avium.systemuiex.util.Config.ICON_SIZE_RATIO
import org.avium.systemuiex.util.Config.ICON_SPACING_MULTIPLIER
import org.avium.systemuiex.util.Config.getCircleMaxIconArray
import org.avium.systemuiex.util.Config.getCircleRadiusRatioArray
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.core.graphics.drawable.toDrawable

abstract class BaseAppCircleViewGroup(
    context: Context,
) : ViewGroup(context) {

    private var hasAnimated = false
    private val animationDuration = 200L
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    init {
        setWillNotDraw(false)
        background = Color.BLACK.toDrawable().apply { alpha = 0 }
    }

    abstract fun isLeft(): Boolean

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
            animatorSet.interpolator = DecelerateInterpolator(2.5f)
            animatorSet.duration = animationDuration + i * 20
            animatorSet.start()
        }
    }

    companion object {
        @Suppress("DEPRECATION")
        val groupLayoutParams = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            format = PixelFormat.RGBA_8888
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
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
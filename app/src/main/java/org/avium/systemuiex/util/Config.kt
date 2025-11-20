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


package org.avium.systemuiex.util

object Config {

    const val TRIGGER_AREA_WIDTH_DP = 20f
    const val TRIGGER_AREA_HEIGHT_DP = 60f
    const val MIN_SWIPE_DISTANCE_DP = 30f
    const val MAX_SWIPE_DISTANCE_DP = 300f
    const val SWIPE_ANGLE_TOLERANCE = 25.0

    const val CIRCLE_OFFSET_X_PORT = 0.1f
    const val CIRCLE_CENTER_Y_PORT = 0.95f
    const val CIRCLE_OFFSET_X_LAND = 0.1f
    const val CIRCLE_CENTER_Y_LAND = 0.95f
    const val ICON_SIZE_RATIO = 0.1f
    const val ICON_SPACING_MULTIPLIER = 1.5f

    const val MAX_ICONS = 6

    const val CIRCLE_RADIUS_RATIO = 0.4f

    fun getCircleRadiusRatioArray(count: Int): FloatArray {
        return floatArrayOf(CIRCLE_RADIUS_RATIO)
    }

    fun getCircleMaxIconArray(count: Int): IntArray {
        return intArrayOf(MAX_ICONS)
    }
}

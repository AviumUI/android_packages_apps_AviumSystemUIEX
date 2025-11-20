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

package org.avium.systemuiex.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import org.avium.systemuiex.R
import org.avium.systemuiex.service.GestureService

class TestGestureActivity : Activity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_gesture)
        
        val leftButton: Button = findViewById(R.id.btn_left_gesture)
        val rightButton: Button = findViewById(R.id.btn_right_gesture)
        
        leftButton.setOnClickListener {
            simulateGesture(true)
        }
        
        rightButton.setOnClickListener {
            simulateGesture(false)
        }
    }
    
    private fun simulateGesture(isLeft: Boolean) {
        Toast.makeText(this, "模拟${if (isLeft) "左侧" else "右侧"}手势", Toast.LENGTH_SHORT).show()

        GestureService.startViaPendingIntent(this, isLeft)
    }
}
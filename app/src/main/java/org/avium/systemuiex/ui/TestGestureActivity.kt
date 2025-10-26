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
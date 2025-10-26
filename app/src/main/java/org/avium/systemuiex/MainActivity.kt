package org.avium.systemuiex

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.avium.systemuiex.ui.HomeScreen
import org.avium.systemuiex.ui.selection.AppSelectionActivity
import org.avium.systemuiex.ui.theme.SystemUIEXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SystemUIEXTheme {
                HomeScreen(
                    onOpenAppSelection = {
                        startActivity(Intent(this, AppSelectionActivity::class.java))
                    }
                )
            }
        }
    }
}

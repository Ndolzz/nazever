package com.naze.nazever

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.naze.nazever.ui.showcase.DesignSystemShowcase

/**
 * Temporary TASK-004 entry point: hosts the design-system showcase only.
 * Real navigation arrives with TASK-006+.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DesignSystemShowcase() }
    }
}

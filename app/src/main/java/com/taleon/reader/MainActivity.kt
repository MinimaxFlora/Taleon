package com.taleon.reader

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.taleon.reader.ui.navigation.TaleonRoot

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaleonRoot(container = (application as TaleonApplication).container)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (ReaderHardwareKeys.onVolumeKey(keyCode)) return true
        return super.onKeyDown(keyCode, event)
    }
}

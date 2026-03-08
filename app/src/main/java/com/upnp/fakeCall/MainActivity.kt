package com.upnp.fakeCall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.upnp.fakeCall.ui.FakeCallApp
import com.upnp.fakeCall.ui.theme.FakecallTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FakecallTheme {
                FakeCallApp()
            }
        }
    }
}

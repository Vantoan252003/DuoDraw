package com.toan.codraw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.navigation.NavGraph
import com.toan.codraw.ui.theme.CodrawTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(sessionManager.getLanguageTag())
        )
        enableEdgeToEdge()
        setContent {
            CodrawTheme {
                NavGraph()
            }
        }
    }
}
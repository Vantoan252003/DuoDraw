package com.toan.codraw

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.toan.codraw.data.local.SessionManager
import com.toan.codraw.navigation.NavGraph
import com.toan.codraw.ui.theme.CodrawTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun attachBaseContext(newBase: Context) {
        val tag = newBase.getSharedPreferences("codraw_prefs", Context.MODE_PRIVATE)
            .getString("language_tag", "vi") ?: "vi"
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = newBase.resources.configuration
        config.setLocale(locale)
        val wrapped = newBase.createConfigurationContext(config)
        super.attachBaseContext(wrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CodrawTheme {
                NavGraph(sessionManager = sessionManager)
            }
        }
    }
}
package gt.pagame.app

import android.os.Bundle
import android.os.Build
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import gt.pagame.app.data.model.AppThemeMode
import gt.pagame.app.data.repository.PagameRepository
import gt.pagame.app.ui.navigation.PagameNavGraph
import gt.pagame.app.ui.theme.PagameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by PagameRepository.instance.theme.collectAsState()
            val isDark = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            SideEffect {
                // Follow the app's selected theme, including when it differs from Android.
                val barStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
            }

            PagameTheme(darkTheme = isDark) {
                PagameNavGraph()
            }
        }
    }
}

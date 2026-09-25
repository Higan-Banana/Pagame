package gt.pagame.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import gt.pagame.app.data.model.AppThemeMode
import gt.pagame.app.data.repository.PagameRepository
import gt.pagame.app.ui.navigation.PagameNavGraph
import gt.pagame.app.ui.theme.PagameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by PagameRepository.instance.theme.collectAsState()
            val isDark = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            PagameTheme(darkTheme = isDark) {
                PagameNavGraph()
            }
        }
    }
}

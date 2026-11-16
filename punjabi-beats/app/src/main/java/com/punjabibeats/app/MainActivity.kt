package com.punjabibeats.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punjabibeats.app.data.local.datastore.SettingsDataStore
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.PunjabiBeatsAppRoot
import com.punjabibeats.app.ui.theme.PunjabiBeatsTheme
import com.punjabibeats.app.ui.theme.pbThemeById
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playerController: MusicPlayerController
    @Inject lateinit var settings: SettingsDataStore

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        captureJamDeepLink(intent)
        setContent {
            val themeId by settings.themeId().collectAsStateWithLifecycle(initialValue = "punjab")
            val currentTheme = remember(themeId) { pbThemeById(themeId) }
            PunjabiBeatsTheme(theme = currentTheme) {
                PunjabiBeatsAppRoot(playerController = playerController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        captureJamDeepLink(intent)
    }

    /** punjabibeats://jam/ABC123 → queued so JamViewModel can auto-join. */
    private fun captureJamDeepLink(intent: Intent?) {
        val code = intent?.data
            ?.takeIf { it.scheme == "punjabibeats" && it.host == "jam" }
            ?.lastPathSegment
            ?.uppercase()
            ?: return
        lifecycleScope.launch { settings.savePendingJamCode(code) }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

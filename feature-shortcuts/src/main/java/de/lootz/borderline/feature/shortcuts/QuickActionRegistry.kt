package de.lootz.borderline.feature.shortcuts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import de.lootz.borderline.core.AccessibilityStateStore
import de.lootz.borderline.core.BorderlineLogger

class QuickActionRegistry(private val context: Context) {
    fun actions(): List<QuickAction> = listOf(
        QuickAction(
            id = "copy_package",
            label = "Paket kopieren",
            description = "Kopiert den aktuellen Paketnamen in die Zwischenablage."
        ) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val packageName = AccessibilityStateStore.state.value.packageName
            clipboard.setPrimaryClip(ClipData.newPlainText("borderline-package", packageName))
            Toast.makeText(context, "Paketname kopiert: $packageName", Toast.LENGTH_SHORT).show()
            "Paketname kopiert: $packageName"
        },
        QuickAction(
            id = "open_app_info",
            label = "App-Info öffnen",
            description = "Öffnet die Android App-Info der aktuell erkannten App."
        ) {
            val packageName = AccessibilityStateStore.state.value.packageName
            if (packageName == "unknown") {
                "Noch keine aktive App erkannt"
            } else {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                BorderlineLogger.i("Opened app info for $packageName")
                "App-Info geöffnet: $packageName"
            }
        }
    )
}

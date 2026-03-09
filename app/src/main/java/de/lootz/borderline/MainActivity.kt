package de.lootz.borderline

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import de.lootz.borderline.core.DeviceCompatibility
import de.lootz.borderline.core.ModuleId
import de.lootz.borderline.core.ModulePrefs
import de.lootz.borderline.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var modulePrefs: ModulePrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        modulePrefs = ModulePrefs(this)

        setupSwitches()
        setupXiaomiTweaks()
        
        binding.openAccessibilitySettingsButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        renderState()
    }

    private fun setupSwitches() {
        binding.overlaySwitch.setOnCheckedChangeListener(null)
        binding.shortcutsSwitch.setOnCheckedChangeListener(null)

        binding.overlaySwitch.isChecked = modulePrefs.isEnabled(ModuleId.OVERLAY)
        binding.shortcutsSwitch.isChecked = modulePrefs.isEnabled(ModuleId.SHORTCUTS)

        binding.overlaySwitch.setOnCheckedChangeListener { _, checked ->
            modulePrefs.setEnabled(ModuleId.OVERLAY, checked)
            if (!checked) {
                modulePrefs.setEnabled(ModuleId.SHORTCUTS, false)
                binding.shortcutsSwitch.isChecked = false
            }
            renderState()
        }
        binding.shortcutsSwitch.setOnCheckedChangeListener { _, checked ->
            modulePrefs.setEnabled(ModuleId.SHORTCUTS, checked && modulePrefs.isEnabled(ModuleId.OVERLAY))
            renderState()
        }
    }

    private fun setupXiaomiTweaks() {
        if (DeviceCompatibility.isXiaomi()) {
            binding.xiaomiCard.visibility = View.VISIBLE
            val hyperOsVersion = DeviceCompatibility.getHyperOSVersion()
            if (hyperOsVersion != null) {
                binding.xiaomiTitle.text = "HyperOS ($hyperOsVersion) Optimierung"
            }

            binding.openAutostartButton.setOnClickListener {
                DeviceCompatibility.openXiaomiAutoStartSettings(this)
            }
            binding.openOtherPermissionsButton.setOnClickListener {
                DeviceCompatibility.openXiaomiOtherPermissions(this)
            }
            binding.openBatteryOptimizationButton.setOnClickListener {
                DeviceCompatibility.openBatteryOptimizationSettings(this)
            }
        } else {
            binding.xiaomiCard.visibility = View.GONE
        }
    }

    private fun renderState() {
        val serviceEnabled = isAccessibilityServiceEnabled()
        binding.accessibilityStatus.text = if (serviceEnabled) {
            "Accessibility: aktiv"
        } else {
            "Accessibility: nicht aktiv"
        }

        val snapshot = modulePrefs.snapshot()
        binding.moduleStatus.text = snapshot.entries.joinToString(separator = "\n") { (id, enabled) ->
            "${id.displayName}: ${if (enabled) "an" else "aus"}"
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = ComponentName(this, de.lootz.borderline.feature.accessibility.BorderlineAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabledServices.split(':').any {
            ComponentName.unflattenFromString(it) == expectedComponent
        }
    }
}

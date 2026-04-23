package de.lootz.borderline

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.lootz.borderline.core.DeviceCompatibility
import de.lootz.borderline.core.ModuleId
import de.lootz.borderline.core.ModulePrefs
import de.lootz.borderline.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private enum class SetupStep {
        Welcome,
        Permissions,
        QuickStart,
        Customize
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var modulePrefs: ModulePrefs
    private lateinit var appUiPrefs: AppUiPrefs

    private var suppressSwitchCallbacks = false
    private var setupDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        modulePrefs = ModulePrefs(this)
        appUiPrefs = AppUiPrefs(this)

        setupModuleSwitches()
        setupActionButtons()
        setupXiaomiCard()
    }

    override fun onResume() {
        super.onResume()
        syncModuleSwitches()
        renderStatus()
        showSetupDialogIfNeeded()
    }

    private fun setupActionButtons() {
        binding.openAccessibilitySettingsButton.setOnClickListener { openAccessibilitySettings() }
        binding.restartSetupButton.setOnClickListener {
            appUiPrefs.resetSetup()
            showSetupDialogIfNeeded(force = true)
        }
    }

    private fun setupModuleSwitches() {
        bindOverlaySwitch(binding.overlaySwitch)
        bindShortcutsSwitch(binding.shortcutsSwitch)
        syncModuleSwitches()
    }

    private fun bindOverlaySwitch(view: CompoundButton) {
        view.setOnCheckedChangeListener { _, checked ->
            if (suppressSwitchCallbacks) return@setOnCheckedChangeListener
            modulePrefs.setEnabled(ModuleId.OVERLAY, checked)
            if (!checked) {
                modulePrefs.setEnabled(ModuleId.SHORTCUTS, false)
            }
            syncModuleSwitches()
            renderStatus()
        }
    }

    private fun bindShortcutsSwitch(view: CompoundButton) {
        view.setOnCheckedChangeListener { _, checked ->
            if (suppressSwitchCallbacks) return@setOnCheckedChangeListener
            val overlayEnabled = modulePrefs.isEnabled(ModuleId.OVERLAY)
            modulePrefs.setEnabled(ModuleId.SHORTCUTS, checked && overlayEnabled)
            syncModuleSwitches()
            renderStatus()
        }
    }

    private fun syncModuleSwitches() {
        val overlayEnabled = modulePrefs.isEnabled(ModuleId.OVERLAY)
        val shortcutsEnabled = modulePrefs.isEnabled(ModuleId.SHORTCUTS)

        suppressSwitchCallbacks = true
        binding.overlaySwitch.isChecked = overlayEnabled
        binding.shortcutsSwitch.isChecked = shortcutsEnabled
        binding.shortcutsSwitch.isEnabled = overlayEnabled
        suppressSwitchCallbacks = false
    }

    private fun renderStatus() {
        val ready = isAccessibilityServiceEnabled() && modulePrefs.isEnabled(ModuleId.OVERLAY)
        binding.headlineText.setText(
            if (ready) R.string.settings_ready_title else R.string.settings_attention_title
        )

        binding.accessibilityStatus.setText(
            if (isAccessibilityServiceEnabled()) {
                R.string.status_accessibility_active
            } else {
                R.string.status_accessibility_inactive
            }
        )

        val snapshot = modulePrefs.snapshot()
        binding.moduleStatus.text = snapshot
            .filterKeys { it != ModuleId.ACCESSIBILITY }
            .entries
            .joinToString(separator = "\n") { (id, enabled) ->
                val stateText = getString(if (enabled) R.string.status_on else R.string.status_off)
                getString(R.string.status_module_format, id.displayName, stateText)
            }
    }

    private fun setupXiaomiCard() {
        val isXiaomi = DeviceCompatibility.isXiaomi()
        binding.xiaomiCard.isVisible = isXiaomi
        if (!isXiaomi) return

        val hyperOsVersion = DeviceCompatibility.getHyperOSVersion()
        if (hyperOsVersion != null) {
            binding.xiaomiTitle.text = getString(R.string.xiaomi_hyperos_title_format, hyperOsVersion)
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
    }

    private fun showSetupDialogIfNeeded(force: Boolean = false) {
        if (!force && appUiPrefs.isSetupComplete()) return
        if (setupDialog?.isShowing == true) return

        val steps = listOf(
            SetupStep.Welcome,
            SetupStep.Permissions,
            SetupStep.QuickStart,
            SetupStep.Customize
        )
        var currentStepIndex = 0

        fun showStep() {
            val step = steps[currentStepIndex]
            val view = buildStepView(step)
            setupDialog?.dismiss()
            setupDialog = MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.setup_dialog_title, currentStepIndex + 1, steps.size))
                .setView(view)
                .setCancelable(false)
                .setNegativeButton(
                    if (currentStepIndex == 0) getString(R.string.action_skip) else getString(R.string.action_back)
                ) { _, _ ->
                    if (currentStepIndex == 0) {
                        appUiPrefs.markSetupComplete()
                    } else {
                        currentStepIndex -= 1
                        showStep()
                    }
                }
                .setPositiveButton(
                    if (currentStepIndex == steps.lastIndex) getString(R.string.action_finish) else getString(R.string.action_continue)
                ) { _, _ ->
                    if (currentStepIndex == steps.lastIndex) {
                        appUiPrefs.markSetupComplete()
                        renderStatus()
                    } else {
                        currentStepIndex += 1
                        showStep()
                    }
                }
                .show()
        }

        showStep()
    }

    private fun buildStepView(step: SetupStep): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18.dp, 8.dp, 18.dp, 0)
            when (step) {
                SetupStep.Welcome -> {
                    addHeading(getString(R.string.onboarding_welcome_title))
                    addBody(getString(R.string.onboarding_welcome_body))
                }

                SetupStep.Permissions -> {
                    addHeading(getString(R.string.onboarding_permissions_title))
                    addPermissionCheck(getString(R.string.onboarding_permission_accessibility), isAccessibilityServiceEnabled())
                    addPermissionCheck(getString(R.string.onboarding_permission_overlay), modulePrefs.isEnabled(ModuleId.OVERLAY))
                    addBody(getString(R.string.onboarding_permissions_body), topMarginDp = 12)
                }

                SetupStep.QuickStart -> {
                    addHeading(getString(R.string.onboarding_quickstart_title))
                    addBody(getString(R.string.onboarding_quickstart_body))
                }

                SetupStep.Customize -> {
                    addHeading(getString(R.string.onboarding_customize_title))
                    addBody(getString(R.string.onboarding_customize_body))
                    addView(CheckBox(context).apply {
                        text = getString(R.string.module_overlay_label)
                        isChecked = modulePrefs.isEnabled(ModuleId.OVERLAY)
                        setOnCheckedChangeListener { _, checked ->
                            modulePrefs.setEnabled(ModuleId.OVERLAY, checked)
                            if (!checked) modulePrefs.setEnabled(ModuleId.SHORTCUTS, false)
                        }
                    })
                    addView(CheckBox(context).apply {
                        text = getString(R.string.module_shortcuts_label)
                        isChecked = modulePrefs.isEnabled(ModuleId.SHORTCUTS)
                        isEnabled = modulePrefs.isEnabled(ModuleId.OVERLAY)
                        setOnCheckedChangeListener { _, checked ->
                            modulePrefs.setEnabled(
                                ModuleId.SHORTCUTS,
                                checked && modulePrefs.isEnabled(ModuleId.OVERLAY)
                            )
                        }
                    })
                }
            }
        }
    }

    private fun LinearLayout.addHeading(text: String) {
        addView(TextView(context).apply {
            this.text = text
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
        })
    }

    private fun LinearLayout.addBody(text: String, topMarginDp: Int = 10) {
        addView(TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.DKGRAY)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = topMarginDp.dp
            }
        })
    }

    private fun LinearLayout.addPermissionCheck(text: String, isChecked: Boolean) {
        addView(CheckBox(context).apply {
            this.text = text
            this.isChecked = isChecked
            isEnabled = false
        })
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private fun openAccessibilitySettings() {
        if (DeviceCompatibility.isXiaomi()) {
            showHyperOsAccessibilityGuide()
            return
        }
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun showHyperOsAccessibilityGuide() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.hyperos_accessibility_guide_title)
            .setView(R.layout.dialog_hyperos_accessibility_guide)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.hyperos_accessibility_open_now) { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .show()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = ComponentName(
            this,
            de.lootz.borderline.feature.accessibility.BorderlineAccessibilityService::class.java
        )
        val enabledServices =
            Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false
        return enabledServices.split(':').any {
            ComponentName.unflattenFromString(it) == expectedComponent
        }
    }
}

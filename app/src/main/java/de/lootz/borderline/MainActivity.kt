package de.lootz.borderline

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.lootz.borderline.core.DeviceCompatibility
import de.lootz.borderline.core.ModuleId
import de.lootz.borderline.core.ModulePrefs
import de.lootz.borderline.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private enum class SetupStep {
        WELCOME,
        ACCESSIBILITY,
        MODULES,
        OEM_HELP,
        FINISH
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var modulePrefs: ModulePrefs
    private lateinit var appUiPrefs: AppUiPrefs

    private val setupSteps: List<SetupStep> by lazy {
        buildList {
            add(SetupStep.WELCOME)
            add(SetupStep.ACCESSIBILITY)
            add(SetupStep.MODULES)
            if (DeviceCompatibility.isXiaomi()) add(SetupStep.OEM_HELP)
            add(SetupStep.FINISH)
        }
    }

    private var currentStepIndex = 0
    private var suppressSwitchCallbacks = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        modulePrefs = ModulePrefs(this)
        appUiPrefs = AppUiPrefs(this)

        setupModuleSwitches()
        setupWizardNavigation()
        setupActionButtons()
        setupXiaomiCards()
    }

    override fun onResume() {
        super.onResume()
        syncModuleSwitches()
        renderCurrentEntryPoint()
    }

    private fun renderCurrentEntryPoint() {
        if (appUiPrefs.isSetupComplete()) {
            renderSettings()
        } else {
            renderWizard()
        }
    }

    private fun setupActionButtons() {
        binding.openAccessibilitySettingsButton.setOnClickListener {
            openAccessibilitySettings()
        }
        binding.restartSetupButton.setOnClickListener {
            appUiPrefs.resetSetup()
            currentStepIndex = 0
            renderWizard()
        }
        binding.wizardPrimaryActionButton.setOnClickListener {
            openAccessibilitySettings()
        }
    }

    private fun setupWizardNavigation() {
        binding.wizardBackButton.setOnClickListener {
            if (currentStepIndex > 0) {
                currentStepIndex -= 1
                renderWizard()
            }
        }

        binding.wizardNextButton.setOnClickListener {
            val lastStep = currentStepIndex == setupSteps.lastIndex
            if (lastStep) {
                appUiPrefs.markSetupComplete()
                finish()
            } else {
                currentStepIndex += 1
                renderWizard()
            }
        }
    }

    private fun setupModuleSwitches() {
        bindOverlaySwitch(binding.wizardOverlaySwitch)
        bindOverlaySwitch(binding.settingsOverlaySwitch)
        bindShortcutsSwitch(binding.wizardShortcutsSwitch)
        bindShortcutsSwitch(binding.settingsShortcutsSwitch)
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
            renderCurrentEntryPoint()
        }
    }

    private fun bindShortcutsSwitch(view: CompoundButton) {
        view.setOnCheckedChangeListener { _, checked ->
            if (suppressSwitchCallbacks) return@setOnCheckedChangeListener
            val overlayEnabled = modulePrefs.isEnabled(ModuleId.OVERLAY)
            modulePrefs.setEnabled(ModuleId.SHORTCUTS, checked && overlayEnabled)
            syncModuleSwitches()
            renderCurrentEntryPoint()
        }
    }

    private fun syncModuleSwitches() {
        val overlayEnabled = modulePrefs.isEnabled(ModuleId.OVERLAY)
        val shortcutsEnabled = modulePrefs.isEnabled(ModuleId.SHORTCUTS)

        suppressSwitchCallbacks = true
        binding.wizardOverlaySwitch.isChecked = overlayEnabled
        binding.settingsOverlaySwitch.isChecked = overlayEnabled
        binding.wizardShortcutsSwitch.isChecked = shortcutsEnabled
        binding.settingsShortcutsSwitch.isChecked = shortcutsEnabled
        binding.wizardShortcutsSwitch.isEnabled = overlayEnabled
        binding.settingsShortcutsSwitch.isEnabled = overlayEnabled
        suppressSwitchCallbacks = false
    }

    private fun setupXiaomiCards() {
        val isXiaomi = DeviceCompatibility.isXiaomi()
        binding.wizardXiaomiCard.isVisible = false
        binding.settingsXiaomiCard.isVisible = false

        if (!isXiaomi) return

        val hyperOsVersion = DeviceCompatibility.getHyperOSVersion()
        if (hyperOsVersion != null) {
            val title = getString(R.string.xiaomi_hyperos_title_format, hyperOsVersion)
            binding.wizardXiaomiTitle.text = title
            binding.settingsXiaomiTitle.text = title
        }

        val openAutostart = View.OnClickListener {
            DeviceCompatibility.openXiaomiAutoStartSettings(this)
        }
        val openPermissions = View.OnClickListener {
            DeviceCompatibility.openXiaomiOtherPermissions(this)
        }
        val openBattery = View.OnClickListener {
            DeviceCompatibility.openBatteryOptimizationSettings(this)
        }

        binding.wizardOpenAutostartButton.setOnClickListener(openAutostart)
        binding.settingsOpenAutostartButton.setOnClickListener(openAutostart)
        binding.wizardOpenOtherPermissionsButton.setOnClickListener(openPermissions)
        binding.settingsOpenOtherPermissionsButton.setOnClickListener(openPermissions)
        binding.wizardOpenBatteryOptimizationButton.setOnClickListener(openBattery)
        binding.settingsOpenBatteryOptimizationButton.setOnClickListener(openBattery)
    }

    private fun renderWizard() {
        binding.setupContainer.isVisible = true
        binding.settingsContainer.isVisible = false

        val serviceEnabled = isAccessibilityServiceEnabled()
        val step = setupSteps[currentStepIndex]

        binding.wizardStepCounter.text = getString(
            R.string.wizard_step_counter,
            currentStepIndex + 1,
            setupSteps.size
        )
        binding.wizardBackButton.isVisible = currentStepIndex > 0
        binding.wizardModulesCard.isVisible = step == SetupStep.MODULES
        binding.wizardXiaomiCard.isVisible = step == SetupStep.OEM_HELP && DeviceCompatibility.isXiaomi()
        binding.wizardSummaryCard.isVisible = step == SetupStep.FINISH
        binding.wizardPrimaryActionButton.isVisible = false
        binding.wizardStepHint.isVisible = true

        when (step) {
            SetupStep.WELCOME -> {
                binding.wizardStepTitle.setText(R.string.wizard_welcome_title)
                binding.wizardStepBody.setText(R.string.wizard_welcome_body)
                binding.wizardStepHint.text = getString(R.string.settings_intro)
                binding.wizardNextButton.setText(R.string.action_continue)
                binding.wizardNextButton.isEnabled = true
            }

            SetupStep.ACCESSIBILITY -> {
                binding.wizardStepTitle.setText(R.string.wizard_accessibility_title)
                binding.wizardStepBody.setText(R.string.wizard_accessibility_body)
                binding.wizardStepHint.text = getString(
                    if (serviceEnabled) {
                        R.string.wizard_accessibility_ready_hint
                    } else {
                        R.string.wizard_accessibility_missing_hint
                    }
                )
                binding.wizardPrimaryActionButton.isVisible = true
                binding.wizardNextButton.setText(R.string.action_continue)
                binding.wizardNextButton.isEnabled = true
            }

            SetupStep.MODULES -> {
                binding.wizardStepTitle.setText(R.string.wizard_modules_title)
                binding.wizardStepBody.setText(R.string.wizard_modules_body)
                binding.wizardStepHint.setText(R.string.wizard_modules_hint)
                binding.wizardNextButton.setText(R.string.action_continue)
                binding.wizardNextButton.isEnabled = true
            }

            SetupStep.OEM_HELP -> {
                binding.wizardStepTitle.setText(R.string.wizard_xiaomi_title)
                binding.wizardStepBody.setText(R.string.wizard_xiaomi_body)
                binding.wizardStepHint.setText(R.string.wizard_xiaomi_hint)
                binding.wizardNextButton.setText(R.string.action_continue)
                binding.wizardNextButton.isEnabled = true
            }

            SetupStep.FINISH -> {
                binding.wizardStepTitle.setText(
                    if (isReadyForDailyUse()) {
                        R.string.wizard_finish_ready_title
                    } else {
                        R.string.wizard_finish_attention_title
                    }
                )
                binding.wizardStepBody.setText(
                    if (isReadyForDailyUse()) {
                        R.string.wizard_finish_ready_body
                    } else {
                        R.string.wizard_finish_attention_body
                    }
                )
                binding.wizardStepHint.setText(R.string.wizard_finish_hint)
                binding.wizardNextButton.setText(R.string.action_finish)
                binding.wizardNextButton.isEnabled = true
                renderStatusTexts(binding.wizardAccessibilityStatus, binding.wizardModuleStatus)
                binding.wizardPrimaryActionButton.isVisible = !serviceEnabled
            }
        }
    }

    private fun renderSettings() {
        binding.setupContainer.isVisible = false
        binding.settingsContainer.isVisible = true

        val serviceEnabled = isAccessibilityServiceEnabled()
        val ready = isReadyForDailyUse()

        binding.settingsTitle.setText(
            if (ready) R.string.settings_ready_title else R.string.settings_attention_title
        )
        binding.settingsBody.setText(
            if (ready) R.string.settings_ready_body else R.string.settings_attention_body
        )
        binding.settingsXiaomiCard.isVisible = DeviceCompatibility.isXiaomi()
        binding.openAccessibilitySettingsButton.text = getString(
            if (serviceEnabled) {
                R.string.open_accessibility_settings
            } else {
                R.string.open_accessibility_settings
            }
        )

        renderStatusTexts(binding.accessibilityStatus, binding.moduleStatus)
    }

    private fun renderStatusTexts(accessibilityView: android.widget.TextView, modulesView: android.widget.TextView) {
        val serviceEnabled = isAccessibilityServiceEnabled()
        accessibilityView.setText(
            if (serviceEnabled) R.string.status_accessibility_active else R.string.status_accessibility_inactive
        )

        val snapshot = modulePrefs.snapshot()
        modulesView.text = snapshot
            .filterKeys { it != ModuleId.ACCESSIBILITY }
            .entries
            .joinToString(separator = "\n") { (id, enabled) ->
                val stateText = getString(if (enabled) R.string.status_on else R.string.status_off)
                getString(R.string.status_module_format, id.displayName, stateText)
            }
    }

    private fun isReadyForDailyUse(): Boolean {
        return isAccessibilityServiceEnabled() && modulePrefs.isEnabled(ModuleId.OVERLAY)
    }

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

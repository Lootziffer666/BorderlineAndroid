package de.lootz.borderline

import androidx.appcompat.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import de.lootz.borderline.core.DeviceCompatibility
import de.lootz.borderline.core.ModuleId
import de.lootz.borderline.core.ModulePrefs
import de.lootz.borderline.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private sealed class SetupStep {
        data object Welcome : SetupStep()
        data object Permissions : SetupStep()
        data object QuickStart : SetupStep()
        data object Customize : SetupStep()
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
        binding.backButton.setOnClickListener { finish() }
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
        binding.shortcutsStateText.text = getString(
            if (modulePrefs.isEnabled(ModuleId.SHORTCUTS)) R.string.status_on_cap else R.string.status_off_cap
        )
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
        var currentStep = 0

        fun showStep(step: SetupStep) {
            val view = buildStepView(step)
            setupDialog?.dismiss()
            setupDialog = AlertDialog.Builder(this)
                .setView(view)
                .setNegativeButton(
                    if (currentStep == 0) getString(R.string.action_skip) else getString(R.string.action_back)
                ) { _, _ ->
                    if (currentStep > 0) {
                        currentStep--
                        showStep(steps[currentStep])
                    } else {
                        appUiPrefs.markSetupComplete()
                        renderStatus()
                    }
                }
                .setPositiveButton(
                    if (currentStep == steps.lastIndex) getString(R.string.action_finish) else getString(R.string.action_next)
                ) { _, _ ->
                    if (currentStep < steps.lastIndex) {
                        currentStep++
                        showStep(steps[currentStep])
                    } else {
                        appUiPrefs.markSetupComplete()
                        syncModuleSwitches()
                        renderStatus()
                    }
                }
                .setCancelable(false)
                .show()
        }

        showStep(steps[0])
    }

    private fun buildStepView(step: SetupStep): View {
        return when (step) {
            SetupStep.Welcome -> buildWelcomeView()
            SetupStep.Permissions -> buildPermissionsView()
            SetupStep.QuickStart -> buildQuickStartView()
            SetupStep.Customize -> buildCustomizeView()
        }
    }

    private fun buildWelcomeView(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                text = getString(R.string.snippet_welcome_title)
                textSize = 24f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(16.dp, 16.dp, 16.dp, 8.dp) }
            })
            addView(TextView(context).apply {
                text = getString(R.string.snippet_welcome_body)
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(16.dp, 8.dp, 16.dp, 16.dp) }
            })
        }
    }

    private fun buildPermissionsView(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp, 16.dp, 16.dp, 0)
            addView(TextView(context).apply {
                text = getString(R.string.snippet_permissions_title)
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
            })
            addPermissionCheck(getString(R.string.snippet_permission_accessibility), isAccessibilityServiceEnabled())
            addPermissionCheck(getString(R.string.snippet_permission_clipboard), true)
            addPermissionCheck(getString(R.string.snippet_permission_overlay), modulePrefs.isEnabled(ModuleId.OVERLAY))
            addView(TextView(context).apply {
                text = getString(R.string.snippet_permissions_footer)
                textSize = 12f
                setTextColor(Color.GRAY)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 16.dp, 0, 0) }
            })
        }
    }

    private fun buildQuickStartView(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp, 16.dp, 16.dp, 0)
            addView(TextView(context).apply {
                text = getString(R.string.snippet_quickstart_title)
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
            })
            addView(TextView(context).apply {
                text = getString(R.string.snippet_quickstart_body)
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 16.dp, 0, 0) }
            })
        }
    }

    private fun buildCustomizeView(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp, 16.dp, 16.dp, 0)
            addView(TextView(context).apply {
                text = getString(R.string.snippet_customize_title)
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
            })
            addView(CheckBox(context).apply {
                text = getString(R.string.snippet_customize_adaptive_actions)
                isChecked = true
            })
            addView(CheckBox(context).apply {
                text = getString(R.string.snippet_customize_clipboard_autograb)
                isChecked = true
            })
            addView(CheckBox(context).apply {
                text = getString(R.string.snippet_customize_haptics)
                isChecked = true
            })
        }
    }

    private fun LinearLayout.addPermissionCheck(text: String, isChecked: Boolean) {
        addView(CheckBox(context).apply {
            this.text = "✓ $text"
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
        androidx.appcompat.app.AlertDialog.Builder(this)
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

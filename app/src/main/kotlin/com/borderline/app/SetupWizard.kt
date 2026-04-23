package com.borderline.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

class SetupWizard(private val context: Context) {

    private val prefs = context.getSharedPreferences("borderline_setup", Context.MODE_PRIVATE)

    fun hasCompletedSetup(): Boolean {
        return prefs.getBoolean("setup_completed", false)
    }

    fun markSetupComplete() {
        prefs.edit().putBoolean("setup_completed", true).apply()
    }

    fun showSetupIfNeeded(activity: Activity, onComplete: () -> Unit) {
        if (hasCompletedSetup()) {
            onComplete()
            return
        }
        showSetupDialog(activity, onComplete)
    }

    private fun showSetupDialog(activity: Activity, onComplete: () -> Unit) {
        val steps = listOf(
            SetupStep.Welcome,
            SetupStep.Permissions,
            SetupStep.QuickStart,
            SetupStep.Customize
        )

        fun showStep(index: Int) {
            val view = buildStepView(activity, steps[index])

            AlertDialog.Builder(activity)
                .setView(view)
                .setNegativeButton(if (index == 0) "Skip" else "Back") { _, _ ->
                    if (index == 0) {
                        markSetupComplete()
                        onComplete()
                    } else {
                        showStep(index - 1)
                    }
                }
                .setPositiveButton(if (index == steps.lastIndex) "Finish" else "Next") { _, _ ->
                    if (index == steps.lastIndex) {
                        markSetupComplete()
                        onComplete()
                    } else {
                        showStep(index + 1)
                    }
                }
                .setCancelable(false)
                .show()
        }

        showStep(0)
    }

    private fun buildStepView(activity: Activity, step: SetupStep): View {
        return when (step) {
            SetupStep.Welcome -> buildWelcomeView(activity)
            SetupStep.Permissions -> buildPermissionsView(activity)
            SetupStep.QuickStart -> buildQuickStartView(activity)
            SetupStep.Customize -> buildCustomizeView(activity)
        }
    }

    private fun buildWelcomeView(activity: Activity): View {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(20), activity.dp(20), activity.dp(20), activity.dp(12))

            addView(TextView(activity).apply {
                text = "Welcome to Borderline"
                textSize = 24f
                setTypeface(typeface, Typeface.BOLD)
            })

            addView(TextView(activity).apply {
                text = "Overlay für schnellere Textbearbeitung, Clipboard-Verwaltung und Smart Actions."
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setPadding(0, activity.dp(12), 0, 0)
            })
        }
    }

    private fun buildPermissionsView(activity: Activity): View {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(20), activity.dp(20), activity.dp(20), activity.dp(12))

            addView(TextView(activity).apply {
                text = "Erforderliche Berechtigungen"
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
            })

            addView(CheckBox(activity).apply {
                text = "Accessibility Service"
                isChecked = true
                isEnabled = false
            })

            addView(CheckBox(activity).apply {
                text = "Clipboard-Zugriff"
                isChecked = true
                isEnabled = false
            })

            addView(CheckBox(activity).apply {
                text = "Overlay-Fenster"
                isChecked = true
                isEnabled = false
            })

            addView(TextView(activity).apply {
                text = "Diese Einstellungen müssen einmalig aktiviert werden."
                textSize = 12f
                setTextColor(Color.GRAY)
                setPadding(0, activity.dp(12), 0, 0)
            })
        }
    }

    private fun buildQuickStartView(activity: Activity): View {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(20), activity.dp(20), activity.dp(20), activity.dp(12))

            addView(TextView(activity).apply {
                text = "Quick Start"
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
            })

            addView(TextView(activity).apply {
                text = "Wische von der Seite, um die Menüs zu öffnen. Langes Drücken öffnet den Bearbeitungsmodus."
                textSize = 14f
                setPadding(0, activity.dp(12), 0, 0)
            })
        }
    }

    private fun buildCustomizeView(activity: Activity): View {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(20), activity.dp(20), activity.dp(20), activity.dp(12))

            addView(TextView(activity).apply {
                text = "Anpassen"
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
            })

            addView(CheckBox(activity).apply {
                text = "Adaptive Quick Actions aktivieren"
                isChecked = true
            })

            addView(CheckBox(activity).apply {
                text = "Clipboard-Auto-Grab"
                isChecked = true
            })

            addView(CheckBox(activity).apply {
                text = "Haptic Feedback"
                isChecked = true
            })
        }
    }

    private fun Context.dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }
}

sealed class SetupStep {
    data object Welcome : SetupStep()
    data object Permissions : SetupStep()
    data object QuickStart : SetupStep()
    data object Customize : SetupStep()
}

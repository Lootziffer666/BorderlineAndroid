package de.lootz.borderline

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.borderline.app.SetupWizard

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SetupWizard(this).showSetupIfNeeded(this) {}
    }
}

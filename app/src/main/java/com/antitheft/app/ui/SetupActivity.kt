package com.antitheft.app.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.antitheft.app.R
import com.antitheft.app.receivers.DeviceAdminReceiver
import com.antitheft.app.services.StealthService
import com.antitheft.app.utils.Constants
import android.app.admin.DevicePolicyManager

class SetupActivity : AppCompatActivity() {
    
    private lateinit var emergencyNumberInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var emailPasswordInput: EditText
    private lateinit var setupButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    
    private val devicePolicyManager by lazy { getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    private val deviceAdminComponent by lazy { ComponentName(this, DeviceAdminReceiver::class.java) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        
        emergencyNumberInput = findViewById(R.id.emergencyNumberInput)
        emailInput = findViewById(R.id.emailInput)
        emailPasswordInput = findViewById(R.id.emailPasswordInput)
        setupButton = findViewById(R.id.setupButton)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        
        setupButton.setOnClickListener { performSetup() }
        
        val prefs = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
        if (!prefs.getBoolean(Constants.PREF_FIRST_RUN, true)) finishSetupAndHide()
    }
    
    private fun performSetup() {
        val emergencyNumber = emergencyNumberInput.text.toString().trim()
        if (emergencyNumber.isEmpty()) {
            Toast.makeText(this, "Emergency number required", Toast.LENGTH_SHORT).show()
            return
        }
        
        setupButton.isEnabled = false
        progressBar.visibility = android.view.View.VISIBLE
        statusText.text = "Configuring system..."
        
        val prefs = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
        prefs.edit().apply {
            putString(Constants.PREF_EMERGENCY_NUMBER, emergencyNumber)
            putString(Constants.PREF_EMAIL, emailInput.text.toString().trim())
            putString(Constants.PREF_EMAIL_PASSWORD, emailPasswordInput.text.toString().trim())
            putStringSet(Constants.PREF_AUTH_NUMBERS, setOf(emergencyNumber))
            putBoolean(Constants.PREF_FIRST_RUN, false)
            apply()
        }
        
        if (!devicePolicyManager.isAdminActive(deviceAdminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for device security")
            startActivityForResult(intent, 1001)
        } else {
            finishSetupAndHide()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) finishSetupAndHide()
    }
    
    private fun finishSetupAndHide() {
        statusText.text = "Setup complete"
        val serviceIntent = Intent(this, StealthService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent)
        else startService(serviceIntent)
        
        packageManager.setComponentEnabledSetting(
            ComponentName(this, SetupActivity::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        
        startActivity(Intent(this, DecoyActivity::class.java))
        finish()
    }
}

package com.antitheft.app.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.antitheft.app.R
import kotlinx.coroutines.*

class DecoyActivity : AppCompatActivity() {
    
    private lateinit var batteryLevelText: TextView
    private lateinit var optimizeButton: Button
    private lateinit var storageCleanButton: Button
    private lateinit var securityStatusText: TextView
    private lateinit var progressBar: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decoy)
        
        batteryLevelText = findViewById(R.id.batteryLevelText)
        optimizeButton = findViewById(R.id.optimizeButton)
        storageCleanButton = findViewById(R.id.storageCleanButton)
        securityStatusText = findViewById(R.id.securityStatusText)
        progressBar = findViewById(R.id.progressBar)
        
        securityStatusText.text = "Device Protected"
        
        val batteryManager = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
        val level = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        batteryLevelText.text = "Battery Level: $level%"
        
        optimizeButton.setOnClickListener {
            progressBar.visibility = android.view.View.VISIBLE
            optimizeButton.isEnabled = false
            CoroutineScope(Dispatchers.Main).launch {
                delay(2000)
                progressBar.visibility = android.view.View.GONE
                optimizeButton.isEnabled = true
                Toast.makeText(this@DecoyActivity, "Optimization complete!", Toast.LENGTH_SHORT).show()
            }
        }
        
        storageCleanButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                delay(3000)
                Toast.makeText(this@DecoyActivity, "Cleaned 156MB of junk files", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

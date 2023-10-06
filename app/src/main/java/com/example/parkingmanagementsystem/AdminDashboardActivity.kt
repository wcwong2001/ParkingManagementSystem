package com.example.parkingmanagementsystem

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.widget.Toolbar

class AdminDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val manageUserProfileButton: Button = findViewById(R.id.manageUserProfileButton)
        val generateReportButton: Button = findViewById(R.id.generateReportButton)

        manageUserProfileButton.setOnClickListener {
            val intent = Intent(this@AdminDashboardActivity, AdminManageUserProfileActivity::class.java)
            startActivity(intent)
        }
        generateReportButton.setOnClickListener {
            val intent = Intent(this@AdminDashboardActivity, AdminGenerateReportActivity::class.java)
            startActivity(intent)
        }
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        // Set the Toolbar as the app bar for the activity
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Admin Dashboard"
        // Enable the "Up" button (back button)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Define the behavior when the "Up" button is pressed
        toolbar.setNavigationOnClickListener {
            onBackPressed() // This will simulate a back button press
        }
    }
}
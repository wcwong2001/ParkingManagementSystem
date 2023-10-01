package com.example.parkingmanagementsystem

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class AdminDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val manageUserProfileButton: Button = findViewById(R.id.manageUserProfileButton)
        val generateReportButton: Button = findViewById(R.id.generateReportButton)

        manageUserProfileButton.setOnClickListener(){
            val intent = Intent(this@AdminDashboardActivity, AdminManageUserProfileActivity::class.java)
            startActivity(intent)
        }
        generateReportButton.setOnClickListener(){
            val intent = Intent(this@AdminDashboardActivity, AdminGenerateReportActivity::class.java)
            startActivity(intent)
        }

    }
}
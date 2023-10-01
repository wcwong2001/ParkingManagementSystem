package com.example.parkingmanagementsystem

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Calendar

class AdminGenerateReportActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var monthSpinner: Spinner
    private lateinit var mostUsedParkingLotText: TextView
    private lateinit var mostUsedParkingLotVal: TextView
    private lateinit var parkingIncomeByDayText: TextView
    private lateinit var parkingIncomeByDayVal: TextView
    private lateinit var peakParkingHourText: TextView
    private lateinit var peakParkingHourVal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_generate_report)

        database = FirebaseDatabase.getInstance().reference
        monthSpinner = findViewById(R.id.monthSpinner)
        parkingIncomeByDayText = findViewById(R.id.parkingIncomeByDayText)
        parkingIncomeByDayVal = findViewById(R.id.parkingIncomeByDay)


        val months = resources.getStringArray(R.array.months)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = adapter

        // Calculate and display metrics when a month is selected
        monthSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                val selectedMonth = position // Month index (0-based)
                calculateParkingIncomeByDay(selectedMonth)

            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        })
    }

    private fun calculateParkingIncomeByDay(selectedMonth: Int) {
        val parkingIncomeByDay = mutableMapOf<String, Double>()

        // Reference to the parking assignments
        val assignmentsRef = database.child("users")

        assignmentsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    val parkingAssignmentsSnapshot = userSnapshot.child("parkingAssignments")
                    for (assignmentSnapshot in parkingAssignmentsSnapshot.children) {
                        val parkingHourCounts = mutableMapOf<String, Int>()
                        try {
                            val checkInTime =
                                assignmentSnapshot.child("checkinTime").getValue(String::class.java)
                            checkInTime?.let { Log.d("checkInTime", it) }
                            val totalFeeStr =
                                assignmentSnapshot.child("totalFee").getValue(String::class.java)
                            totalFeeStr?.let { Log.d("totalFee", it) }
                            // Parse the check-in time and calculate the parking income for this assignment
                            val checkInDate =
                                SimpleDateFormat("dd/MM/yyyy HH:mm").parse(checkInTime)
                            val calendar = Calendar.getInstance()
                            calendar.time = checkInDate

                            val month = calendar.get(Calendar.MONTH) // 0-based month

                            if (month == selectedMonth) {
                                val day = calendar.get(Calendar.DAY_OF_MONTH)
                                val totalFee = totalFeeStr?.toDouble() ?: 0.0


                                val dayMonth = "$day/$month"
                                parkingIncomeByDay[dayMonth] = (parkingIncomeByDay[dayMonth] ?: 0.0) + totalFee
                            }
                        } catch (e: NullPointerException) {
                            continue
                        }

                    }
                }

                // Now you have parking income by day for the selected month
                // Display or use the result as needed
                val parkingIncomeText = StringBuilder()
                for ((dayMonth, income) in parkingIncomeByDay) {
                    parkingIncomeText.append("Day/Month: $dayMonth, Income: RM $income\n")
                }
                parkingIncomeByDayText.text = "Parking Income by Day:"
                parkingIncomeByDayVal.text = parkingIncomeText.toString()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }

}

package com.example.parkingmanagementsystem

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Calendar

class ReportActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var monthSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        database = FirebaseDatabase.getInstance().reference
        monthSpinner = findViewById(R.id.monthSpinner)

        val months = resources.getStringArray(R.array.months)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = adapter

        // Calculate and display metrics when a month is selected
        monthSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                val selectedMonth = position // Month index (0-based)

                findMostUsedParkingLot(selectedMonth)
                calculateParkingIncomeByDay(selectedMonth)
                findPeakParkingHour(selectedMonth)
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        })
    }

    private fun findMostUsedParkingLot(selectedMonth: Int) {
        val parkingLotCounts = mutableMapOf<String, Int>()

        // Reference to the parking assignments
        val assignmentsRef = database.child("users").child("parkingAssignments")

        assignmentsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    for (assignmentSnapshot in userSnapshot.children) {
                        val checkInTime = assignmentSnapshot.child("checkinTime").getValue(String::class.java)
                        val totalFeeStr = assignmentSnapshot.child("totalFee").getValue(String::class.java)

                        // Parse the check-in time and calculate the parking income for this assignment
                        val checkInDate = SimpleDateFormat("dd/MM/yyyy HH:mm").parse(checkInTime)
                        val calendar = Calendar.getInstance()
                        calendar.time = checkInDate

                        val month = calendar.get(Calendar.MONTH) // 0-based month
                        val day = calendar.get(Calendar.DAY_OF_MONTH)

                        if (month == selectedMonth) {
                            val parkingLot = assignmentSnapshot.child("parkingLot").getValue(String::class.java)
                            // Increment the count for this parking lot
                            parkingLot?.let {
                                parkingLotCounts[it] = (parkingLotCounts[it] ?: 0) + 1
                            }
                        }
                    }
                }

                // Find the parking lot with the highest count
                var mostUsedParkingLot = ""
                var maxCount = 0

                for ((lot, count) in parkingLotCounts) {
                    if (count > maxCount) {
                        maxCount = count
                        mostUsedParkingLot = lot
                    }
                }

                // Now you have the most used parking lot
                // Display or use the result as needed
                println("Most Used Parking Lot: $mostUsedParkingLot")
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun calculateParkingIncomeByDay(selectedMonth: Int) {
        val parkingIncomeByDay = mutableMapOf<String, Double>()

        // Reference to the parking assignments
        val assignmentsRef = database.child("users").child("parkingAssignments")

        assignmentsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    for (assignmentSnapshot in userSnapshot.children) {
                        val checkInTime = assignmentSnapshot.child("checkinTime").getValue(String::class.java)
                        val totalFeeStr = assignmentSnapshot.child("totalFee").getValue(String::class.java)

                        // Parse the check-in time and calculate the parking income for this assignment
                        val checkInDate = SimpleDateFormat("dd/MM/yyyy HH:mm").parse(checkInTime)
                        val calendar = Calendar.getInstance()
                        calendar.time = checkInDate

                        val month = calendar.get(Calendar.MONTH) // 0-based month

                        if (month == selectedMonth) {
                            val day = calendar.get(Calendar.DAY_OF_MONTH)
                            val totalFee = totalFeeStr?.toDouble() ?: 0.0

                            parkingIncomeByDay["$day/$month"] = (parkingIncomeByDay["$day/$month"] ?: 0.0) + totalFee
                        }
                    }
                }

                // Now you have parking income by day for the selected month
                // Display or use the result as needed
                for ((dayMonth, income) in parkingIncomeByDay) {
                    println("Day/Month: $dayMonth, Income: RM $income")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun findPeakParkingHour(selectedMonth: Int) {
        val parkingHourCounts = mutableMapOf<String, Int>()

        // Reference to the parking assignments
        val assignmentsRef = database.child("users").child("parkingAssignments")

        assignmentsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    for (assignmentSnapshot in userSnapshot.children) {
                        val checkInTime = assignmentSnapshot.child("checkinTime").getValue(String::class.java)

                        // Parse the check-in time to get the hour
                        val checkInDate = SimpleDateFormat("dd/MM/yyyy HH:mm").parse(checkInTime)
                        val calendar = Calendar.getInstance()
                        calendar.time = checkInDate

                        val month = calendar.get(Calendar.MONTH) // 0-based month

                        if (month == selectedMonth) {
                            val hour = calendar.get(Calendar.HOUR_OF_DAY)

                            // Increment the count for this hour
                            parkingHourCounts["$hour:00 - ${hour + 1}:00"] = (parkingHourCounts["$hour:00 - ${hour + 1}:00"] ?: 0) + 1
                        }
                    }
                }

                // Find the peak parking hour
                var peakParkingHour = ""
                var maxCount = 0

                for ((hourRange, count) in parkingHourCounts) {
                    if (count > maxCount) {
                        maxCount = count
                        peakParkingHour = hourRange
                    }
                }

                // Now you have the peak parking hour
                // Display or use the result as needed
                println("Peak Parking Hour: $peakParkingHour")
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }
}

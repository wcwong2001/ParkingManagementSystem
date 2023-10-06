package com.example.parkingmanagementsystem

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.widget.Toolbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ParkingAssignmentList : AppCompatActivity() {

    private lateinit var accountButton: Button
    private lateinit var assignmentsListView: ListView
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parking_assignment_list)

        accountButton = findViewById(R.id.btnUserProfile)
        accountButton.setOnClickListener{
            val intent = Intent(this@ParkingAssignmentList, AdminManageUserProfileActivity::class.java)
            startActivity(intent)
        }

        assignmentsListView = findViewById(R.id.parkingAssignmentsListView)

        // Retrieve the username passed from the previous activity

        // Initialize Firebase Realtime Database
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")
        username = intent.getStringExtra("username").toString()

        // Retrieve parking assignments for the user
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val parkingAssignments = ArrayList<String>()

                for (userSnapshot in snapshot.children) {
                    val userUsername = userSnapshot.child("username").getValue(String::class.java)
                    if (userUsername == username) {
                        val parkingAssignmentsRef = userSnapshot.child("parkingAssignments")

                        for (assignmentSnapshot in parkingAssignmentsRef.children) {
                            val parkingLot = assignmentSnapshot.child("parkingLot").getValue(String::class.java)
                            val parkingSlotNumber =
                                assignmentSnapshot.child("parkingSlotNumber").getValue(Int::class.java)
                            val checkinTime = assignmentSnapshot.child("checkinTime").getValue(String::class.java)
                            val checkoutTime = assignmentSnapshot.child("checkoutTime").getValue(String::class.java)

                            // Retrieve the total fee from the assignment
                            var totalFee = assignmentSnapshot.child("totalFee").getValue(String::class.java)
                            if(totalFee.isNullOrEmpty()){
                                totalFee = ""
                            }

                            val assignmentText = "Parking Lot: $parkingLot, Slot: $parkingSlotNumber\n" +
                                    "Check-in Time: $checkinTime\n" +
                                    "Check-out Time: $checkoutTime\n" +
                                    "Total Fee: RM $totalFee"

                            parkingAssignments.add(assignmentText)
                        }
                        break
                    }
                }

                // Create an ArrayAdapter to display parking assignments in the ListView
                val adapter = ArrayAdapter(
                    this@ParkingAssignmentList,
                    android.R.layout.simple_list_item_1,
                    parkingAssignments
                )

                assignmentsListView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error if needed
            }
        })
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        // Set the Toolbar as the app bar for the activity
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Parking History"
        // Enable the "Up" button (back button)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Define the behavior when the "Up" button is pressed
        toolbar.setNavigationOnClickListener {
            onBackPressed() // This will simulate a back button press
        }
    }

}

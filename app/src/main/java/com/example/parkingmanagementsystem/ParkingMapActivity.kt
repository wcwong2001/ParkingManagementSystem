package com.example.parkingmanagementsystem

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone

class ParkingMapActivity : AppCompatActivity() {
    private lateinit var parkingButtonA: Button
    private lateinit var parkingButtonB: Button
    private lateinit var parkingButtonC: Button
    private lateinit var accountButton: Button
    private lateinit var parkingHistoryButton: Button
    private lateinit var assignParkingButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parking_map)

        val database = FirebaseDatabase.getInstance()
        val username = intent.getStringExtra("username")
        val userRef = FirebaseDatabase.getInstance().getReference("users")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(usersSnapshot: DataSnapshot) {
                for (userSnapshot in usersSnapshot.children) {
                    val userUsername = userSnapshot.child("username").getValue(String::class.java)
                    if (userUsername == username) {
                        val membershipStatus = userSnapshot.child("membership").getValue(String::class.java)
                        // Check the membership status
                        if (membershipStatus == "Expired") {
                            // Disable the assignParkingButton and show a toast
                            assignParkingButton.isEnabled = false
                            Toast.makeText(this@ParkingMapActivity, "Your membership is expired. Please renew it in the ACCOUNT section."
                                , Toast.LENGTH_LONG).show()
                        } else {
                            // Enable the assignParkingButton
                            assignParkingButton.isEnabled = true
                        }

                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Handle database error if needed
            }
        })


        accountButton = findViewById(R.id.accountButton)
        accountButton.setOnClickListener{
            val username = intent.getStringExtra("username")
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }

        assignParkingButton = findViewById(R.id.assignParkingButton)
        assignParkingButton.setOnClickListener{
            val username = intent.getStringExtra("username")
            val intent = Intent(this, AssignParkingActivity::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }


        parkingHistoryButton = findViewById(R.id.parkingHistoryButton)
        parkingHistoryButton.setOnClickListener{
            val username = intent.getStringExtra("username")
            val intent = Intent(this, ParkingAssignmentList::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }

        val assignParkingButton = findViewById<Button>(R.id.parkingHistoryButton)
        assignParkingButton.setOnClickListener {
            val username = intent.getStringExtra("username")
            val intent = Intent(this, ParkingAssignmentList::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }

        val parkingLotRef = database.getReference("lot")

        parkingButtonA = findViewById(R.id.parkingButtonA)
        parkingButtonB = findViewById(R.id.parkingButtonB)
        parkingButtonC = findViewById(R.id.parkingButtonC)

        val parkingLotARef = parkingLotRef.child("A")
        val parkingLotBRef = parkingLotRef.child("B")
        val parkingLotCRef = parkingLotRef.child("C")

        parkingLotARef.child("availability").addValueEventListener(createParkingLotListener(parkingButtonA,"BLOCK K"))
        parkingLotBRef.child("availability").addValueEventListener(createParkingLotListener(parkingButtonB,"CITC"))
        parkingLotCRef.child("availability").addValueEventListener(createParkingLotListener(parkingButtonC,"EAST CAMPUS"))

        parkingButtonA.setOnClickListener {
            navigateToParkingLotMap("A")
        }

        parkingButtonB.setOnClickListener {
            navigateToParkingLotMap("B")
        }

        parkingButtonC.setOnClickListener {
            navigateToParkingLotMap("C")
        }
    }



    private fun createParkingLotListener(button: Button, text:String): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val availability = snapshot.getValue(String::class.java)
                button.text = "Parking Lot ${button.tag} $text \n $availability/40"
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error if needed
            }
        }
    }

    private fun navigateToParkingLotMap(parkingLot: String) {
        val intent = Intent(this@ParkingMapActivity, ParkingLotMapActivity::class.java)
        val username = intent.getStringExtra("username")
        intent.putExtra("username", username)
        startActivity(intent)
    }

}

data class ParkingLot(
    val availability: String = ""
)
package com.example.parkingmanagementsystem

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.widget.Button
import android.widget.TextView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserProfileActivity : AppCompatActivity() {

    private lateinit var logoutButton: Button
    private lateinit var paymentButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        val username = intent.getStringExtra("username")

        paymentButton = findViewById<Button>(R.id.paymentButton)
        paymentButton.setOnClickListener{
            val intent = Intent(this@UserProfileActivity, PaymentActivity::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }

        logoutButton = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            val intent = Intent(this@UserProfileActivity, LoginActivity::class.java)
            startActivity(intent)
        }

        val usernameTextView = findViewById<TextView>(R.id.usernameTextView)

        // Set the username in the TextView
        usernameTextView.text = username

        // Fetch other user details from the database based on the username
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")

        usersRef.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { userSnapshot ->
                        val userFound = userSnapshot.getValue(User::class.java)

                        if (userFound != null) {
                            val membershipStatusTextView = findViewById<TextView>(R.id.membershipStatusTextView)
                            membershipStatusTextView.text = "${userFound.membership}"

                            if (userFound.membership == "Expired") {
                                membershipStatusTextView.setTextColor(Color.RED)
                            } else {
                                membershipStatusTextView.setTextColor(Color.BLACK)
                            }

                            // Set other user details in the respective TextViews
                            val vehiclePlateTextView = findViewById<TextView>(R.id.vehiclePlateTextView)
                            vehiclePlateTextView.text = "${userFound.licensePlate}"

                            val emailTextView = findViewById<TextView>(R.id.emailTextView)
                            emailTextView.text = "${userFound.email}"

                            val phoneTextView = findViewById<TextView>(R.id.phoneTextView)
                            phoneTextView.text = "${userFound.phoneNumber}"

                            val balanceTextView = findViewById<TextView>(R.id.balanceTextView)
                            balanceTextView.text = "RM ${userFound.accountBalance}"

                            if ("${userFound.membership}" == "Expired"){
                                membershipStatusTextView.text = "Expired"
                            } else{
                                membershipStatusTextView.text = "${userFound.membership}"
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error if needed
                }
            })

        val parkingButton = findViewById<Button>(R.id.parkingButton)
        parkingButton.setOnClickListener {
            onParkingButtonClick()
        }
    }

    fun onParkingButtonClick() {
        val username = intent.getStringExtra("username")
        val intent = Intent(this@UserProfileActivity, ParkingMapActivity::class.java)
        intent.putExtra("username", username)
        startActivity(intent)
    }

}

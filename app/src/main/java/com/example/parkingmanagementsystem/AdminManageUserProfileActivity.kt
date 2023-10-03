package com.example.parkingmanagementsystem

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class AdminManageUserProfileActivity : AppCompatActivity() {

    private lateinit var parkingHistoryButton: Button

    private lateinit var usernameEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var saveButton: Button
    private lateinit var editButton:Button

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_manage_user_profile)

        database = FirebaseDatabase.getInstance().reference

        usernameEditText = findViewById(R.id.usernameEditText)
        searchButton = findViewById(R.id.searchButton)
        saveButton = findViewById(R.id.saveButton)
        editButton = findViewById(R.id.editButton)

        parkingHistoryButton = findViewById(R.id.parkingHistoryButton)
        parkingHistoryButton.setOnClickListener{
            val username = usernameEditText.text.toString()
            if (!username.isNullOrEmpty()){
                val intent = Intent(this@AdminManageUserProfileActivity, ParkingAssignmentList::class.java)
                Log.d("username", username)
                intent.putExtra("username", username)
                startActivity(intent)
            } else {
                Toast.makeText(this@AdminManageUserProfileActivity, "Please enter username", Toast.LENGTH_SHORT).show()
            }
        }




        searchButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            fetchUserDetails(username)
        }

        editButton.setOnClickListener {
            Toast.makeText(this@AdminManageUserProfileActivity, "Field editing enabled", Toast.LENGTH_SHORT).show()
            // Enable editing of EditText views
            enableEditing(true)
        }

        saveButton.setOnClickListener {
            updateUserDetails()
            enableEditing(false)
        }
    }

    private fun fetchUserDetails(username: String) {
        val usersRef = database.child("users")

        usersRef.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val user = userSnapshot.getValue(User::class.java)

                            // Check if user is not null
                            if (user != null) {
                                // Populate EditText fields with user details
                                // Replace these lines with your EditText fields
                                val licensePlateEditText = findViewById<EditText>(R.id.licensePlateEditText)
                                val emailEditText = findViewById<EditText>(R.id.emailEditText)
                                val phoneNumberEditText = findViewById<EditText>(R.id.phoneNumberEditText)

                                licensePlateEditText.setText(user.licensePlate)
                                emailEditText.setText(user.email)
                                phoneNumberEditText.setText(user.phoneNumber)
                            }
                        }
                    } else {
                        // User not found, display a message or handle accordingly
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error if needed
                }
            })
        val adminButton:Button = findViewById(R.id.btnUserProfile)
        adminButton.setOnClickListener(){
            val intent = Intent(this@AdminManageUserProfileActivity, AdminManageUserProfileActivity::class.java)
            startActivity(intent)
        }
    }



    private fun updateUserDetails() {
        val licensePlate = findViewById<EditText>(R.id.licensePlateEditText).text.toString()
        val email = findViewById<EditText>(R.id.emailEditText).text.toString()
        val phoneNumber = findViewById<EditText>(R.id.phoneNumberEditText).text.toString()

        // Update the user details in the database based on the user's username
        val username = usernameEditText.text.toString()
        val usersRef = database.child("users")

        usersRef.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            userSnapshot.ref.child("licensePlate").setValue(licensePlate)
                            userSnapshot.ref.child("email").setValue(email)
                            userSnapshot.ref.child("phoneNumber").setValue(phoneNumber)

                            // Show a success message or handle the subscription here
                            Toast.makeText(this@AdminManageUserProfileActivity, "Update Successful", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // User not found, display a message or handle accordingly
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error if needed
                }
            })

    }

    private fun enableEditing(enable: Boolean) {
        val licensePlateEditText = findViewById<EditText>(R.id.licensePlateEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val phoneNumberEditText = findViewById<EditText>(R.id.phoneNumberEditText)

        licensePlateEditText.isFocusable = enable
        licensePlateEditText.isFocusableInTouchMode = enable

        emailEditText.isFocusable = enable
        emailEditText.isFocusableInTouchMode = enable

        phoneNumberEditText.isFocusable = enable
        phoneNumberEditText.isFocusableInTouchMode = enable
    }
}


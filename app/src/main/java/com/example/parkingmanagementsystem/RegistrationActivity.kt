package com.example.parkingmanagementsystem

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone

class RegistrationActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var nricEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var vehicleLicensePlateEditText: EditText
    private lateinit var database : FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)



        emailEditText = findViewById(R.id.emailEditText)
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        registerButton = findViewById(R.id.registerButton)
        nricEditText = findViewById(R.id.nricEditText)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        vehicleLicensePlateEditText = findViewById(R.id.vehicleLicensePlateEditText)

        passwordEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showPasswordRequirementsDialog()
            }
        }

        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val nric = nricEditText.text.toString()
            val phoneNumber = phoneNumberEditText.text.toString()
            val licensePlate = vehicleLicensePlateEditText.text.toString()
            val database = FirebaseDatabase.getInstance()
            val usersRef = database.getReference("users")

            // Query the database to check if the email or username already exist
            usersRef.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val usernameExists = snapshot.exists()

                        // Check email existence if username doesn't exist
                        if (!usernameExists) {
                            usersRef.orderByChild("email").equalTo(email)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val emailExists = snapshot.exists()

                                        if (!emailExists) {
                                            usersRef.orderByChild("nric").equalTo(nric)
                                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                                    override fun onDataChange(snapshot: DataSnapshot) {
                                                        val nricExists = snapshot.exists()

                                                        if (!nricExists) {
                                                            usersRef.orderByChild("phoneNumber").equalTo(phoneNumber)
                                                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                                                    override fun onDataChange(snapshot: DataSnapshot) {
                                                                        val phoneExists = snapshot.exists()

                                                                        if (!phoneExists) {
                                                                            // Check license plate if necessary

                                                                            if (isInputValid(username, email, password, nric, phoneNumber, licensePlate)) {
                                                                                // Save user account data to the database
                                                                                val userId = usersRef.push().key // Generate a unique key for the user

                                                                                val user = User(userId, username, email, password, nric, phoneNumber, licensePlate)
                                                                                if (userId != null) {
                                                                                    usersRef.child(userId).setValue(user)
                                                                                } // Save the user data to the database

                                                                                // Show registration success message
                                                                                showRegistrationSuccessDialog()
                                                                            }
                                                                        } else {
                                                                            showRegistrationErrorDialog(this@RegistrationActivity, "Phone number already exists.")
                                                                        }
                                                                    }

                                                                    override fun onCancelled(error: DatabaseError) {
                                                                        // Handle database error if needed
                                                                    }
                                                                })
                                                        } else {
                                                            showRegistrationErrorDialog(this@RegistrationActivity, "nric already exists.")
                                                        }
                                                    }

                                                    override fun onCancelled(error: DatabaseError) {
                                                        // Handle database error if needed
                                                    }
                                                })
                                        } else {
                                            showRegistrationErrorDialog(this@RegistrationActivity, "Email already exists.")
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        // Handle database error if needed
                                    }
                                })
                        } else {
                            // Username already exists, show error message
                            showRegistrationErrorDialog(this@RegistrationActivity , "Username already exists.")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle database error if needed
                    }
                })
        }
    }

    private fun isInputValid(username: String, email: String, password: String, nric: String, phoneNumber: String, licensePlate: String): Boolean {
        if (username.isEmpty()) {
            usernameEditText.error = "Username is required"
            return false
        } else if (username.length > 15) {
            usernameEditText.error = "Username must be less than 15 characters"
            return false
        }

        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Invalid email address"
            return false
        }

        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            return false
        } else if (password.length < 6 || password.length > 15) {
            passwordEditText.error = "Password should be between 6 and 15 characters long"
            return false
        } else if (!password.matches(Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{6,15}\$"))) {
            passwordEditText.error =
                "Password should include numbers, uppercase and lowercase letters"
            return false
        }

        val nricPattern = "\\d{12}".toRegex()
        if (!nric.matches(nricPattern)) {
            nricEditText.error = "Invalid ID number"
            return false
        }

        val phonePattern = "\\d{10}".toRegex()
        if (!phoneNumber.matches(phonePattern)) {
            phoneNumberEditText.error = "Invalid phone number"
            return false
        }

        return true
    }

    private fun showRegistrationSuccessDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Registration Successful")
            .setMessage("Your account has been successfully registered.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()

                // Go back to the login page
                finish()
            }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showRegistrationErrorDialog(context: Context, errorMessage: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Registration Error")
            .setMessage(errorMessage)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }


    private fun showPasswordRequirementsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Password Requirements")
            .setMessage(
                "Password should be between 6 and 15 characters long\n" +
                        "Include at least one number\n" +
                        "Include at least one uppercase letter\n" +
                        "Include at least one lowercase letter"
            )
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed() // Go back to the login activity
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

data class User(
    val userId: String? = null,
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val nric: String = "",
    val phoneNumber: String = "",
    val licensePlate: String = "",
    val registrationDate: String = SimpleDateFormat("dd/MM/yyyy HH:mm")
        .apply { timeZone = TimeZone.getTimeZone("GMT+8") }.format(Calendar.getInstance().time),
    val accountBalance: String = "0",
    val membership: String = "Expired",
)
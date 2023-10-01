package com.example.parkingmanagementsystem

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


class LoginActivity : AppCompatActivity() {

    private lateinit var showPasswordToggleButton : ToggleButton
    private lateinit var passwordEditText : EditText
    private lateinit var usernameEditText : EditText
    private lateinit var registerButton : Button
    private lateinit var loginButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        showPasswordToggleButton = findViewById(R.id.showPasswordToggleButton)
        passwordEditText = findViewById(R.id.passwordEditText)
        usernameEditText = findViewById(R.id.usernameEditText)
        registerButton = findViewById(R.id.registerButton)
        loginButton = findViewById(R.id.loginButton)

        registerButton.setOnClickListener{
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            val database = FirebaseDatabase.getInstance()
            val usersRef = database.getReference("users")

            // Query the database to check if the username and password match
            usersRef.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userFound = snapshot.children.firstOrNull()?.getValue(User::class.java)
                        if (userFound != null && userFound.password == password) {

                            //Check if membership date is past, and set expired if so
                            val currentTimeMillis = System.currentTimeMillis()
                            if (userFound.userId == "ADMIN"){
                                val intent = Intent(this@LoginActivity, AdminDashboardActivity::class.java)
                                startActivity(intent)
                            }else if (userFound.membership != "" && userFound.membership != "Expired") {
                                val dateFormat =
                                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                        .apply { timeZone = TimeZone.getTimeZone("GMT+8") }
                                val membershipExpiryDate =
                                    userFound.membership.let { dateFormat.parse(it) }
                                if (membershipExpiryDate != null) {
                                    if (membershipExpiryDate.time <= currentTimeMillis) {
                                        usersRef.child(snapshot.children.first().key!!)
                                            .child("membership").setValue("Expired")
                                    }
                                }
                                val intent = Intent(this@LoginActivity, UserProfileActivity::class.java)
                                intent.putExtra("username", username) // Pass the username
                                startActivity(intent)
                            }

                        } else {
                            // Username and/or password don't match, show error message
                            showLoginErrorDialog(this@LoginActivity)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle database error if needed
                    }
                })
        }

        showPasswordToggleButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // Show password
                passwordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                // Hide password
                passwordEditText.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            // Move cursor to the end of the text
            passwordEditText.setSelection(passwordEditText.text.length)
        }

    }

    private fun showLoginErrorDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Login Error")
            .setMessage("Username and/or password do not match.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }

}
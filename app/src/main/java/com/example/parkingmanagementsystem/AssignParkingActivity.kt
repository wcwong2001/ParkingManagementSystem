package com.example.parkingmanagementsystem

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class AssignParkingActivity : AppCompatActivity() {

    private lateinit var destinationSpinner: Spinner
    private lateinit var mapGuidanceButton: Button
    private lateinit var assignParkingButton: Button
    private lateinit var assignedSpotTextView: TextView
    private lateinit var cancelAssignmentButton: Button
    private lateinit var checkinTimeTextView: TextView

    private lateinit var selectedDestination: String
    private lateinit var availabilityRef: DatabaseReference

    private var selectedParkingLot: String = "A" // Default value
    private var username: String? = null
    private var parkingFee: Double = 0.0
    private var checkInTimeString: String = "";


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assign_parking)
        username = intent.getStringExtra("username")

        destinationSpinner = findViewById(R.id.destinationSpinner)
        mapGuidanceButton = findViewById(R.id.mapGuidanceButton)
        assignParkingButton = findViewById(R.id.parkingHistoryButton)
        assignedSpotTextView = findViewById(R.id.assignedSpotTextView)
        cancelAssignmentButton = findViewById(R.id.cancelAssignmentButton)
        checkinTimeTextView = findViewById(R.id.checkinTimeTextView)

        checkinTimeTextView.visibility = View.GONE
        val parkingButton:Button = findViewById(R.id.parkingButton)
        parkingButton.setOnClickListener {
            val intent = Intent(this@AssignParkingActivity, ParkingMapActivity::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }

        val initParkingLotRef = FirebaseDatabase.getInstance().getReference("lot/$selectedParkingLot")
        updateParkingSlotVisual(initParkingLotRef)
        username?.let { checkAssignedParkingSpot(it) }

        val destinations = listOf("EAST CAMPUS", "CITC", "Block K")
        val destinationCoordinates = mapOf(
            "EAST CAMPUS" to Pair(3.2178909195773695, 101.72880844046568),
            "CITC" to Pair(3.214313130685198, 101.72693483422599),
            "Block K" to Pair(3.2167105598223897, 101.72484042665121)
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, destinations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        destinationSpinner.adapter = adapter

        destinationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                checkinTimeTextView.visibility = View.GONE
                selectedDestination = destinations[position]
                val coordinates = destinationCoordinates[selectedDestination]
                mapGuidanceButton.isEnabled = coordinates != null

                // Update availabilityRef based on selected destination
                availabilityRef = when (selectedDestination) {
                    "Block K" -> FirebaseDatabase.getInstance().getReference("lot/A/availability")
                    "CITC" -> FirebaseDatabase.getInstance().getReference("lot/B/availability")
                    "EAST CAMPUS" -> FirebaseDatabase.getInstance().getReference("lot/C/availability")
                    else -> FirebaseDatabase.getInstance().getReference("lot/A/availability")
                }

                availabilityRef.addValueEventListener(availabilityListener)
                selectedParkingLot = when (selectedDestination) {
                    "Block K" -> "A"
                    "CITC" -> "B"
                    "EAST CAMPUS" -> "C"
                    else -> "A"
                }
                val parkingLotRef = FirebaseDatabase.getInstance().getReference("lot/$selectedParkingLot")
                updateParkingSlotVisual(parkingLotRef)
                username?.let { checkAssignedParkingSpot(it) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val parkingLotRef = FirebaseDatabase.getInstance().getReference("lot/$selectedParkingLot")
        updateParkingSlotVisual(parkingLotRef)
        username?.let { checkAssignedParkingSpot(it) }

        mapGuidanceButton.setOnClickListener {
            val coordinates = destinationCoordinates[selectedDestination]
            if (coordinates != null) {
                val uri = "google.navigation:q=${coordinates.first},${coordinates.second}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                intent.setPackage("com.google.android.apps.maps")
                startActivity(intent)
            }
        }

        assignParkingButton.setOnClickListener {
            if (username != null) {
                val usersRef = FirebaseDatabase.getInstance().getReference("users")

                usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(usersSnapshot: DataSnapshot) {
                        for (userChildSnapshot in usersSnapshot.children) {
                            val userUsername = userChildSnapshot.child("username").getValue(String::class.java)
                            if (userUsername == username) {
                                val accountBalanceStr = userChildSnapshot.child("accountBalance").getValue(String::class.java)
                                if (accountBalanceStr != null) {
                                    try {
                                        val userAccountBalance = accountBalanceStr.toDouble()
                                        if (userAccountBalance >= 20.0) {
                                            // Sufficient balance, assign parking spot
                                            assignParkingSpot(username!!)
                                        } else {
                                            // Insufficient balance, show a message
                                            Toast.makeText(
                                                applicationContext,
                                                "Insufficient account balance. Please recharge your account.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: NumberFormatException) {
                                        // Handle conversion error
                                    }
                                }
                                return
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle database error if needed
                    }
                })
            }
        }
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        // Set the Toolbar as the app bar for the activity
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Assign Parking"
        // Enable the "Up" button (back button)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Define the behavior when the "Up" button is pressed
        toolbar.setNavigationOnClickListener {
            onBackPressed() // This will simulate a back button press
        }
    }

    private val availabilityListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val availability = snapshot.getValue(String::class.java)
            mapGuidanceButton.text = "Map Guidance\nAvailability: $availability/40"
        }

        override fun onCancelled(error: DatabaseError) {
            // Handle database error if needed
        }
    }

    private fun assignParkingSpot(username: String) {
        val parkingLotRef = FirebaseDatabase.getInstance().getReference("lot/$selectedParkingLot")
        checkAssignedParkingSpot(username)
        checkinTimeTextView.visibility = View.VISIBLE
        parkingLotRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(parkingSnapshot: DataSnapshot) {
                for (spotSnapshot in parkingSnapshot.children) {
                    val spotNumber = spotSnapshot.key
                    val availability = spotSnapshot.getValue(String::class.java)
                    checkAssignedParkingSpot(username)

                    if (availability == username) {
                        // User already has an assigned spot
                        displayAssignedSpot(spotNumber)
                        return
                    }
                }

                // No assigned spot found, proceed to assign a new spot
                val availableSpotRef = parkingLotRef.orderByValue().equalTo("available").limitToFirst(1)

                availableSpotRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.hasChildren()) {
                            val spotKey = snapshot.children.first().key
                            if (spotKey != null) {
                                // Update the specific parking spot with the username
                                val spotToUpdateRef = parkingLotRef.child(spotKey)
                                spotToUpdateRef.setValue(username)

                                // Decrement the availability count
                                val availabilityRef = parkingLotRef.child("availability")
                                availabilityRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(availabilitySnapshot: DataSnapshot) {
                                        val availability = availabilitySnapshot.getValue(String::class.java) ?: "0"
                                        val availabilityInt = availability.toIntOrNull() ?: 0

                                        if (availabilityInt > 0) {
                                            val newAvailability = (availabilityInt - 1).toString()
                                            availabilityRef.setValue(newAvailability)
                                            Toast.makeText(applicationContext, "Parking assigned successfully", Toast.LENGTH_SHORT).show()

                                            // Update the visual parking slot status
                                            updateParkingSlotVisual(parkingLotRef)

                                            // Assign the parking to the user's parkingAssignments
                                            assignParkingToUser(username, selectedParkingLot, spotKey.toInt())
                                            checkAssignedParkingSpot(username)
                                        } else {
                                            Toast.makeText(applicationContext, "No available parking spots", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        // Handle database error if needed
                                    }
                                })
                            }
                        } else {
                            Toast.makeText(applicationContext, "No available parking spots", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle database error if needed
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error if needed
            }
        })

    }

    private fun deductParkingFee(username: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(usersSnapshot: DataSnapshot) {
                for (userChildSnapshot in usersSnapshot.children) {
                    val userUsername = userChildSnapshot.child("username").getValue(String::class.java)
                    if (userUsername == username) {
                        val accountBalanceStr = userChildSnapshot.child("accountBalance").getValue(String::class.java)
                        if (accountBalanceStr != null) {
                            try {
                                var userAccountBalance = accountBalanceStr.toDouble()
                                // Deduct the parking fee
                                userAccountBalance -= parkingFee

                                // Update the user's account balance in the database
                                userChildSnapshot.ref.child("accountBalance").setValue(userAccountBalance.toString())
                                return // This return statement exits the loop early once the user is found
                            } catch (e: NumberFormatException) {
                                // Handle conversion error
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error if needed
            }
        })
    }


    private fun assignParkingToUser(username: String, parkingLot: String, parkingSlotNumber: Int) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(usersSnapshot: DataSnapshot) {
                for (userSnapshot in usersSnapshot.children) {
                    val userUsername = userSnapshot.child("username").getValue(String::class.java)
                    if (userUsername == username) {
                        val parkingAssignmentsRef = userSnapshot.child("parkingAssignments")
                        val assignmentId = parkingAssignmentsRef.ref.push().key

                        val assignmentData = HashMap<String, Any>()
                        assignmentData["parkingLot"] = parkingLot
                        assignmentData["parkingSlotNumber"] = parkingSlotNumber
                        checkInTimeString = SimpleDateFormat("dd/MM/yyyy HH:mm")
                            .apply { timeZone = TimeZone.getTimeZone("GMT+8") }.format(
                                Calendar.getInstance().time)
                        assignmentData["checkinTime"] = checkInTimeString
                        assignmentData["checkoutTime"] = ""

                        if (assignmentId != null) {
                            parkingAssignmentsRef.ref.child(assignmentId).setValue(assignmentData)
                        }

                        return
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error if needed
            }
        })
    }


    private fun checkAssignedParkingSpot(username: String) {
        val parkingLotRef = FirebaseDatabase.getInstance().getReference("lot/$selectedParkingLot")

        parkingLotRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(parkingSnapshot: DataSnapshot) {
                var hasAssignedSpot = false
                for (spotSnapshot in parkingSnapshot.children) {
                    val availability = spotSnapshot.getValue(String::class.java)
                    if (availability == username) {
                        hasAssignedSpot = true
                        displayAssignedSpot(spotSnapshot.key)
                        break
                    }
                }

                if (!hasAssignedSpot) {
                    // Hide the assigned spot views if no spot is assigned
                    assignedSpotTextView.visibility = View.GONE
                    cancelAssignmentButton.visibility = View.GONE

                    // Show the "Assign Parking" button
                    assignParkingButton.visibility = View.VISIBLE
                } else {
                    // Hide the "Assign Parking" button if a spot is assigned
                    assignParkingButton.visibility = View.GONE
                }
                checkinTimeTextView.text = "Check-in Time : " + checkInTimeString
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error if needed
            }
        })
    }

    private fun displayAssignedSpot(spotNumber: String?) {
        assignedSpotTextView.text = "Assigned Parking Slot Number: $spotNumber"
        assignedSpotTextView.visibility = View.VISIBLE
        cancelAssignmentButton.visibility = View.VISIBLE
        checkinTimeTextView.text = "Check-in Time : " + checkInTimeString
        cancelAssignmentButton.setOnClickListener {
            cancelParkingAssignment(username!!)
        }
    }

    private fun cancelParkingAssignment(username: String) {
        val parkingLotRef = FirebaseDatabase.getInstance().getReference("lot/$selectedParkingLot")

        parkingLotRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (spotSnapshot in snapshot.children) {
                    val spotNumber = spotSnapshot.key
                    val availability = spotSnapshot.getValue(String::class.java)

                    if (availability == username) {
                        // Found the assigned spot, update it to "available" and record checkout time
                        spotSnapshot.ref.setValue("available")

                        // Increment the availability count
                        val availabilityRef = parkingLotRef.child("availability")
                        availabilityRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(availabilitySnapshot: DataSnapshot) {
                                val availability = availabilitySnapshot.getValue(String::class.java) ?: "0"
                                val availabilityInt = availability.toIntOrNull() ?: 0

                                val newAvailability = (availabilityInt + 1).toString()
                                availabilityRef.setValue(newAvailability)
                                Toast.makeText(applicationContext, "Parking assignment canceled", Toast.LENGTH_SHORT).show()

                                // Update the visual parking slot status
                                updateParkingSlotVisual(parkingLotRef)
                                assignParkingButton.visibility = View.VISIBLE

                                // Update the checkout time for this spot
                                if (spotNumber != null) {
                                    checkout(username)
                                }
                                return
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("CancelAssignment", "Database error: ${error.message}")
                                // Handle database error if needed
                            }
                        })
                        return
                    }
                }

                // If no assigned spot was found for the user
                Log.d("CancelAssignment", "No assigned parking spot found")
                Toast.makeText(applicationContext, "No assigned parking spot found", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CancelAssignment", "Database error: ${error.message}")
                // Handle database error if needed
            }
        })

        assignedSpotTextView.visibility = View.GONE
        cancelAssignmentButton.visibility = View.GONE
        checkinTimeTextView.visibility = View.GONE
    }



    private fun checkout(username: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(usersSnapshot: DataSnapshot) {
                var latestAssignment: DataSnapshot? = null

                for (userSnapshot in usersSnapshot.children) {
                    val userUsername = userSnapshot.child("username").getValue(String::class.java)
                    if (userUsername == username) {
                        val parkingAssignmentsRef = userSnapshot.child("parkingAssignments")

                        // Collect all parking assignment children in reverse order
                        val assignmentsList = ArrayList<DataSnapshot>()
                        for (assignmentSnapshot in parkingAssignmentsRef.children.reversed()) {
                            val parkingLot = assignmentSnapshot.child("parkingLot").getValue(String::class.java)

                            if (parkingLot == selectedParkingLot) {
                                assignmentsList.add(assignmentSnapshot)

                                // Store the latest assignment
                                if (latestAssignment == null) {
                                    latestAssignment = assignmentSnapshot
                                }
                            }
                        }

                        // Update the checkoutTime of the first matching assignment (latest)
                        if (latestAssignment != null) {
                            latestAssignment.ref.child("checkoutTime").setValue(
                                SimpleDateFormat("dd/MM/yyyy HH:mm")
                                    .apply { timeZone = TimeZone.getTimeZone("GMT+8") }
                                    .format(Calendar.getInstance().time)
                            )

                            // Calculate  the parking fee
                            var checkoutTimeString = latestAssignment.child("checkoutTime").getValue(String::class.java)
                            val checkinTimeString = latestAssignment.child("checkinTime").getValue(String::class.java)

                            val checkinTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                .parse(checkinTimeString)?.time ?: 0
                            if(checkoutTimeString.isNullOrBlank()){
                                checkoutTimeString = (
                                        SimpleDateFormat("dd/MM/yyyy HH:mm")
                                            .apply { timeZone = TimeZone.getTimeZone("GMT+8") }
                                            .format(Calendar.getInstance().time)
                                        )
                            }

                            val checkoutTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                .parse(checkoutTimeString)?.time ?: 0

                            parkingFee = calculateParkingFee(checkinTime, checkoutTime)

                            // Record the total fee in the parking assignment
                            latestAssignment.ref.child("totalFee").setValue(parkingFee.toString())

                            val alertDialogBuilder = AlertDialog.Builder(this@AssignParkingActivity)
                            alertDialogBuilder.setTitle("Parking Fee")
                            alertDialogBuilder.setMessage("The parking fee is RM ${String.format("%.2f", parkingFee)}")
                            alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                            }
                            val alertDialog = alertDialogBuilder.create()
                            alertDialog.show()
                            deductParkingFee(username)

                        }

                        return
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error if needed
            }
        })
    }




    private val parkingSpotImageViews: MutableList<ImageView> = mutableListOf()

    private fun updateParkingSlotVisual(parkingLotRef: DatabaseReference) {
        val parkingSlotLayout = findViewById<GridLayout>(R.id.parkingSlotLayout)
        parkingSlotLayout.removeAllViews() // Clear any existing views
        parkingSpotImageViews.clear() // Clear the list of ImageViews

        for (spotNumber in 1..40) {
            val squareSize = resources.getDimensionPixelSize(R.dimen.parking_square_size)
            val square = ImageView(this)

            val layoutParams = GridLayout.LayoutParams().apply {
                width = squareSize
                height = squareSize
                setMargins(2, 2, 2, 2)
            }

            square.layoutParams = layoutParams
            parkingSlotLayout.addView(square)
            parkingSpotImageViews.add(square)

            val spotAvailabilityRef = parkingLotRef.child(spotNumber.toString())
            spotAvailabilityRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val availability = snapshot.getValue(String::class.java)

                    val drawableResourceId = when (availability) {
                        "available" -> R.drawable.available_parking_square
                        "unavailable" -> R.drawable.unavailable_parking_square
                        else -> R.drawable.unavailable_parking_square
                    }

                    square.setImageResource(drawableResourceId)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error if needed
                }
            })
        }
    }

    private fun calculateParkingFee(checkinTime: Long, checkoutTime: Long): Double {
        val durationInMillis = checkoutTime - checkinTime
        val durationInHours = TimeUnit.MILLISECONDS.toHours(durationInMillis)

        val firstHourFree = 1
        val next4HoursRate = 2
        val after4HoursRate = 3
        val maxFeeCap = 20

        var totalFee :Long  = 0L

        if (durationInHours <= firstHourFree) {
            // First hour is free
            totalFee = 0
        } else if (durationInHours <= (firstHourFree + 4)) {
            // First hour free, next 4 hours at RM2 per hour
            totalFee = ((durationInHours - firstHourFree) * next4HoursRate)
        } else {
            // First hour free, next 4 hours at RM2 per hour, additional hours at RM3 per hour
            totalFee = ((4 * next4HoursRate) + ((durationInHours - firstHourFree - 4) * after4HoursRate))
        }

        // Apply the fee cap
        totalFee = totalFee.coerceAtMost(maxFeeCap.toLong())

        return totalFee.toDouble()
    }
}


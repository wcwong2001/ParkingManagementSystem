package com.example.parkingmanagementsystem

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class ParkingLotMapActivity : AppCompatActivity() {

    private lateinit var parkingLayout: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parking_lot_map)

        val username = intent.getStringExtra("username")
        val parkingLot = intent.getStringExtra("parkingLot")

        parkingLayout = findViewById(R.id.parkingLayout)
        val parkingLotRef = FirebaseDatabase.getInstance().getReference("lot/$parkingLot")

        // Attach a ChildEventListener to listen for changes in the parking lot spots
        parkingLotRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                updateParkingSpot(snapshot.key?.toIntOrNull(), snapshot.value as String)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                updateParkingSpot(snapshot.key?.toIntOrNull(), snapshot.value as String)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle spot removal if needed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle spot movement if needed
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error if needed
            }
        })
    }

    private fun updateParkingSpot(spotNumber: Int?, availability: String) {
        if (spotNumber != null) {
            val squareSize = resources.getDimensionPixelSize(R.dimen.parking_square_size)
            val square = ImageView(this)
            val drawableResourceId = when (availability) {
                "available" -> R.drawable.available_parking_square
                "unavailable" -> R.drawable.unavailable_parking_square
                else -> R.drawable.unavailable_parking_square
            }

            square.setImageResource(drawableResourceId)
            square.layoutParams = GridLayout.LayoutParams().apply {
                width = squareSize
                height = squareSize
                setMargins(2, 2, 2, 2)
            }

            // Remove the previous parking spot image if it exists
            val existingView = parkingLayout.findViewWithTag<View>(spotNumber)
            if (existingView != null) {
                parkingLayout.removeView(existingView)
            }

            // Set a unique tag to the square to identify it
            square.tag = spotNumber

            // Add the updated parking spot image
            parkingLayout.addView(square)
        }
    }
}


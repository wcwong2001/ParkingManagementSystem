package com.example.parkingmanagementsystem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import androidx.appcompat.widget.Toolbar;

public class PaymentActivity extends AppCompatActivity {
    Button topUpButton;
    private static final String PUBLISHABLE_KEY = "pk_test_51NkhA0I2QUd3ZywRowXRiibZEAOMDSmSaLUxhbO4XnGyKXKkgCL9I8uixNz8kFGI0Ywk3DWDaC0l0klPtbGRwgIC00cUkRr6XV";
    private static final String SECRET_KEY = "sk_test_51NkhA0I2QUd3ZywRGqh3kzI58hklepQUHFqv4TUWV60saCavaXE7akPoYym8TYqVQ2FRpdXJOzM4JXbPMtT6btBL00x3zdWFGX";
    private String customerId;
    private String ephemeralKey;
    private String clientSecret;
    private String amountStr = "2000"; //RM20 in cents(*100)

    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    PaymentSheet paymentSheet;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        String username = getIntent().getStringExtra("username");

        Button accountButton = findViewById(R.id.accountButton);
         accountButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 Intent intent = new Intent(PaymentActivity.this, ParkingMapActivity.class);
                 intent.putExtra("username", username);
                 startActivity(intent);
             }
         });

        Button oneMonthSubscriptionButton = findViewById(R.id.oneMonthSubscriptionButton);
        Button threeMonthsSubscriptionButton = findViewById(R.id.threeMonthsSubscriptionButton);
        Button sixMonthsSubscriptionButton = findViewById(R.id.sixMonthsSubscriptionButton);


        oneMonthSubscriptionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSubscription(5.0, 1); // Deduct RM5 for 1 month subscription
            }
        });

        threeMonthsSubscriptionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSubscription(10.0, 3); // Deduct RM10 for 3 months subscription
            }
        });

        sixMonthsSubscriptionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSubscription(20.0, 6); // Deduct RM20 for 6 months subscription
            }
        });


        PaymentConfiguration.init(this, PUBLISHABLE_KEY);

        paymentSheet = new PaymentSheet(this, paymentSheetResult -> {
            onPaymentResult(paymentSheetResult);
        });

        topUpButton = findViewById(R.id.topUpButton);
        topUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentFlow();
            }
        });


        StringRequest request = new StringRequest(Request.Method.POST,
                "https://api.stripe.com/v1/customers",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject object = new JSONObject(response);
                            customerId = object.getString("id");
                            getEphemeralKey();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                Toast.makeText(PaymentActivity.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> header = new HashMap<>();
                header.put("Authorization", "Bearer " + SECRET_KEY);
                return header;
            }

        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Membership Subcription");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Enable the "Up" button
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed(); // Handle "Up" button click
            }
        });



    }




    private void handleSubscription(double subscriptionAmount, int subscriptionMonths) {
        String username = getIntent().getStringExtra("username");
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot usersSnapshot) {
                for (DataSnapshot userChildSnapshot : usersSnapshot.getChildren()) {
                    String userUsername = userChildSnapshot.child("username").getValue(String.class);
                    if (userUsername != null && userUsername.equals(username)) {
                        String accountBalanceStr = userChildSnapshot.child("accountBalance").getValue(String.class);
                        if (accountBalanceStr != null) {
                            try {
                                double userAccountBalance = Double.parseDouble(accountBalanceStr);
                                if (userAccountBalance >= subscriptionAmount) {
                                    // Deduct the subscription amount
                                    userAccountBalance -= subscriptionAmount;

                                    // Get the current membership status
                                    String membershipStatus = userChildSnapshot.child("membership").getValue(String.class);

                                    // Calculate the new membership expiry date
                                    Calendar calendar = Calendar.getInstance();
                                    if (membershipStatus != null && !membershipStatus.equals("Expired")) {
                                        // Existing membership is a date, add months to it
                                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                        try {
                                            Date currentDate = sdf.parse(membershipStatus);
                                            calendar.setTime(currentDate);
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    // Add the specified number of months to the current date
                                    calendar.add(Calendar.MONTH, subscriptionMonths);
                                    Date newMembershipDate = calendar.getTime();
                                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                    String newMembershipStatus = sdf.format(newMembershipDate);

                                    // Update the user's account balance and membership status in the database
                                    userChildSnapshot.getRef().child("accountBalance").setValue(String.valueOf(userAccountBalance));
                                    userChildSnapshot.getRef().child("membership").setValue(newMembershipStatus);

                                    // Show a success message or handle the subscription here
                                    Toast.makeText(PaymentActivity.this, "Subscription successful!", Toast.LENGTH_SHORT).show();
                                } else {
                                    // Insufficient balance
                                    Toast.makeText(PaymentActivity.this, "Insufficient balance.", Toast.LENGTH_SHORT).show();
                                }
                                return; // This return statement exits the loop early once the user is found
                            } catch (NumberFormatException e) {
                                // Handle conversion error
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle database error if needed
            }
        });
    }

    private void paymentFlow() {
        paymentSheet.presentWithPaymentIntent(clientSecret, new PaymentSheet.Configuration(
                        "Parking Management System"
                , new PaymentSheet.CustomerConfiguration(customerId, ephemeralKey)));
    }

    private void onPaymentResult(PaymentSheetResult paymentSheetResult) {
        if(paymentSheetResult instanceof PaymentSheetResult.Completed){
            Toast.makeText(this,"Payment Success",Toast.LENGTH_SHORT).show();
            updateAccountBalance();
        }
    }

    private void updateAccountBalance() {
        String username = getIntent().getStringExtra("username");
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot usersSnapshot) {
                for (DataSnapshot userChildSnapshot : usersSnapshot.getChildren()) {

                    String userUsername = userChildSnapshot.child("username").getValue(String.class);
                    if (userUsername != null && userUsername.equals(username)) {
                        String accountBalanceStr = userChildSnapshot.child("accountBalance").getValue(String.class);
                        if (accountBalanceStr != null) {
                            try {
                                double userAccountBalance = Double.parseDouble(accountBalanceStr);
                                // Deduct the parking fee
                                userAccountBalance += Double.parseDouble(amountStr) / 100;


                                // Update the user's account balance in the database
                                userChildSnapshot.getRef().child("accountBalance").setValue(String.valueOf(userAccountBalance));
                                return; // This return statement exits the loop early once the user is found
                            } catch (NumberFormatException e) {
                                // Handle conversion error
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void getEphemeralKey() {
        StringRequest request = new StringRequest(Request.Method.POST,
                "https://api.stripe.com/v1/ephemeral_keys",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject object = new JSONObject(response);
                            ephemeralKey = object.getString("id");
                            getClientSecret(customerId, ephemeralKey);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                Toast.makeText(PaymentActivity.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {

                Map<String, String> header = new HashMap<>();
                header.put("Authorization","Bearer " + SECRET_KEY);
                header.put("Stripe-Version","2023-08-16");
                return header;
            }

            @Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                Map<String,String> params = new HashMap<>();
                params.put("customer", customerId);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);

    }

    private void getClientSecret(String customerId, String ephericalKey) {
        StringRequest request = new StringRequest(Request.Method.POST,
                "https://api.stripe.com/v1/payment_intents",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject object = new JSONObject(response);
                            clientSecret = object.getString("client_secret");

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                Toast.makeText(PaymentActivity.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {

                Map<String, String> header = new HashMap<>();
                header.put("Authorization","Bearer " + SECRET_KEY);
                return header;
            }

            @Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                Map<String,String> params = new HashMap<>();
                params.put("customer", PaymentActivity.this.customerId);
                params.put("amount", amountStr); //RM20
                params.put("currency", "myr");

                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }



        // Find the "RETURN" button by its ID


}
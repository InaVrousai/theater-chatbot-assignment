package com.example.theaterapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WalletActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        TextView ticketText = findViewById(R.id.ticketInfo);

        SharedPreferences prefs = getSharedPreferences("Bookings", MODE_PRIVATE);
        String booking = prefs.getString("latestBooking", null);

        if (booking != null) {
            ticketText.setText("🎫 Κράτηση:\n" + booking + "\n\n🔐 Κωδικός Ακύρωσης: 12345");
        } else {
            ticketText.setText("Δεν υπάρχουν κρατήσεις.");
        }
    }
}
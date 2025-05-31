package com.example.theaterapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WalletActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        TextView showNameView   = findViewById(R.id.showName);
        TextView dateTimeView   = findViewById(R.id.showDateTime);
        TextView seatsView      = findViewById(R.id.showSeats);
        TextView holderView     = findViewById(R.id.holderName);
        TextView ticketCodeView = findViewById(R.id.ticketCode);
        TextView cancelCodeView = findViewById(R.id.cancelCode);
        ImageView barcodeView   = findViewById(R.id.barcodeImage);

        SharedPreferences prefs = getSharedPreferences("Bookings", MODE_PRIVATE);
        String booking = prefs.getString("latestBooking", null);

        if (booking != null) {
            String[] lines = booking.split("\n");
            // 0: "<Show> στις <Time>"
            // 1: "Θέσεις: <seats>"
            // 2: "Όνομα: <holder>"
            // 3: "Κωδικός: <code>"

            if (lines.length >= 4) {
                String[] firstParts = lines[0].split(" στις ");
                String show = firstParts[0].trim();
                String datetime = firstParts.length > 1 ? firstParts[1].trim() : "";

                String seats = lines[1].replaceFirst("Θέσεις:\\s*", "").trim();
                String holder = lines[2].replaceFirst("Όνομα:\\s*", "").trim();
                String ticketCode = lines[3].replaceFirst("Κωδικός:\\s*", "").trim();

                showNameView.setText("Παράσταση: " + show);
                dateTimeView.setText("Ημερομηνία/Ώρα: " + datetime);
                seatsView.setText("Θέσεις: " + seats);
                holderView.setText("Κάτοχος: " + holder);
                ticketCodeView.setText("Κωδικός Εισιτηρίου: " + ticketCode);
                cancelCodeView.setText("Κωδικός Ακύρωσης: " + prefs.getString("latestCancelCode", "–"));
            } else {
                showNameView.setText("Δεν βρέθηκαν πλήρη στοιχεία κράτησης.");
                dateTimeView.setText("");
                seatsView.setText("");
                holderView.setText("");
                ticketCodeView.setText("");
                cancelCodeView.setText("");
            }
        } else {
            showNameView.setText("Δεν υπάρχουν κρατήσεις.");
            dateTimeView.setText("");
            seatsView.setText("");
            holderView.setText("");
            ticketCodeView.setText("");
            cancelCodeView.setText("");
            barcodeView.setImageResource(R.drawable.ic_no_ticket);
        }
    }
}

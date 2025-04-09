package com.example.theaterapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class ChatActivity extends AppCompatActivity {

    EditText inputField;
    Button sendButton;
    TextView chatBox;

    StringBuilder chatHistory = new StringBuilder();
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        inputField = findViewById(R.id.inputField);
        sendButton = findViewById(R.id.sendButton);
        chatBox = findViewById(R.id.chatBox);

        prefs = getSharedPreferences("Bookings", MODE_PRIVATE);

        sendButton.setOnClickListener(v -> handleMessage());
    }

    private void handleMessage() {
        String userMessage = inputField.getText().toString().trim();
        if (userMessage.isEmpty()) return;

        appendMessage("You: " + userMessage);
        inputField.setText("");

        String response = generateResponse(userMessage);
        appendMessage("Theater: " + response);
    }

    private String generateResponse(String msg) {
        msg = msg.toLowerCase();

        if (msg.contains("info") || msg.contains("shows") || msg.contains("available")) {
            return getShowInfo();
        }

        if (msg.contains("book") || msg.contains("reserve") || msg.contains("ticket")) {
            if (msg.contains("oedipus")) {
                saveBooking("Oedipus Rex - Hall 1 - 18:00");
                return "Your booking for 'Oedipus Rex' at 18:00 is confirmed.";
            } else if (msg.contains("antigone")) {
                saveBooking("Antigone - Hall 2 - 17:30");
                return "Your booking for 'Antigone' at 17:30 is confirmed.";
            } else if (msg.contains("medea")) {
                saveBooking("Medea - Hall 1 - 20:00");
                return "Your booking for 'Medea' at 20:00 is confirmed.";
            } else {
                return "Which show would you like to book? Try: 'Oedipus', 'Antigone', or 'Medea'.";
            }
        }

        if (msg.contains("cancel")) {
            removeBooking();
            return "Your booking has been cancelled.";
        }

        if (msg.contains("booking") || msg.contains("reservation")) {
            return getBooking();
        }

        if (msg.contains("help") || msg.contains("human")) {
            return "Connecting you to a theater representative...";
        }

        return "Sorry, I didn't understand that. You can ask for show info or book a ticket for 'Oedipus', 'Antigone', or 'Medea'.";
    }

    private String getShowInfo() {
        return "🎭 Available Shows:\n" +
                "1. Oedipus Rex - Hall 1 - 18:00 & 21:00\n" +
                "2. Antigone - Hall 2 - 17:30 & 20:30\n" +
                "3. Medea - Hall 1 - 20:00";
    }

    private void saveBooking(String data) {
        prefs.edit().putString("latestBooking", data).apply();
    }

    private void removeBooking() {
        prefs.edit().remove("latestBooking").apply();
    }

    private String getBooking() {
        String data = prefs.getString("latestBooking", null);
        return data != null ? "Your current booking: " + data : "You have no active bookings.";
    }

    private void appendMessage(String text) {
        chatHistory.append(text).append("\n\n");
        chatBox.setText(chatHistory.toString());
    }
}

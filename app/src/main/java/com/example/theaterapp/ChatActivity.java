package com.example.theaterapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    EditText inputField;
    Button sendButton;
    private RecyclerView chatRecycler;
    private List<ChatMessage> messages;
    private ChatAdapter adapter;

    SharedPreferences prefs;

    private String pendingAction = null;
    private String tempBooking = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        inputField = findViewById(R.id.inputField);
        sendButton = findViewById(R.id.sendButton);
        chatRecycler = findViewById(R.id.chatRecycler);

        // Initialize message list and adapter
        messages = new ArrayList<>();
        adapter = new ChatAdapter(messages);
        chatRecycler.setLayoutManager(new LinearLayoutManager(this));
        chatRecycler.setAdapter(adapter);

        prefs = getSharedPreferences("Bookings", MODE_PRIVATE);

        // initial bot greeting
        appendMessage("Πώς μπορώ να βοηθήσω;", false);

        sendButton.setOnClickListener(v -> handleMessage());
    }

    private void handleMessage() {
        String userMessage = inputField.getText().toString().trim();
        if (userMessage.isEmpty()) return;

        appendMessage(userMessage, true);
        inputField.setText("");

        String response = generateResponse(userMessage.toLowerCase());
        appendMessage(response, false);
    }

    private String generateResponse(String msg) {
        if (pendingAction != null) {
            if (msg.contains("ναι")) {
                if (pendingAction.equals("book") && tempBooking != null) {
                    saveBooking(tempBooking);
                    resetConfirmationState();
                    return "Η κράτησή σας καταχωρήθηκε: " + tempBooking;
                } else if (pendingAction.equals("cancel")) {
                    removeBooking();
                    resetConfirmationState();
                    return "Η κράτησή σας ακυρώθηκε.";
                }
            } else if (msg.contains("όχι")) {
                resetConfirmationState();
                return "Η ενέργεια ακυρώθηκε.";
            } else {
                return "Παρακαλώ απαντήστε με 'ναι' ή 'όχι'.";
            }
        }

        if (msg.contains("πληροφορίες") || msg.contains("παραστάσεις") || msg.contains("διαθέσιμες")) {
            return getShowInfo();
        }

        if (msg.contains("κλείσε") || msg.contains("κράτηση") || msg.contains("εισιτήριο")) {
            if (msg.contains("οιδίπους")) {
                tempBooking = "Οιδίπους Τύραννος - Αίθουσα 1 - 18:00";
            } else if (msg.contains("αντιγόνη")) {
                tempBooking = "Αντιγόνη - Αίθουσα 2 - 17:30";
            } else if (msg.contains("μήδεια")) {
                tempBooking = "Μήδεια - Αίθουσα 1 - 20:00";
            } else {
                return "Για ποια παράσταση θέλετε να κάνετε κράτηση; Π.χ. 'Οιδίπους', 'Αντιγόνη', 'Μήδεια'";
            }
            pendingAction = "book";
            return "Επιβεβαιώνετε την κράτηση για: " + tempBooking + "; (ναι / όχι)";
        }

        if (msg.contains("ακύρωσε") || msg.contains("ακύρωση")) {
            if (prefs.contains("latestBooking")) {
                pendingAction = "cancel";
                return "Είστε σίγουροι ότι θέλετε να ακυρώσετε την κράτησή σας; (ναι / όχι)";
            } else {
                return "Δεν υπάρχει κράτηση για ακύρωση.";
            }
        }

        if (msg.contains("κράτηση") || msg.contains("κρατήσεις")) {
            return getBooking();
        }

        if (msg.contains("βοήθεια") || msg.contains("υπάλληλος")) {
            return "Σας συνδέουμε με έναν εκπρόσωπο του θεάτρου...";
        }

        return "Συγγνώμη, δεν κατάλαβα. Θέλετε να δείτε [πληροφορίες], να κάνετε [κράτηση], να [ακυρώσετε] ή να μιλήσετε με [υπάλληλο];";
    }

    private void resetConfirmationState() {
        pendingAction = null;
        tempBooking = null;
    }

    private String getShowInfo() {
        return "🎭 Διαθέσιμες Παραστάσεις:\n" +
                "1. Οιδίπους Τύραννος - Αίθουσα 1 - 18:00 & 21:00\n" +
                "2. Αντιγόνη - Αίθουσα 2 - 17:30 & 20:30\n" +
                "3. Μήδεια - Αίθουσα 1 - 20:00";
    }

    private void saveBooking(String data) {
        prefs.edit().putString("latestBooking", data).apply();
    }

    private void removeBooking() {
        prefs.edit().remove("latestBooking").apply();
    }

    private String getBooking() {
        String data = prefs.getString("latestBooking", null);
        return data != null ? "Η ενεργή κράτησή σας είναι: " + data : "Δεν έχετε κάποια ενεργή κράτηση.";
    }

    private void appendMessage(String text, boolean isUser) {
        messages.add(new ChatMessage(text, isUser));
        adapter.notifyItemInserted(messages.size() - 1);
        chatRecycler.scrollToPosition(messages.size() - 1);
    }

}
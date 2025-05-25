package com.example.theaterapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.theaterapp.api.WitResponse;
import com.example.theaterapp.api.WitService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";

    EditText inputField;
    Button sendButton;
    RecyclerView chatRecyclerView;
    List<ChatMessage> messageList = new ArrayList<>();
    ChatAdapter adapter;

    SharedPreferences prefs;
    private String pendingAction = null;
    private String tempBooking = null;

    // Retrofit + logging interceptor for Wit.ai
    private WitService witService;
    private final String WIT_TOKEN = "Bearer " + BuildConfig.WIT_AI_SERVER_TOKEN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        inputField = findViewById(R.id.inputField);
        sendButton = findViewById(R.id.sendButton);
        chatRecyclerView = findViewById(R.id.chatRecycler);
        prefs = getSharedPreferences("Bookings", MODE_PRIVATE);

        adapter = new ChatAdapter(messageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(adapter);

        appendMessage("Πώς μπορώ να βοηθήσω;", false);

        // Setup Retrofit + logging for Wit.ai
        HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logInterceptor)
                .build();

        Retrofit witRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.wit.ai/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        witService = witRetrofit.create(WitService.class);

        sendButton.setOnClickListener(v -> handleMessage());
    }

    private void handleMessage() {
        String userMessage = inputField.getText().toString().trim();
        if (userMessage.isEmpty()) return;

        appendMessage(userMessage, true);
        inputField.setText("");

        callWitAI(userMessage);
    }

    private void callWitAI(String userInput) {
        witService.getMessage("20230608", userInput, WIT_TOKEN)
                .enqueue(new Callback<WitResponse>() {
                    @Override
                    public void onResponse(Call<WitResponse> call, Response<WitResponse> resp) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            WitResponse wr = resp.body();
                            String intent = wr.intents.isEmpty()
                                    ? "none"
                                    : wr.intents.get(0).name;
                            handleWitIntent(intent, wr.entities);
                        } else {
                            Log.e(TAG, "Wit.ai bad response: " + resp.code());
                            appendMessage("⚠️ Σφάλμα από Wit.ai", false);
                        }
                    }

                    @Override
                    public void onFailure(Call<WitResponse> call, Throwable t) {
                        Log.e(TAG, "Wit.ai call failed", t);
                        appendMessage("⚠️ Αποτυχία σύνδεσης με Wit.ai", false);
                    }
                });
    }

    private void handleWitIntent(String intent, Map<String, List<WitResponse.Entity>> entities) {
        switch (intent) {
            case "info":
                appendMessage(getShowInfo(), false);
                break;
            case "book_ticket":
                appendMessage("Για ποια παράσταση θέλετε να κάνετε κράτηση;", false);
                break;
            case "cancel_ticket":
                if (prefs.contains("latestBooking")) {
                    pendingAction = "cancel";
                    appendMessage("Είστε σίγουροι ότι θέλετε να ακυρώσετε την κράτησή σας; (ναι/όχι)", false);
                } else {
                    appendMessage("Δεν έχετε κάποια κράτηση για ακύρωση.", false);
                }
                break;
            case "confirm_yes":
                if ("book".equals(pendingAction)) {
                    saveBooking(tempBooking);
                    resetConfirmationState();
                    appendMessage("Η κράτησή σας επιβεβαιώθηκε!", false);
                } else if ("cancel".equals(pendingAction)) {
                    removeBooking();
                    resetConfirmationState();
                    appendMessage("Η κράτηση ακυρώθηκε.", false);
                } else {
                    appendMessage("Δεν υπάρχει ενέργεια προς επιβεβαίωση.", false);
                }
                break;
            case "confirm_no":
                resetConfirmationState();
                appendMessage("Η ενέργεια ακυρώθηκε.", false);
                break;
            case "booking_status":
                appendMessage(getBooking(), false);
                break;
            default:
                appendMessage("Δεν κατάλαβα. Δοκιμάστε ξανά.", false);
        }
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
        return data != null
                ? "Η ενεργή κράτησή σας είναι: " + data
                : "Δεν έχετε κάποια ενεργή κράτηση.";
    }

    private void appendMessage(String text, boolean isUser) {
        messageList.add(new ChatMessage(text, isUser));
        adapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }
}

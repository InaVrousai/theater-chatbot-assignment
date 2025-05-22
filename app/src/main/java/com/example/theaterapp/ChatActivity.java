package com.example.theaterapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.theaterapp.api.LlamaService;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatActivity extends AppCompatActivity {

    EditText inputField;
    Button sendButton;
    RecyclerView chatRecyclerView;
    List<ChatMessage> messageList = new ArrayList<>();
    ChatAdapter adapter;

    SharedPreferences prefs;
    private String pendingAction = null;
    private String tempBooking = null;

    Retrofit retrofit;
    LlamaService llamaService;
    Gson gson = new Gson();

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

        retrofit = new Retrofit.Builder()
                .baseUrl("https://api-inference.huggingface.co/")

                .addConverterFactory(GsonConverterFactory.create())
                .build();

        llamaService = retrofit.create(LlamaService.class);

        sendButton.setOnClickListener(v -> handleMessage());
    }

    private void handleMessage() {
        String userMessage = inputField.getText().toString().trim();
        if (userMessage.isEmpty()) return;

        appendMessage(userMessage, true);
        inputField.setText("");

        callLlamaAPI(userMessage);
    }

    private void callLlamaAPI(String userInput) {
        String prompt = "You are a Greek theater booking assistant. Respond ONLY in valid JSON like:\n" +
                "{ \"intent\": \"info\", \"show\": null, \"confirmation_required\": false }\n" +
                "Possible intents: info, book, cancel, confirm_yes, confirm_no, help, booking_status.\n\n" +
                "User: " + userInput;

        LlamaRequest request = new LlamaRequest(prompt);
        String apiKey = BuildConfig.HF_API_KEY;

        llamaService.getChatCompletion("Bearer " + apiKey, request).enqueue(new Callback<LlamaResponse>()
        {
            @Override
            public void onResponse(Call<LlamaResponse> call, Response<LlamaResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String raw = response.body().generated_text.trim();
                    Log.d("LLaMA_RAW", raw);
                    try {
                        JSONObject obj = new JSONObject(raw);
                        String intent = obj.getString("intent");
                        String show = obj.optString("show", null);
                        boolean confirmation = obj.optBoolean("confirmation_required", false);
                        appendMessage(generateBotReply(intent, show, confirmation), false);
                    } catch (JSONException e) {
                        Log.e("LLaMA_PARSE", "Invalid JSON", e);
                        appendMessage("⚠️ Δεν κατάλαβα την απάντηση.", false);
                    }
                } else {
                    Log.e("LLaMA_API", "Bad response: " + response.code());
                    appendMessage("⚠️ Σφάλμα από τον LLaMA API.", false);
                }
            }


            @Override
            public void onFailure(Call<LlamaResponse> call, Throwable t) {
                Log.e("LLaMA_FAIL", "Call failed", t);
                appendMessage("⚠️ Αποτυχία σύνδεσης με LLaMA.", false);
            }

        });

    }



    private String generateBotReply(String intent, String show, boolean confirmation) {
        switch (intent) {
            case "info":
                return getShowInfo();
            case "book":
                if (show == null) return "Για ποια παράσταση θέλετε να κάνετε κράτηση;";
                tempBooking = show + " - ώρα και αίθουσα TBD";
                pendingAction = "book";
                return confirmation ? "Επιβεβαιώνετε την κράτηση για: " + tempBooking + "; (ναι / όχι)" : "Η κράτηση έγινε για: " + tempBooking;
            case "cancel":
                if (prefs.contains("latestBooking")) {
                    pendingAction = "cancel";
                    return "Είστε σίγουροι ότι θέλετε να ακυρώσετε την κράτησή σας; (ναι / όχι)";
                } else {
                    return "Δεν υπάρχει κράτηση για ακύρωση.";
                }
            case "confirm_yes":
                if ("book".equals(pendingAction)) {
                    saveBooking(tempBooking);
                    resetConfirmationState();
                    return "Η κράτησή σας επιβεβαιώθηκε!";
                } else if ("cancel".equals(pendingAction)) {
                    removeBooking();
                    resetConfirmationState();
                    return "Η κράτηση ακυρώθηκε.";
                }
                return "Δεν υπάρχει ενέργεια προς επιβεβαίωση.";
            case "confirm_no":
                resetConfirmationState();
                return "Η ενέργεια ακυρώθηκε.";
            case "help":
                return "Σας συνδέουμε με έναν εκπρόσωπο...";
            case "booking_status":
                return getBooking();
            default:
                return "Δεν κατάλαβα. Θέλετε να κάνετε κράτηση, ακύρωση ή να δείτε παραστάσεις;";
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
        return data != null ? "Η ενεργή κράτησή σας είναι: " + data : "Δεν έχετε κάποια ενεργή κράτηση.";
    }

    private void appendMessage(String text, boolean isUser) {
        messageList.add(new ChatMessage(text, isUser));
        adapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }
}

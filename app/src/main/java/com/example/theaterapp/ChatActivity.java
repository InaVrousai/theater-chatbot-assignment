package com.example.theaterapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.theaterapp.api.WitResponse;
import com.example.theaterapp.api.WitService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;


import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private static final String STATE_ENTER_NAME = "enterName";
    private static final int REQUEST_CODE_SELECT_SEATS = 1001;

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
    private final String WIT_VERSION = "20250527";
    private final String WIT_TOKEN = "Bearer " + BuildConfig.WIT_AI_SERVER_TOKEN;

    private LinearLayout quickRepliesLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        inputField = findViewById(R.id.inputField);
        sendButton = findViewById(R.id.sendButton);
        chatRecyclerView = findViewById(R.id.chatRecycler);
        quickRepliesLayout = findViewById(R.id.quickRepliesLayout);
        prefs = getSharedPreferences("Bookings", MODE_PRIVATE);

        adapter = new ChatAdapter(messageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(adapter);


        appendMessage("Πώς μπορώ να βοηθήσω;", false);
        showQuickReplies();

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

        sendButton.setOnClickListener(v -> {
            // disable quick replies when typing
            quickRepliesLayout.setVisibility(View.GONE);
            handleMessage();
        });
    }

    private void showQuickReplies() {
        quickRepliesLayout.removeAllViews();
        String[] options = new String[]{
                "Πληροφορίες παραστάσεων",
                "Κράτηση εισιτηρίου",
                "Ακύρωση εισιτηρίου",
                "Επικοινωνία με υπάλληλο"
    };
    for (String opt : options) {
        Button btn = new Button(this);
        btn.setText(opt);
        btn.setAllCaps(false);
        btn.setOnClickListener(v -> {
            quickRepliesLayout.setVisibility(View.GONE);
            appendMessage(opt, true);
            // χειρισμός χωρίς free-text
            switch (opt) {
                case "Πληροφορίες παραστάσεων":
                    handleWitIntent("seeSchedule", Collections.emptyMap());
                    break;
                case "Κράτηση εισιτηρίου":
                    handleWitIntent("makeReservation", Collections.emptyMap());
                    break;
                case "Ακύρωση εισιτηρίου":
                    handleWitIntent("cancelReservation", Collections.emptyMap());
                    break;
                case "Επικοινωνία με υπάλληλο":
                    handleWitIntent("contactStaff", Collections.emptyMap());
                    break;
            }
        });
        quickRepliesLayout.addView(btn);
    }
    quickRepliesLayout.setVisibility(View.VISIBLE);
}


    void onQuickReplyClicked(String text) {
        appendMessage(text, true);
        quickRepliesLayout.setVisibility(View.GONE);

        switch (pendingAction) {
            case "selectShow":
                showTimeButtons(text);
                break;

            case "selectTime":
                // Proceed to seat selection
                String showName = tempBooking;
                String showTime = text;
                startSeatSelection(showName, showTime);
                break;

            case "selectSeat":
                appendMessage("Έχετε επιλέξει θέση: " + text, false);
                tempBooking = tempBooking + " - Θέση: " + text;
                pendingAction = STATE_ENTER_NAME;
                appendMessage("Παρακαλώ γράψτε το ονοματεπώνυμό σας για την επιβεβαίωση της κράτησης.", false);
                break;

            default:
                appendMessage("⚠️ Σφάλμα επιλογής", false);
                showQuickReplies();
        }
    }



    private void appendButtonMessage(List<String> options) {
        messageList.add(new ChatMessage(options));
        adapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }

//    private void showSeatButtons(String showName, String time) {
//        pendingAction = "selectSeat";
//        tempBooking = showName + " - " + time;
//
//        appendMessage("Παρακαλώ επιλέξτε θέση", false);
//
//        List<String> seats = Arrays.asList("A1", "A2", "B1", "B2", "Γ1", "Γ2");
//        appendButtonMessage(seats);
//    }

    private void startSeatSelection(String showName, String time) {
        tempBooking = showName + " στις " + time;
        Intent intent = new Intent(this, SeatSelectionActivity.class);
        intent.putExtra("showName", showName);
        intent.putExtra("showTime", time);
        startActivityForResult(intent, REQUEST_CODE_SELECT_SEATS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_SEATS && resultCode == RESULT_OK && data != null) {
            String[] seats = data.getStringArrayExtra("selectedSeats");
            if (seats != null && seats.length > 0) {
                String sel = String.join(", ", seats);
                appendMessage("Έχετε επιλέξει θέσεις: " + sel, false);
                // πλέον ζητάμε το όνομα
                pendingAction = STATE_ENTER_NAME;
                appendMessage("Παρακαλώ γράψτε το ονοματεπώνυμό σας για επιβεβαίωση.", false);
            } else {
                appendMessage("Δεν επιλέχθηκε καμία θέση.", false);
                showQuickReplies();
            }
        }
    }

    private void showPerformanceButtons() {
        pendingAction = "selectShow";

        appendMessage("Παρακαλώ επιλέξτε παράσταση", false);

        List<String> shows = Arrays.asList(
                "Οιδίπους Τύραννος",
                "Αντιγόνη",
                "Μήδεια"
        );
        appendButtonMessage(shows);
    }


    private void showTimeButtons(String showName) {
        pendingAction = "selectTime";
        tempBooking   = showName;

        appendMessage("Παρακαλώ επιλέξτε ώρα", false);

        List<String> times;
        switch (showName) {
            case "Αντιγόνη":
                times = Arrays.asList("17:30", "20:30");
                break;
            case "Οιδίπους Τύραννος":
                times = Arrays.asList("18:00", "21:00");
                break;
            case "Μήδεια":
                times = Arrays.asList("20:00");
                break;
            default:
                times = Collections.emptyList();
        }
        appendButtonMessage(times);
    }

    private void showScheduleAll() {
        appendMessage(
                "🎭 Πρόγραμμα αυτή την εβδομάδα:\n" +
                        "- Οιδίπους Τύραννος: Δευ 18:00, Τετ 21:00\n" +
                        "- Αντιγόνη: Τρι 17:30, Πεμ 20:30\n" +
                        "- Μήδεια: Σαβ 20:00",
                false
        );
    }


    private void handleMessage() {

        if (STATE_ENTER_NAME.equals(pendingAction)) {
            String fullName = inputField.getText().toString().trim();
            if (fullName.isEmpty()) return;
            String confirmedBooking = tempBooking + " - Όνομα: " + fullName;
            prefs.edit().putString("latestBooking", confirmedBooking).apply();
            appendMessage("✅ Η κράτηση σας επιβεβαιώθηκε για: " + confirmedBooking, false);

            // Reset state
            pendingAction = null;
            tempBooking = null;
            inputField.setText("");
            showQuickReplies();
            return;
        }
        String userMessage = inputField.getText().toString().trim();
        if (userMessage.isEmpty()) return;

        appendMessage(userMessage, true);
        inputField.setText("");

        callWitAI(userMessage);
    }

    private void callWitAI(String userInput) {
        witService.getMessage(WIT_VERSION, userInput, WIT_TOKEN)
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
                            showQuickReplies();
                        }
                    }

                    @Override
                    public void onFailure(Call<WitResponse> call, Throwable t) {
                        Log.e(TAG, "Wit.ai call failed", t);
                        appendMessage("⚠️ Αποτυχία σύνδεσης με Wit.ai", false);
                        showQuickReplies();
                    }
                });
    }

private void handleWitIntent(String intent, Map<String, List<WitResponse.Entity>> entities) {
    switch (intent) {
        case "seeSchedule":
            if (entities.containsKey("wit$datetime:datetime")) {
                String date = entities.get("wit$datetime:datetime").get(0).values.get(0).value;
                showScheduleFor(date);
            } else {
                showScheduleAll();
            }
            break;
        case "makeReservation":
            showPerformanceButtons();
            return;
        case "cancelReservation":
            appendMessage("Παρακαλώ δώστε τον κωδικό κράτησης για ακύρωση.", false);
            break;
        case "contactStaff":
            appendMessage("Παρακαλώ επιλέξτε υπάλληλο: ταμείο ή διοίκηση.", false);
            break;
        default:
            appendMessage("Δεν σε κατάλαβα, δοκίμασε ξανά.", false);
    }
    // μετά από κάθε bot response, εμφάνισε ξανά επιλογές
    showQuickReplies();
}


//    private void resetConfirmationState() {
//        pendingAction = null;
//        tempBooking = null;
//    }

    private void showScheduleFor(String date) {
        appendMessage(String.format(Locale.getDefault(),
                "🎭 Πρόγραμμα για %s:\n- Αντιγόνη: 17:30 & 20:30", date), false);
    }

    private void doReservation(String showName, int count, String date) {
        String booking = String.format(Locale.getDefault(),
                "%s στις %s, %d εισιτήρια", showName, date, count);
        // Απλή αποθήκευση
        prefs.edit().putString("latestBooking", booking).apply();
        appendMessage("Η κράτησή σας: " + booking, false);
    }

    private void cancelReservation(String reservationId) {
        prefs.edit().remove("latestBooking").apply();
        appendMessage("Ακύρωση κράτησης: " + reservationId, false);
    }

    private void showContactStaff(String dept) {
        switch (dept.toLowerCase(Locale.getDefault())) {
            case "ταμείο":
                appendMessage("📞 Ταμείο: 210-1234567", false);
                break;
            case "διοίκηση":
                appendMessage("📧 Διοίκηση: admin@theater.gr", false);
                break;
            default:
                appendMessage("📞 Τηλέφωνο γενικής πληροφόρησης: 210-7654321", false);
        }
    }

    private void appendMessage(String text, boolean isUser) {
        messageList.add(new ChatMessage(text, isUser));
        adapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }
}

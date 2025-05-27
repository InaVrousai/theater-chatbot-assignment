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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
    private final String WIT_VERSION = "20250527";
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

        appendMessage("Πώς μπορώ να βοηθήσω; Πες μου τι χρειάζεσαι ή πληκτρολόγησε τον αριθμό", false);
        appendMessage("Διαθέσιμες υπηρεσίες:\n\n" +
                "1\uFE0F⃣Παίζουν τώρα\n" +
                "2\uFE0F⃣ Αγορά εισητηρίων\n" +
                "3\uFE0F⃣ Ακύρωση αγοράς\n" +
                "4\uFE0F⃣ Επικοινωνήστε με κάποιον βοηθό", false);

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
            witService.getMessage(WIT_VERSION, userInput, WIT_TOKEN)
                    .enqueue(new Callback<WitResponse>() {
                        @Override
                        public void onResponse(Call<WitResponse> call, Response<WitResponse> resp) {
                            if (resp.isSuccessful() && resp.body() != null) {
                                // number input matching menu available services
                                if (userInput.matches("\\d+") && NumberInMenuServices(Integer.parseInt(userInput))) {
                                    int num = Integer.parseInt(userInput);
                                    String intent = mapNumberToIntent(num);
                                    handleWitIntent(intent, Collections.emptyMap());
                                } else {
                                    WitResponse wr = resp.body();
                                    String intent = wr.intents.isEmpty()
                                            ? "none"
                                            : wr.intents.get(0).name;
                                    handleWitIntent(intent, wr.entities);
                                }
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
        if (entities == null) {String bre = "brr";}

        switch (intent) {
            case "seeSchedule": {
                // Ελέγχουμε εάν έχουμε date entity
                if (entities.containsKey("wit$datetime:datetime")) {
                    String date = entities.get("wit$datetime:datetime").get(0).values.get(0).value;
                    showScheduleFor(date);
                } else {
                    showScheduleAll();
                }
                break;
            }
            case "makeReservation": {
                String date = null, showName = null;
                Integer count = null;
                if (entities.containsKey("wit$datetime:datetime")) {
                    date = entities.get("wit$datetime:datetime").get(0).values.get(0).value;
                }
                if (entities.containsKey("show_name:show_name")) {
                    showName = entities.get("show_name:show_name").get(0).body;
                }
                if (entities.containsKey("wit$number:number")) {
                    count = Integer.valueOf(entities.get("wit$number:number").get(0).body);
                }
                if (date != null && showName != null && count != null) {
                    doReservation(showName, count, date);
                } else {
                    appendMessage("Για την κράτηση, πες μου τίτλο παράστασης, ημερομηνία και αριθμό εισιτηρίων.", false);
                }
                break;
            }
            case "cancelReservation": {
                if (entities.containsKey("reservation:reservation")) {
                    String resId = entities.get("reservation:reservation").get(0).body;
                    cancelReservation(resId);
                } else {
                    appendMessage("Δώσε τον κωδικό της κράτησης που θες να ακυρώσεις.", false);
                }
                break;
            }
            case "contactStaff": {
                if (entities.containsKey("employee:employee")) {
                    String dept = entities.get("employee:employee").get(0).body;
                    showContactStaff(dept);
                } else {
                    appendMessage("Ποιον υπεύθυνο θες να επικοινωνήσεις; (π.χ. ταμείο, διοίκηση)", false);
                }
                break;
            }
            default:
                appendMessage("Δεν σε κατάλαβα, δοκίμασε ξανά.", false);
        }
    }

    private void resetConfirmationState() {
        pendingAction = null;
        tempBooking = null;
    }

    private void showScheduleAll() {
        appendMessage("🎭 Το πρόγραμμα των παραστάσεων για αυτή την εβδομάδα:\n" +
                "- Οιδίπους Τύραννος: Δευ 18:00, Τετ 21:00\n" +
                "- Αντιγόνη: Τρι 17:30, Πεμ 20:30\n" +
                "- Μήδεια: Σαβ 20:00", false);
    }

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

    private String mapNumberToIntent(Integer num) {
        String intent = "";
        switch(num) {
            case 1:
                intent = "seeSchedule";
                break;
            case 2:
                intent = "makeReservation";
                break;
            case 3:
                intent = "cancelReservation";
                break;
            case 4:
                intent = "contactStaff";
                break;
        }
        return intent;
    }

    private boolean NumberInMenuServices(int number) {
        int[] menuServices = new int[] {1, 2, 3, 4};

        for (int num : menuServices) {
            if (num == number) return true;
        }
        return false;
    }
}



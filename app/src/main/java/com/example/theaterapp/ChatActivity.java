package com.example.theaterapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ImageButton;
import android.widget.Toast;

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
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;


import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";

    private static final String EMPLOYEE_PHONE_NUMBER = "2101234567";
    private static final String STATE_ENTER_NAME = "enterName";
    private static final int REQUEST_CODE_SELECT_SEATS = 1001;
    private static final int TOTAL_SEATS = 30;
    private static final String STATE_ENTER_COUNT = "enterCount";
    private int ticketCount = 0;

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

    private List<Reservation> reservations = new ArrayList<>();
    private final String RES_FILE = "reservations.json";

    private String selectedShowName;
    private String selectedShowTime;
    private List<String> selectedSeatsList = new ArrayList<>();

    private LinearLayout quickRepliesLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        inputField = findViewById(R.id.inputField);
        sendButton = findViewById(R.id.sendButton);

        ImageButton walletButton = findViewById(R.id.walletButton);
        walletButton.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, WalletActivity.class);
            startActivity(intent);
        });


        chatRecyclerView = findViewById(R.id.chatRecycler);
        quickRepliesLayout = findViewById(R.id.quickRepliesLayout);
        prefs = getSharedPreferences("Bookings", MODE_PRIVATE);

        adapter = new ChatAdapter(messageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(adapter);
        Log.d("ChatActivity", "Internal files dir = " + getFilesDir().getAbsolutePath());


        loadReservations();


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

    private void loadReservations() {
        try {
            File f = new File(getFilesDir(), RES_FILE);
            if (!f.exists()) return;
            FileReader reader = new FileReader(f);
            Type listType = new TypeToken<List<Reservation>>(){}.getType();
            reservations = new Gson().fromJson(reader, listType);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveReservations() {
        try {
            File f = new File(getFilesDir(), RES_FILE);
            FileWriter writer = new FileWriter(f, false);
            new Gson().toJson(reservations, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                    Log.d(TAG, "Πατήθηκε ‘Επικοινωνία με υπάλληλο’ → δοκιμή startActivity χωρίς resolveActivity");
                    Toast.makeText(ChatActivity.this, "Προσπάθεια να ανοίξω το Dialer…", Toast.LENGTH_SHORT).show();

                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + EMPLOYEE_PHONE_NUMBER));
                    try {
                        startActivity(dialIntent);
                    } catch (android.content.ActivityNotFoundException ex) {
                        // Αν πάλι δεν υπάρχει κάποια εφαρμογή να το αναλάβει:
                        Log.e(TAG, "ActivityNotFoundException για ACTION_DIAL", ex);
                        Toast.makeText(this, "Δεν βρέθηκε τηλεφωνική εφαρμογή (Exception)", Toast.LENGTH_LONG).show();
                    }
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
                selectedShowName = text;
                showTimeButtons(text);
                break;

            case "selectTime":
                selectedShowTime = text;
                pendingAction = STATE_ENTER_COUNT;
                tempBooking = selectedShowName + " στις " + selectedShowTime;
                appendMessage("Πόσα εισιτήρια θέλετε;", false);
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


    private void startSeatSelection(int maxSeats) {

        List<String> occupied = new ArrayList<>();
        for (Reservation r : reservations) {
            if (r.showName.equals(selectedShowName) &&
                    r.showTime.equals(selectedShowTime)) {
                occupied.addAll(r.seats);
            }
        }

        Intent intent = new Intent(this, SeatSelectionActivity.class);
        intent.putExtra("showName", selectedShowName);
        intent.putExtra("showTime", selectedShowTime);
        intent.putExtra("maxSeats", maxSeats);
        intent.putStringArrayListExtra("reservedSeats",
                new ArrayList<>(occupied));
        startActivityForResult(intent, REQUEST_CODE_SELECT_SEATS);


        pendingAction = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_SEATS && resultCode == RESULT_OK && data != null) {
            String[] seats = data.getStringArrayExtra("selectedSeats");
            selectedSeatsList = seats != null
                    ? Arrays.asList(seats)
                    : Collections.emptyList();
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



    protected void handleMessage() {
        String text = inputField.getText().toString().trim();
        if (text.isEmpty()) return;


        if (STATE_ENTER_COUNT.equals(pendingAction)) {
            try {
                int requested = Integer.parseInt(text);

                // Υπολογίζουμε πόσες θέσεις είναι ήδη κατειλημμένες για αυτό το show+time
                int occupied = 0;
                for (Reservation r : reservations) {
                    if (r.showName.equals(selectedShowName)
                            && r.showTime.equals(selectedShowTime)) {
                        occupied += r.seats.size();
                    }
                }
                int available = TOTAL_SEATS - occupied;

                if (requested > available) {
                    appendMessage(
                            "❌ Διαθέσιμες μόνο " + available +
                                    " θέσεις για αυτή την παράσταση/ώρα.\n" +
                                    "Παρακαλώ δώστε νέο αριθμό εισιτηρίων.",
                            false
                    );
                    return;
                }

                // ΟΚ, αποθηκεύουμε το έγκυρο αίτημα
                ticketCount = requested;
                appendMessage(
                        "Επιβεβαιώσατε " + ticketCount + " εισιτήρια.",
                        false
                );
                inputField.setText("");

                // ξεκινάμε επιλογή καθισμάτων
                startSeatSelection(ticketCount);

            } catch (NumberFormatException e) {
                appendMessage("Παρακαλώ εισάγετε έναν έγκυρο αριθμό.", false);
            }
            return;
        }

        // 2) Αν περιμένουμε το όνομα για επιβεβαίωση
        if (STATE_ENTER_NAME.equals(pendingAction)) {
            String fullName = text;

            String bookingId = UUID.randomUUID()
                    .toString()
                    .substring(0, 8)
                    .toUpperCase();

            Reservation r = new Reservation(
                    bookingId,
                    selectedShowName,
                    selectedShowTime,
                    ticketCount,
                    selectedSeatsList,
                    fullName
            );

            // προσθήκη & αποθήκευση
            reservations.add(r);
            saveReservations();

            String confirmed = String.format(
                    "%s στις %s\nΘέσεις: %s\nΌνομα: %s\nΚωδικός: %s",
                    selectedShowName,
                    selectedShowTime,
                    String.join(", ", selectedSeatsList),
                    fullName,
                    bookingId
            );
//            prefs.edit()
//                    .putString("latestBooking", confirmed)
//                    .putString("latestBookingId", bookingId)
//                    .apply();
            appendMessage("✅ Η κράτησή σας επιβεβαιώθηκε:\n" + confirmed, false);

            // επαναφορά
            pendingAction = null;
            tempBooking   = null;
            inputField.setText("");
            showQuickReplies();
            return;
        }

        pendingAction = null;
        tempBooking   = null;
        selectedSeatsList.clear();
        inputField.setText("");
        showQuickReplies();
        return;
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

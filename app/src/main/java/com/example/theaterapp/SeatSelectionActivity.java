package com.example.theaterapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SeatSelectionActivity extends AppCompatActivity {
    private GridLayout seatGrid;
    private Button confirmBtn;
    private List<String> selectedSeats = new ArrayList<>();
    private List<String> reservedSeats = new ArrayList<>();
    private int maxSeats;


    private final List<String> allSeats = Arrays.asList(
            "A1","A2","A3","A4","A5","A6",
            "B1","B2","B3","B4","B5","B6",
            "C1","C2","C3","C4","C5","C6",
            "D1","D2","D3","D4","D5","D6",
            "E1","E2","E3","E4","E5","E6"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seat_selection);

        maxSeats = getIntent().getIntExtra("maxSeats", 1);




        maxSeats      = getIntent().getIntExtra("maxSeats", 1);
        reservedSeats = getIntent().getStringArrayListExtra("reservedSeats");
        if (reservedSeats == null) reservedSeats = new ArrayList<>();

        seatGrid   = findViewById(R.id.seatGrid);
        confirmBtn = findViewById(R.id.confirmButton);
        confirmBtn.setEnabled(false);


        int colCount = 6;
        seatGrid.setColumnCount(colCount);

        float density = getResources().getDisplayMetrics().density;
        int sizePx = (int)(48 * density + .5f); // ή κάνε reference σε R.dimen.seat_size

        for (int i = 0; i < allSeats.size(); i++) {
            String code = allSeats.get(i);
            Button btn = new Button(this);
            btn.setText(code);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            btn.setAllCaps(false);

            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width  = getResources().getDimensionPixelSize(R.dimen.seat_size);
            p.height = p.width;
            p.columnSpec = GridLayout.spec(i % colCount);
            p.rowSpec    = GridLayout.spec(i / colCount);
            p.setMargins(8,8,8,8);
            btn.setLayoutParams(p);

            if (reservedSeats.contains(code)) {
                btn.setEnabled(false);
                btn.setAlpha(0.3f);
            }


            btn.setOnClickListener(v -> {
                if (!btn.isEnabled()) return;
                if (selectedSeats.contains(code)) {
                    selectedSeats.remove(code);
                    btn.setAlpha(1f);
                } else if (selectedSeats.size() < maxSeats) {
                    selectedSeats.add(code);
                    btn.setAlpha(0.5f);
                } else {
                    Toast.makeText(this,
                            "Μπορείτε να επιλέξετε έως " + maxSeats + " θέσεις",
                            Toast.LENGTH_SHORT).show();
                }
                confirmBtn.setEnabled(selectedSeats.size() == maxSeats);
            });
            seatGrid.addView(btn);
        }

        confirmBtn.setOnClickListener(v -> {
            Intent data = new Intent();
            data.putExtra("selectedSeats",
                    selectedSeats.toArray(new String[0]));
            setResult(RESULT_OK, data);
            finish();
        });
    }
}

package com.example.theaterapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.theaterapp.api.ApiMessage;
import com.example.theaterapp.api.AiService;
import com.example.theaterapp.rag.RetrievalService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {

    EditText inputField;
    Button sendButton;
    private RecyclerView chatRecycler;
    private List<ChatMessage> messages;
    private ChatAdapter adapter;

    private AiService aiService;
    private ExecutorService executor;
    private Handler mainHandler;
    private RetrievalService retrieval;



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

        // initialize AI service & RAG
        aiService   = new AiService(BuildConfig.OPENAI_API_KEY);
        retrieval   = new RetrievalService(this, aiService);

        // threading handler
        executor    = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // initial greeting
        appendMessage("Πώς μπορώ να βοηθήσω;", false);

        // send button
        sendButton.setOnClickListener(v -> handleMessage());
    }

    private void handleMessage() {
        String userMessage = inputField.getText().toString().trim();
        if (userMessage.isEmpty()) return;

        appendMessage(userMessage, true);
        inputField.setText("");
        sendButton.setEnabled(false);

        List<ApiMessage> apiHistory = new ArrayList<>();
        for (ChatMessage cm : messages) {
            String role = cm.isUser() ? "user" : "assistant";
            apiHistory.add(new ApiMessage(role, cm.getText()));
        }

        generateResponse(apiHistory);
    }
    private void generateResponse(List<ApiMessage> apiHistory) {
        executor.execute(() -> {
            try {
                String aiReply = aiService.getChatCompletion(apiHistory);
                mainHandler.post(() -> {
                    appendMessage(aiReply, false);
                    sendButton.setEnabled(true);
                });
            } catch (Exception e) {
                // Log full stack to Logcat
                Log.e("ChatActivity", "Chat API error", e);

                // Show the exception message in‐chat for debugging
                String errorMsg = e.getMessage();
                mainHandler.post(() -> {
                    appendMessage("Chat API failed: " + errorMsg, false);
                    sendButton.setEnabled(true);
                });
            }
        });
    }




//    private void generateResponse(List<ApiMessage> apiHistory) {
//        executor.execute(() -> {
//            try {
//                // 1. ask RetrievalService for top 3 contexts
//                String userText = apiHistory.get(apiHistory.size() - 1).getContent();
//                List<String> contexts = retrieval.getTopContexts(userText, 3);
//                // 2. call chatWithRetrieval
//                String aiReply = aiService.chatWithRetrieval(apiHistory, contexts);
//                mainHandler.post(() -> {
//                    appendMessage(aiReply, false);
//                    sendButton.setEnabled(true);
//                });
//            } catch (Exception e) {
//                Log.e("ChatActivity", "Error in generateResponse", e);
//                mainHandler.post(() -> {
//                    appendMessage("Συγγνώμη, κάτι πήγε λάθος.", false);
//                    sendButton.setEnabled(true);
//                });
//            }
//        });
//    }

    private void appendMessage(String text, boolean isUser) {
        messages.add(new ChatMessage(text, isUser));
        adapter.notifyItemInserted(messages.size() - 1);
        chatRecycler.scrollToPosition(messages.size() - 1);
    }

}
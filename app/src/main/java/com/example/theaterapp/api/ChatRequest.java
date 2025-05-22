package com.example.theaterapp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatRequest {
    @SerializedName("model")
    private final String model;

    @SerializedName("messages")
    private final List<ApiMessage> messages;

    public ChatRequest(String model, List<ApiMessage> messages) {
        this.model = model;
        this.messages = messages;
    }

    public String getModel() { return model; }
    public List<ApiMessage> getMessages() { return messages; }
}
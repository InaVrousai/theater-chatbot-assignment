package com.example.theaterapp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatResponse {
    @SerializedName("choices")
    private List<Choice> choices;

    public List<Choice> getChoices() { return choices; }

    public static class Choice {
        @SerializedName("message")
        private ApiMessage message;

        public ApiMessage getMessage() { return message; }
    }
}

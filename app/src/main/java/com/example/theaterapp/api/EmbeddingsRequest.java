package com.example.theaterapp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class EmbeddingsRequest {
    @SerializedName("model")
    private final String model;

    @SerializedName("input")
    private final List<String> input;

    public EmbeddingsRequest(String model, String text) {
        this.model = model;
        this.input = List.of(text);
    }
    public String getModel() { return model; }
    public List<String> getInput() { return input; }
}


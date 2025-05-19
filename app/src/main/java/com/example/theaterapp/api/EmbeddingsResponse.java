package com.example.theaterapp.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class EmbeddingsResponse {
    @SerializedName("data")
    private List<EmbeddingData> data;

    public List<EmbeddingData> getData() { return data; }

    public static class EmbeddingData {
        @SerializedName("embedding")
        private List<Float> embedding;

        public List<Float> getEmbedding() { return embedding; }
    }
}

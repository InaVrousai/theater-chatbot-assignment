package com.example.theaterapp.rag;

import android.content.Context;

import com.example.theaterapp.api.AiService;
import com.example.theaterapp.api.ApiMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RetrievalService {
    private final List<PlayInfo> catalog;
    private final AiService aiService;
    private final Context context;

    public RetrievalService(Context context, AiService aiService) {
        this.context = context;
        this.aiService = aiService;
        this.catalog = loadCatalogFromAssets();
    }

    /**
     * Load the play_embeddings.json from assets and parse into PlayInfo objects.
     */
    private List<PlayInfo> loadCatalogFromAssets() {
        try (InputStream is = context.getAssets().open("play_embeddings.json");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            Gson gson = new Gson();
            Type listType = new TypeToken<List<PlayInfo>>() {}.getType();
            return gson.fromJson(reader, listType);

        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Compute cosine similarity between two float vectors.
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length && i < b.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Returns the top-K most relevant play contexts (title + synopsis)
     * for a given user question.
     */
    public List<String> getTopContexts(String userQuestion, int topK) throws Exception {
        // 1. embed the user question
        float[] qEmb = aiService.getEmbedding(userQuestion);

        // 2. score each play by cosine similarity
        return catalog.stream()
                .map(play -> new Object[]{ play, cosineSimilarity(play.getEmbedding(), qEmb) })
                // sort descending by similarity
                .sorted((o1, o2) -> Double.compare((Double)o2[1], (Double)o1[1]))
                .limit(topK)
                .map(o -> {
                    PlayInfo p = (PlayInfo) o[0];
                    return p.getTitle() + ": " + p.getSynopsis();
                })
                .collect(Collectors.toList());
    }
}

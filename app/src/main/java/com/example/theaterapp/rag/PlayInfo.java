package com.example.theaterapp.rag;

import java.util.List;

public class PlayInfo {
    private String playId;
    private String title;
    private String synopsis;
    private List<String> times;
    private String price;
    private float[] embedding;

    public PlayInfo(String playId, String title, String synopsis,
                    List<String> times, String price, float[] embedding) {
        this.playId = playId;
        this.title = title;
        this.synopsis = synopsis;
        this.times = times;
        this.price = price;
        this.embedding = embedding;
    }

    public String getTitle() { return title; }
    public String getSynopsis() { return synopsis; }
    public List<String> getTimes() { return times; }
    public String getPrice() { return price; }
    public float[] getEmbedding() { return embedding; }
}
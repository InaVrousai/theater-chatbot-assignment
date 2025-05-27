package com.example.theaterapp.api;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface WitService {
    @GET("message")
    Call<WitResponse> getMessage(
            @Query("v")   String version,        // e.g. "20230608"
            @Query("q")   String q,              // το user μήνυμα
            @Header("Authorization") String auth // "Bearer {WIT_TOKEN}"
    );

    // factory method για εύκολη δημιουργία instance
    static WitService create(String witToken) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.wit.ai/")                   // Base URL
                .addConverterFactory(GsonConverterFactory.create()) // JSON ↔ POJO
                .build();

        return retrofit.create(WitService.class);
    }
}

package com.example.theaterapp.api;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
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
}

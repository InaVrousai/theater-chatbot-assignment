package com.example.theaterapp.api;

import com.example.theaterapp.LlamaRequest;
import com.example.theaterapp.LlamaResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface LlamaService {
    @POST("models/google/flan-t5-small")
    Call<LlamaResponse> getChatCompletion(
            @Header("Authorization") String authHeader,
            @Body LlamaRequest request
    );
}

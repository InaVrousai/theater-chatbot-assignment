package com.example.theaterapp.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface OpenAiApi {
    @Headers({
            "Content-Type: application/json"
    })
    @POST("v1/chat/completions")
    Call<ChatResponse> createChat(@Body ChatRequest request);

    @Headers({"Content-Type: application/json"})
    @POST("v1/embeddings")
    Call<EmbeddingsResponse> createEmbeddings(@Body EmbeddingsRequest request);
}
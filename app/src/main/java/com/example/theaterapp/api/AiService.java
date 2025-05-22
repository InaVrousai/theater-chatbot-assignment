package com.example.theaterapp.api;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AiService {
    private static final String BASE_URL = "https://api.openai.com/";
    private final OpenAiApi api;
    private final String systemPrompt =
            "You are Athena, the official assistant for the theater. " +
                    "Answer user queries about shows, bookings, cancellations or connecting to staff.";

    public AiService(String apiKey) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("Authorization", "Bearer " + apiKey)
                                .build();
                        return chain.proceed(request);
                    }
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(OpenAiApi.class);
    }

    /**
     * Sends the conversation history (including a system prompt) and returns the AI reply.
     */
    public String getChatCompletion(List<ApiMessage> history) throws IOException {
        List<ApiMessage> payload = new ArrayList<>();
        payload.add(new ApiMessage("system", systemPrompt));
        payload.addAll(history);

        ChatRequest req = new ChatRequest("gpt-3.5-turbo", payload);

        // execute and inspect HTTP status
        retrofit2.Response<ChatResponse> raw = api.createChat(req).execute();
        if (!raw.isSuccessful()) {
            String err = raw.errorBody() != null
                    ? raw.errorBody().string()
                    : "no errorBody";
            throw new IOException("HTTP " + raw.code() + ": " + err);
        }

        ChatResponse resp = raw.body();
        // dump the entire resp for debugging
        if (resp == null) {
            throw new IOException("Empty response body");
        }
        if (resp.getChoices() == null || resp.getChoices().isEmpty()) {
            // serialize resp to JSON so you can see its shape
            String debug = new com.google.gson.Gson().toJson(resp);
            throw new IOException("Empty choices in response: " + debug);
        }

        return resp.getChoices().get(0).getMessage().getContent();
    }

    public float[] getEmbedding(String text) throws IOException {
        EmbeddingsRequest ereq = new EmbeddingsRequest("text-embedding-3-small", text);
        EmbeddingsResponse eres = api.createEmbeddings(ereq).execute().body();
        if (eres != null && !eres.getData().isEmpty()) {
            List<Float> list = eres.getData().get(0).getEmbedding();
            float[] arr = new float[list.size()];
            for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
            return arr;
        }
        throw new IOException("Embedding failed");
    }

    /**
     * Chat with Retrieval-Augmented Generation (RAG)
     */
    public String chatWithRetrieval(List<ApiMessage> history, List<String> contexts) throws IOException {
        String system = "You are Athena, theater assistant. Use the following context to answer user."
                + "\n\nContext:\n" + String.join("\n---\n", contexts);

        List<ApiMessage> payload = new ArrayList<>();
        payload.add(new ApiMessage("system", system));
        payload.addAll(history);

        ChatRequest req = new ChatRequest("gpt-4o-mini", payload);
        ChatResponse resp = api.createChat(req).execute().body();
        if (resp != null && !resp.getChoices().isEmpty()) {
            return resp.getChoices().get(0).getMessage().getContent();
        }
        throw new IOException("Empty response");
    }
}
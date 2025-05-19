package com.example.theaterapp.api;

import com.google.gson.annotations.SerializedName;

public class ApiMessage {
    @SerializedName("role")
    private final String role;

    @SerializedName("content")
    private final String content;

    public ApiMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() { return role; }
    public String getContent() { return content; }
}
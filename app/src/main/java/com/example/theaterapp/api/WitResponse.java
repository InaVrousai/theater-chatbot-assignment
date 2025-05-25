package com.example.theaterapp.api;

import java.util.List;
import java.util.Map;

public class WitResponse {
    public List<Intent> intents;
    public Map<String, List<Entity>> entities;
    public static class Intent {
        public String name;
        public float confidence;
    }
    public static class Entity {
        public String body;
        public float confidence;
    }
}

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

        public List<Value> values;

        public static class Value {
            public String value;      // ISO format, π.χ. "2025-05-27T00:00:00.000+00:00"
            public String type;       // π.χ. "value"
            public String grain;      // π.χ. "day"
        }
    }
}

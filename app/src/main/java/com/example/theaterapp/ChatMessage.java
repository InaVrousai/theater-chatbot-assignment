// ChatMessage.java
package com.example.theaterapp;

import java.util.List;

public class ChatMessage {
    public enum Type { TEXT, BUTTONS }
    public final Type type;
    public final String text;           // για Type.TEXT
    public final List<String> buttons;  // για Type.BUTTONS
    public final boolean isUser;        // true αν είναι μήνυμα χρήστη

    // constructor για μήνυμα κειμένου με specification ποιος το στέλνει
    public ChatMessage(String text, boolean isUser) {
        this.type = Type.TEXT;
        this.text = text;
        this.buttons = null;
        this.isUser = isUser;
    }

    // helper για bot-text (isUser = false)
    public ChatMessage(String text) {
        this(text, false);
    }

    // constructor για λίστα κουμπιών (πάντα bot)
    public ChatMessage(List<String> buttons) {
        this.type = Type.BUTTONS;
        this.text = null;
        this.buttons = buttons;
        this.isUser = false;
    }
}

package com.example.theaterapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<ChatMessage> messages;

    // View-type constants
    private static final int VIEW_TYPE_USER_TEXT   = 0;
    private static final int VIEW_TYPE_BOT_TEXT    = 1;
    private static final int VIEW_TYPE_BUTTONS     = 2;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messages.get(position);
        if (msg.type == ChatMessage.Type.BUTTONS) {
            return VIEW_TYPE_BUTTONS;
        } else {
            return msg.isUser
                    ? VIEW_TYPE_USER_TEXT
                    : VIEW_TYPE_BOT_TEXT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_USER_TEXT:
                View userView = inflater.inflate(
                        R.layout.item_user_message, parent, false);
                return new TextViewHolder(userView);

            case VIEW_TYPE_BOT_TEXT:
                View botView = inflater.inflate(
                        R.layout.item_bot_message, parent, false);
                return new TextViewHolder(botView);

            case VIEW_TYPE_BUTTONS:
                View btnView = inflater.inflate(
                        R.layout.item_bot_buttons, parent, false);
                return new ButtonsViewHolder(btnView);

            default:
                throw new IllegalStateException(
                        "Unknown viewType " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position) {

        ChatMessage msg = messages.get(position);
        if (holder instanceof TextViewHolder) {
            ((TextViewHolder) holder).bind(msg.text);
        } else if (holder instanceof ButtonsViewHolder) {
            ((ButtonsViewHolder) holder).bind(msg.buttons);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    /** ViewHolder για απλά μηνύματα (user ή bot) **/
    static class TextViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        TextViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
        }

        void bind(String text) {
            messageText.setText(text);
        }
    }

    /** ViewHolder για λίστα κουμπιών (bot) **/
    class ButtonsViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container;

        ButtonsViewHolder(View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.buttonsContainer);
        }

        void bind(List<String> buttons) {
            container.removeAllViews();
            for (String title : buttons) {
                Button btn = new Button(itemView.getContext());
                btn.setText(title);
                btn.setAllCaps(false);
                btn.setOnClickListener(v ->
                        ((ChatActivity) itemView.getContext())
                                .onQuickReplyClicked(title)
                );
                container.addView(btn);
            }
        }
    }
}

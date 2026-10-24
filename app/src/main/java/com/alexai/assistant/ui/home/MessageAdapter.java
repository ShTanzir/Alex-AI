package com.alexai.assistant.ui.home;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.alexai.assistant.R;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {
    private final List<Message> items;
    private final DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT);

    public MessageAdapter(List<Message> items) { this.items = items; }

    static final class VH extends RecyclerView.ViewHolder {
        final TextView text, time;

        VH(View v) {
            super(v);
            text = v.findViewById(R.id.text);
            time = v.findViewById(R.id.time);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Message m = items.get(position);
        h.text.setText(m.text);
        h.time.setText(format.format(new Date(m.timestamp)));
        int gravity = m.fromUser ? Gravity.END : Gravity.START;
        ((LinearLayout.LayoutParams) h.text.getLayoutParams()).gravity = gravity;
        ((LinearLayout.LayoutParams) h.time.getLayoutParams()).gravity = gravity;
        h.text.setBackgroundResource(m.fromUser ? R.drawable.bubble_user : R.drawable.bubble_ai);
        int color = m.fromUser ? R.color.white : (m.error ? R.color.alex_error : R.color.alex_text);
        h.text.setTextColor(ContextCompat.getColor(h.itemView.getContext(), color));
        h.text.requestLayout();
    }

    @Override
    public int getItemCount() { return items.size(); }
}

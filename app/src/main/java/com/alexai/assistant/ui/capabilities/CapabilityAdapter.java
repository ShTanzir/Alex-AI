package com.alexai.assistant.ui.capabilities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.alexai.assistant.R;
import com.alexai.assistant.commands.CapabilityCatalog;

import java.util.ArrayList;
import java.util.List;

public class CapabilityAdapter extends RecyclerView.Adapter<CapabilityAdapter.VH> {
    private final List<CapabilityCatalog.Entry> shown = new ArrayList<>(CapabilityCatalog.all());

    static final class VH extends RecyclerView.ViewHolder {
        final TextView title, status, notes, category;

        VH(View v) {
            super(v);
            title = v.findViewById(R.id.title);
            status = v.findViewById(R.id.status);
            notes = v.findViewById(R.id.notes);
            category = v.findViewById(R.id.category);
        }
    }

    public void filter(String query) {
        shown.clear();
        String q = query == null ? "" : query.trim();
        for (CapabilityCatalog.Entry e : CapabilityCatalog.all()) {
            if (q.isEmpty() || e.matches(q)) shown.add(e);
        }
        notifyDataSetChanged();
    }

    public int size() { return shown.size(); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_capability, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        CapabilityCatalog.Entry e = shown.get(position);
        h.title.setText(e.title);
        h.status.setText(e.status.label);
        h.notes.setText(e.notes);
        h.category.setText(e.category);
        int color;
        switch (e.status) {
            case IMPLEMENTED: color = R.color.alex_ok; break;
            case UNSUPPORTED: color = R.color.alex_error; break;
            default: color = R.color.alex_text_secondary; break;
        }
        h.status.setTextColor(ContextCompat.getColor(h.itemView.getContext(), color));
    }

    @Override
    public int getItemCount() { return shown.size(); }
}

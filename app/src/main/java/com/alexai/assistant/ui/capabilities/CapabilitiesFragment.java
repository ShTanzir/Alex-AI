package com.alexai.assistant.ui.capabilities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alexai.assistant.R;
import com.alexai.assistant.commands.CapabilityCatalog;

public class CapabilitiesFragment extends Fragment {
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle b) {
        return inflater.inflate(R.layout.fragment_capabilities, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final TextView summary = view.findViewById(R.id.summary);
        EditText search = view.findViewById(R.id.search);
        RecyclerView list = view.findViewById(R.id.list);
        final CapabilityAdapter adapter = new CapabilityAdapter();
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        summary.setText(CapabilityCatalog.all().size() + " capabilities: "
                + CapabilityCatalog.count(CapabilityCatalog.Status.IMPLEMENTED) + " working, "
                + CapabilityCatalog.count(CapabilityCatalog.Status.DEVICE_DEPENDENT) + " device-dependent, "
                + CapabilityCatalog.count(CapabilityCatalog.Status.PLANNED) + " planned.");

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { adapter.filter(s.toString()); }
        });
    }
}

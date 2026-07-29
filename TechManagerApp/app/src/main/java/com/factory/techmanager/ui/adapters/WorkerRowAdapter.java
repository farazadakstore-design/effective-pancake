package com.factory.techmanager.ui.adapters;

import android.app.TimePickerDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.factory.techmanager.R;
import com.factory.techmanager.data.Repository;
import com.factory.techmanager.data.ReportWorker;
import com.factory.techmanager.util.Lang;
import com.factory.techmanager.util.StatusLabels;

import java.util.List;
import java.util.Locale;

public class WorkerRowAdapter extends RecyclerView.Adapter<WorkerRowAdapter.VH> {

    public interface Listener {
        void onRemove(int position);
        void onChanged();
    }

    private final List<ReportWorker> items;
    private final Repository repo;
    private final Listener listener;

    public WorkerRowAdapter(List<ReportWorker> items, Repository repo, Listener listener) {
        this.items = items;
        this.repo = repo;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_worker_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ReportWorker w = items.get(position);

        h.etFileNo.removeTextChangedListener(h.fileNoWatcher);
        h.etFileNo.setText(w.fileNo);
        h.fileNoWatcher = simpleWatcher(s -> { int p = h.getBindingAdapterPosition(); if (p != RecyclerView.NO_POSITION) items.get(p).fileNo = s; });
        h.etFileNo.addTextChangedListener(h.fileNoWatcher);

        h.etName.removeTextChangedListener(h.nameWatcher);
        h.etName.setText(w.name);
        h.etName.setHint(Lang.t("الاسم", "Name"));
        h.nameWatcher = simpleWatcher(s -> { int p = h.getBindingAdapterPosition(); if (p != RecyclerView.NO_POSITION) { items.get(p).name = s; items.get(p).techId = null; } });
        h.etName.addTextChangedListener(h.nameWatcher);

        h.etContract.removeTextChangedListener(h.contractWatcher);
        h.etContract.setText(w.contractNo);
        h.contractWatcher = simpleWatcher(s -> { int p = h.getBindingAdapterPosition(); if (p != RecyclerView.NO_POSITION) items.get(p).contractNo = s; });
        h.etContract.addTextChangedListener(h.contractWatcher);

        String[] keys = StatusLabels.REPORT_STATUS_KEYS;
        String[] labels = new String[keys.length];
        for (int i = 0; i < keys.length; i++) labels[i] = StatusLabels.label(keys[i]);
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(h.itemView.getContext(), android.R.layout.simple_spinner_item, labels);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        h.spStatus.setAdapter(statusAdapter);
        int sel = 0;
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(w.status)) sel = i;
        h.spStatus.setSelection(sel, false);
        h.spStatus.setOnTouchListener((v, e) -> { h.statusTouched = true; return false; });
        h.spStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (h.statusTouched) {
                    int p = h.getBindingAdapterPosition();
                    if (p != RecyclerView.NO_POSITION) items.get(p).status = keys[pos];
                    h.statusTouched = false;
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        h.btnCheckIn.setText((w.checkIn == null || w.checkIn.isEmpty()) ? Lang.t("بداية", "Start") : w.checkIn);
        h.btnCheckOut.setText((w.checkOut == null || w.checkOut.isEmpty()) ? Lang.t("نهاية", "End") : w.checkOut);
        h.tvHours.setText(w.hours == null ? "" : w.hours);

        h.btnCheckIn.setOnClickListener(v -> pickTime(h, true, w));
        h.btnCheckOut.setOnClickListener(v -> pickTime(h, false, w));
        h.btnRemove.setOnClickListener(v -> {
            int p = h.getBindingAdapterPosition();
            if (p != RecyclerView.NO_POSITION) listener.onRemove(p);
        });
    }

    private void pickTime(VH h, boolean isCheckIn, ReportWorker w) {
        String current = isCheckIn ? w.checkIn : w.checkOut;
        int hh = 6, mm = 30;
        if (current != null && current.contains(":")) {
            try {
                String[] parts = current.split(":");
                hh = Integer.parseInt(parts[0]);
                mm = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}
        }
        new TimePickerDialog(h.itemView.getContext(), (view, hour, minute) -> {
            String time = String.format(Locale.US, "%02d:%02d", hour, minute);
            int p = h.getBindingAdapterPosition();
            if (p == RecyclerView.NO_POSITION) return;
            ReportWorker rw = items.get(p);
            if (isCheckIn) rw.checkIn = time; else rw.checkOut = time;
            rw.hours = repo.calcHours(rw.checkIn, rw.checkOut);
            notifyItemChanged(p);
            listener.onChanged();
        }, hh, mm, true).show();
    }

    private TextWatcher simpleWatcher(java.util.function.Consumer<String> onChange) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { onChange.accept(s.toString()); }
        };
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        EditText etFileNo, etName, etContract;
        Spinner spStatus;
        Button btnCheckIn, btnCheckOut;
        TextView tvHours;
        ImageButton btnRemove;
        TextWatcher fileNoWatcher, nameWatcher, contractWatcher;
        boolean statusTouched = false;

        VH(@NonNull View itemView) {
            super(itemView);
            etFileNo = itemView.findViewById(R.id.etFileNo);
            etName = itemView.findViewById(R.id.etName);
            etContract = itemView.findViewById(R.id.etContract);
            spStatus = itemView.findViewById(R.id.spStatus);
            btnCheckIn = itemView.findViewById(R.id.btnCheckIn);
            btnCheckOut = itemView.findViewById(R.id.btnCheckOut);
            tvHours = itemView.findViewById(R.id.tvHours);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}

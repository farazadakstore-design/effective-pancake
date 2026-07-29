package com.factory.techmanager.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.factory.techmanager.R;
import com.factory.techmanager.data.Department;
import com.factory.techmanager.data.Seed;
import com.factory.techmanager.data.Technician;
import com.factory.techmanager.util.Lang;

import java.util.List;

public class ExtraTechAdapter extends RecyclerView.Adapter<ExtraTechAdapter.VH> {

    public interface Listener {
        void onDelete(Technician t);
    }

    private final List<Technician> items;
    private final Listener listener;

    public ExtraTechAdapter(List<Technician> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_extra_tech, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Technician t = items.get(position);
        Department d = Seed.findDept(t.group);
        String dLabel = d != null ? d.label(Lang.isArabic()) : t.group;
        h.tvName.setText(t.displayName(Lang.isArabic()));
        h.tvSub.setText(t.fileNo + " · " + dLabel + (t.nat != null && !t.nat.isEmpty() ? " · " + t.nat : ""));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(t));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvSub;
        android.widget.Button btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvSub = itemView.findViewById(R.id.tvSub);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

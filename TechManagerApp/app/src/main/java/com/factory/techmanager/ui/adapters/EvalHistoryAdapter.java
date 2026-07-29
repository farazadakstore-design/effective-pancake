package com.factory.techmanager.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.factory.techmanager.R;
import com.factory.techmanager.data.Department;
import com.factory.techmanager.data.Evaluation;
import com.factory.techmanager.data.Seed;
import com.factory.techmanager.util.Lang;

import java.util.List;

public class EvalHistoryAdapter extends RecyclerView.Adapter<EvalHistoryAdapter.VH> {

    public interface Listener {
        void onDelete(Evaluation e);
    }

    private final List<Evaluation> items;
    private final boolean showDept;
    private final boolean showDelete;
    private final Listener listener;

    public EvalHistoryAdapter(List<Evaluation> items, boolean showDept, boolean showDelete, Listener listener) {
        this.items = items;
        this.showDept = showDept;
        this.showDelete = showDelete;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_eval_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Evaluation e = items.get(position);
        Department d = Seed.findDept(e.deptId);
        String dname = d != null ? d.label(Lang.isArabic()) : e.deptId;
        String name = (e.fileNo == null ? "" : e.fileNo) + " - " + e.displayName(Lang.isArabic()) + (showDept ? " · " + dname : "");
        h.tvTitle.setText(name);
        h.tvSub.setText(e.month + (e.note != null && !e.note.isEmpty() ? " · " + e.note : ""));
        h.tvScore.setText(e.score + "/10");
        h.tvScore.setTextColor(Color.parseColor(e.score >= 8 ? "#1A7A47" : e.score >= 5 ? "#7D6608" : "#C0392B"));
        h.btnDelete.setVisibility(showDelete ? View.VISIBLE : View.GONE);
        h.btnDelete.setOnClickListener(v -> listener.onDelete(e));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSub, tvScore;
        android.widget.Button btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSub = itemView.findViewById(R.id.tvSub);
            tvScore = itemView.findViewById(R.id.tvScore);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

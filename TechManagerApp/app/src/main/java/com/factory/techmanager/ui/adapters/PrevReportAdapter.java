package com.factory.techmanager.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.factory.techmanager.R;
import com.factory.techmanager.data.Department;
import com.factory.techmanager.data.Report;
import com.factory.techmanager.data.Seed;
import com.factory.techmanager.util.Lang;

import java.util.List;

public class PrevReportAdapter extends RecyclerView.Adapter<PrevReportAdapter.VH> {

    public interface Listener {
        void onView(Report r);
        void onDelete(Report r);
    }

    private final List<Report> items;
    private final Listener listener;
    private final boolean showDelete;

    public PrevReportAdapter(List<Report> items, Listener listener, boolean showDelete) {
        this.items = items;
        this.listener = listener;
        this.showDelete = showDelete;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prev_report, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Report r = items.get(position);
        Department d = Seed.findDept(r.deptId);
        String dname = d != null ? d.label(Lang.isArabic()) : r.deptId;
        String title = (r.reportNo != null && !r.reportNo.isEmpty() ? "[" + r.reportNo + "] " : "") + r.date + " · " + dname;
        h.tvTitle.setText(title);
        h.tvSub.setText(r.workers.size() + " " + Lang.t("عامل", "workers") + " · " + (r.section == null || r.section.isEmpty() ? "—" : r.section));
        h.btnView.setOnClickListener(v -> listener.onView(r));
        h.btnDelete.setVisibility(showDelete ? View.VISIBLE : View.GONE);
        h.btnDelete.setOnClickListener(v -> listener.onDelete(r));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSub;
        android.widget.Button btnView, btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSub = itemView.findViewById(R.id.tvSub);
            btnView = itemView.findViewById(R.id.btnView);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

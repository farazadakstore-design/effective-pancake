package com.factory.techmanager.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.factory.techmanager.R;
import com.factory.techmanager.data.Evaluation;
import com.factory.techmanager.data.Technician;
import com.factory.techmanager.util.Lang;

import java.util.List;

/** Rows are either a String (section header) or a Technician; each technician row exposes a
 *  score Spinner (tag R.id.spScore) and note EditText, read back directly by the fragment on save. */
public class EvalRowAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ROW = 1;

    private final List<Object> rows;
    private final java.util.Map<String, Evaluation> existing;

    public EvalRowAdapter(List<Object> rows, java.util.Map<String, Evaluation> existingForMonth) {
        this.rows = rows;
        this.existing = existingForMonth;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position) instanceof String ? TYPE_HEADER : TYPE_ROW;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            TextView tv = new TextView(parent.getContext());
            tv.setTextSize(12);
            tv.setTextColor(parent.getContext().getColor(R.color.muted));
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            int pad = Math.round(4 * parent.getResources().getDisplayMetrics().density);
            tv.setPadding(pad, Math.round(14 * parent.getResources().getDisplayMetrics().density), pad, Math.round(6 * parent.getResources().getDisplayMetrics().density));
            return new HeaderVH(tv);
        }
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_eval_row, parent, false);
        return new RowVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = rows.get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).tv.setText((String) item);
            return;
        }
        Technician tc = (Technician) item;
        RowVH h = (RowVH) holder;
        h.itemView.setTag(tc.id);
        String displayName = tc.displayName(Lang.isArabic());
        h.tvAvatar.setText(tc.fileNo != null && tc.fileNo.length() >= 2 ? tc.fileNo.substring(tc.fileNo.length() - 2) : tc.fileNo);
        h.tvName.setText((tc.fileNo == null ? "" : tc.fileNo) + " - " + displayName);
        h.tvSub.setText(tc.nat == null ? "" : tc.nat);

        String[] labels = new String[11];
        labels[0] = Lang.t("— تقييم —", "— Score —");
        for (int i = 1; i <= 10; i++) labels[i] = i + " / 10";
        ArrayAdapter<String> adapter = new ArrayAdapter<>(h.itemView.getContext(), android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        h.spScore.setAdapter(adapter);

        Evaluation existingEval = existing.get(tc.id);
        h.spScore.setSelection(existingEval != null ? existingEval.score : 0, false);
        h.etNote.setText(existingEval != null ? existingEval.note : "");
        h.etNote.setHint(Lang.t("ملاحظة (اختياري)", "Note (optional)"));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tv;
        HeaderVH(TextView tv) { super(tv); this.tv = tv; }
    }

    public static class RowVH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvSub;
        public Spinner spScore;
        public EditText etNote;

        RowVH(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvSub = itemView.findViewById(R.id.tvSub);
            spScore = itemView.findViewById(R.id.spScore);
            etNote = itemView.findViewById(R.id.etNote);
        }
    }
}

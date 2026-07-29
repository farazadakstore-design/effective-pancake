package com.factory.techmanager.ui.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.factory.techmanager.R;
import com.factory.techmanager.data.Repository;
import com.factory.techmanager.data.TechStatus;
import com.factory.techmanager.data.Technician;
import com.factory.techmanager.util.Lang;

import java.util.List;

/** Renders the technician list on Home; rows are either a String (section header) or a Technician. */
public class MemberAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ROW = 1;

    public interface Listener {
        void onMainStatusChanged(String techId, String value);
        void onTempStatusChanged(String techId, String value);
    }

    private final List<Object> rows;
    private final Repository repo;
    private final Listener listener;

    public MemberAdapter(List<Object> rows, Repository repo, Listener listener) {
        this.rows = rows;
        this.repo = repo;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position) instanceof String ? TYPE_HEADER : TYPE_ROW;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            TextView tv = new TextView(parent.getContext());
            tv.setTextSize(12);
            tv.setTextColor(parent.getContext().getColor(R.color.muted));
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            int pad = dp(parent, 4);
            tv.setPadding(pad, dp(parent, 14), pad, dp(parent, 6));
            return new HeaderVH(tv);
        }
        View v = inflater.inflate(R.layout.item_member_row, parent, false);
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
        TechStatus s = repo.getStatus(tc.id);
        String displayName = tc.displayName(Lang.isArabic());

        h.tvAvatar.setText(tc.fileNo != null && tc.fileNo.length() >= 2 ? tc.fileNo.substring(tc.fileNo.length() - 2) : tc.fileNo);
        h.tvName.setText((tc.fileNo == null ? "" : tc.fileNo) + " - " + displayName);
        h.tvSub.setText(tc.nat == null ? "" : tc.nat);

        h.badgeContainer.removeAllViews();
        boolean onsite = "onsite".equals(s.main);
        h.badgeContainer.addView(makeBadge(h.itemView,
                onsite ? Lang.t("في الموقع", "On Site") : Lang.t("في المصنع", "In Factory"),
                onsite ? R.color.badge_onsite_bg : R.color.badge_avail_bg,
                onsite ? R.color.badge_onsite_text : R.color.badge_avail_text));
        if (s.temp != null) {
            String label; int bg, fg;
            switch (s.temp) {
                case "vacation": label = "🏖 " + Lang.t("إجازة", "Vacation"); bg = R.color.badge_vac_bg; fg = R.color.badge_vac_text; break;
                case "sick": label = "🤒 " + Lang.t("مرض", "Sick"); bg = R.color.badge_sick_bg; fg = R.color.badge_sick_text; break;
                case "permsite": label = "🏗 " + Lang.t("موقع دائم", "Perm. Site"); bg = R.color.badge_perm_bg; fg = R.color.badge_perm_text; break;
                case "absent": label = "🚫 " + Lang.t("غياب", "Absent"); bg = R.color.badge_absent_bg; fg = R.color.badge_absent_text; break;
                default: label = null; bg = 0; fg = 0;
            }
            if (label != null) {
                if (s.tempEnd != null && !s.tempEnd.isEmpty()) label += " " + s.tempEnd;
                h.badgeContainer.addView(makeBadge(h.itemView, label, bg, fg));
            }
        }

        if (tc.wa != null && !tc.wa.isEmpty()) {
            h.btnWa.setVisibility(View.VISIBLE);
            h.btnWa.setOnClickListener(v -> {
                String phone = tc.wa.replaceAll("\\D", "");
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + phone));
                v.getContext().startActivity(intent);
            });
        } else {
            h.btnWa.setVisibility(View.GONE);
        }

        String[] mainLabels = {Lang.t("مصنع", "Factory"), Lang.t("موقع", "On Site")};
        ArrayAdapter<String> mainAdapter = new ArrayAdapter<>(h.itemView.getContext(), android.R.layout.simple_spinner_item, mainLabels);
        mainAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        h.spMain.setAdapter(mainAdapter);
        h.spMain.setSelection(onsite ? 1 : 0, false);
        h.spMain.setOnTouchListener((v, event) -> { h.mainTouched = true; return false; });
        h.spMain.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (h.mainTouched) { listener.onMainStatusChanged(tc.id, pos == 1 ? "onsite" : "factory"); h.mainTouched = false; }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        String[] tempLabels = {
                Lang.t("— حالة —", "— Status —"), Lang.t("إجازة 🏖", "Vacation 🏖"),
                Lang.t("مرض 🤒", "Sick 🤒"), Lang.t("موقع دائم 🏗", "Perm Site 🏗"), Lang.t("غياب 🚫", "Absent 🚫")
        };
        String[] tempValues = {"", "vacation", "sick", "permsite", "absent"};
        ArrayAdapter<String> tempAdapter = new ArrayAdapter<>(h.itemView.getContext(), android.R.layout.simple_spinner_item, tempLabels);
        tempAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        h.spTemp.setAdapter(tempAdapter);
        int sel = 0;
        for (int i = 0; i < tempValues.length; i++) if (tempValues[i].equals(s.temp == null ? "" : s.temp)) sel = i;
        h.spTemp.setSelection(sel, false);
        h.spTemp.setOnTouchListener((v, event) -> { h.tempTouched = true; return false; });
        h.spTemp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (h.tempTouched) { listener.onTempStatusChanged(tc.id, tempValues[pos]); h.tempTouched = false; }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private View makeBadge(View anchor, String text, int bgColorRes, int fgColorRes) {
        TextView tv = new TextView(anchor.getContext());
        tv.setText(text);
        tv.setTextSize(10.5f);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setBackgroundResource(R.drawable.bg_badge);
        tv.getBackground().mutate().setTint(anchor.getContext().getColor(bgColorRes));
        tv.setTextColor(anchor.getContext().getColor(fgColorRes));
        int padH = dp(anchor, 7), padV = dp(anchor, 2);
        tv.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(anchor, 4));
        tv.setLayoutParams(lp);
        return tv;
    }

    private int dp(View v, int val) {
        return Math.round(val * v.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tv;
        HeaderVH(TextView tv) { super(tv); this.tv = tv; }
    }

    static class RowVH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvSub;
        LinearLayout badgeContainer;
        android.widget.Button btnWa;
        Spinner spMain, spTemp;
        boolean mainTouched = false, tempTouched = false;

        RowVH(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvSub = itemView.findViewById(R.id.tvSub);
            badgeContainer = itemView.findViewById(R.id.badgeContainer);
            btnWa = itemView.findViewById(R.id.btnWa);
            spMain = itemView.findViewById(R.id.spMain);
            spTemp = itemView.findViewById(R.id.spTemp);
        }
    }
}

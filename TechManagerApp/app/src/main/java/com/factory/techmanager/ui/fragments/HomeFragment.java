package com.factory.techmanager.ui.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.factory.techmanager.R;
import com.factory.techmanager.data.Department;
import com.factory.techmanager.data.Repository;
import com.factory.techmanager.data.Seed;
import com.factory.techmanager.data.TechStatus;
import com.factory.techmanager.data.Technician;
import com.factory.techmanager.ui.adapters.MemberAdapter;
import com.factory.techmanager.util.AppState;
import com.factory.techmanager.util.Lang;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private Repository repo;
    private LinearLayout statsBar, addTechCard;
    private TextView tvTechniciansTitle, tvAddTechTitle, tvAddTechMsg;
    private RecyclerView rvMembers;
    private EditText etName, etNameAr, etFileNo, etWa, etNat;
    private Button btnAddTech;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        repo = new Repository(requireContext());

        statsBar = v.findViewById(R.id.statsBar);
        addTechCard = v.findViewById(R.id.addTechCard);
        tvTechniciansTitle = v.findViewById(R.id.tvTechniciansTitle);
        tvAddTechTitle = v.findViewById(R.id.tvAddTechTitle);
        tvAddTechMsg = v.findViewById(R.id.tvAddTechMsg);
        rvMembers = v.findViewById(R.id.rvMembers);
        etName = v.findViewById(R.id.etName);
        etNameAr = v.findViewById(R.id.etNameAr);
        etFileNo = v.findViewById(R.id.etFileNo);
        etWa = v.findViewById(R.id.etWa);
        etNat = v.findViewById(R.id.etNat);
        btnAddTech = v.findViewById(R.id.btnAddTech);

        applyLang();
        rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        btnAddTech.setOnClickListener(x -> onAddTech());

        render();
        return v;
    }

    private void applyLang() {
        tvTechniciansTitle.setText(Lang.t("الفنيون", "Technicians"));
        tvAddTechTitle.setText("➕ " + Lang.t("إضافة فني للقسم", "Add Technician"));
        etName.setHint(Lang.t("الاسم (English)", "Name (English)"));
        etNameAr.setHint(Lang.t("الاسم بالعربي", "Arabic Name"));
        etFileNo.setHint(Lang.t("رقم الملف", "File No."));
        etWa.setHint(Lang.t("واتساب", "WhatsApp"));
        etNat.setHint(Lang.t("الجنسية", "Nationality"));
        btnAddTech.setText("✅ " + Lang.t("إضافة", "Add"));
    }

    private void render() {
        boolean isAdmin = AppState.session.isAdmin();
        String myDeptId = AppState.session.deptId;
        addTechCard.setVisibility(isAdmin ? View.GONE : View.VISIBLE);

        List<Technician> techs = new ArrayList<>();
        for (Technician t : repo.getAllTechs()) {
            if (isAdmin || t.group.equals(myDeptId)) techs.add(t);
        }

        int factory = 0, onsite = 0;
        for (Technician t : techs) {
            TechStatus s = repo.getStatus(t.id);
            if ("onsite".equals(s.main)) onsite++; else factory++;
        }
        buildStats(techs.size(), factory, onsite);

        List<Object> rows = new ArrayList<>();
        List<Department> groups = isAdmin ? Seed.DEPTS : java.util.Collections.singletonList(Seed.findDept(myDeptId));
        for (Department d : groups) {
            if (d == null) continue;
            List<Technician> deptTechs = new ArrayList<>();
            for (Technician t : techs) if (t.group.equals(d.id)) deptTechs.add(t);
            if (deptTechs.isEmpty()) continue;
            if (isAdmin) rows.add(d.label(Lang.isArabic()) + " (" + deptTechs.size() + ")");
            rows.addAll(deptTechs);
        }

        rvMembers.setAdapter(new MemberAdapter(rows, repo, new MemberAdapter.Listener() {
            @Override
            public void onMainStatusChanged(String techId, String value) {
                repo.setStatus(techId, "main", value);
                render();
            }

            @Override
            public void onTempStatusChanged(String techId, String value) {
                repo.setStatus(techId, "temp", value.isEmpty() ? null : value);
                if (value.isEmpty()) {
                    repo.setStatus(techId, "tempEnd", null);
                    render();
                } else {
                    showTempEndDialog(techId);
                }
            }
        }));
    }

    private void showTempEndDialog(String techId) {
        new AlertDialog.Builder(requireContext())
                .setTitle(Lang.t("تاريخ انتهاء الحالة", "Status end date"))
                .setMessage(Lang.t("حدد تاريخ الانتهاء أو اتركها بلا تاريخ", "Pick an end date or leave it open-ended"))
                .setPositiveButton(Lang.t("تحديد تاريخ", "Pick date"), (d, w) -> {
                    Calendar c = Calendar.getInstance();
                    new DatePickerDialog(requireContext(), (view, year, month, day) -> {
                        String date = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                        repo.setStatus(techId, "tempEnd", date);
                        render();
                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
                })
                .setNegativeButton(Lang.t("بدون تاريخ", "No date"), (d, w) -> {
                    repo.setStatus(techId, "tempEnd", null);
                    render();
                })
                .setCancelable(true)
                .setOnCancelListener(d -> render())
                .show();
    }

    private void buildStats(int total, int factory, int onsite) {
        statsBar.removeAllViews();
        statsBar.addView(statCard(String.valueOf(total), Lang.t("الإجمالي", "Total")));
        statsBar.addView(statCard(String.valueOf(factory), Lang.t("في المصنع", "In Factory")));
        statsBar.addView(statCard(String.valueOf(onsite), Lang.t("في الموقع", "On Site")));
    }

    private View statCard(String num, String label) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(android.view.Gravity.CENTER);
        card.setBackgroundResource(R.drawable.bg_stat_card);
        int padV = dp(12), padH = dp(6);
        card.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        card.setLayoutParams(lp);

        TextView tvNum = new TextView(requireContext());
        tvNum.setText(num);
        tvNum.setTextSize(20);
        tvNum.setTypeface(null, android.graphics.Typeface.BOLD);
        tvNum.setTextColor(requireContext().getColor(R.color.primary));
        tvNum.setGravity(android.view.Gravity.CENTER);
        card.addView(tvNum);

        TextView tvLbl = new TextView(requireContext());
        tvLbl.setText(label);
        tvLbl.setTextSize(10.5f);
        tvLbl.setTextColor(requireContext().getColor(R.color.muted));
        tvLbl.setGravity(android.view.Gravity.CENTER);
        card.addView(tvLbl);
        return card;
    }

    private void onAddTech() {
        String name = etName.getText().toString().trim();
        String nameAr = etNameAr.getText().toString().trim();
        String fileNo = etFileNo.getText().toString().trim();
        String wa = etWa.getText().toString().trim();
        String nat = etNat.getText().toString().trim();

        if (name.isEmpty() && nameAr.isEmpty()) { showMsg(Lang.t("أدخل الاسم", "Enter a name"), false); return; }
        if (fileNo.isEmpty()) { showMsg(Lang.t("أدخل رقم الملف", "Enter file number"), false); return; }
        if (repo.fileNoExists(fileNo)) { showMsg(Lang.t("رقم الملف موجود مسبقاً", "File number already exists"), false); return; }

        Technician t = new Technician("x" + System.currentTimeMillis(), AppState.session.deptId, fileNo,
                name.isEmpty() ? nameAr : name, nameAr.isEmpty() ? name : nameAr, wa, nat);
        repo.addTechnician(t);

        etName.setText(""); etNameAr.setText(""); etFileNo.setText(""); etWa.setText(""); etNat.setText("");
        showMsg("✓ " + Lang.t("تمت الإضافة", "Added"), true);
        render();
    }

    private void showMsg(String text, boolean ok) {
        tvAddTechMsg.setVisibility(View.VISIBLE);
        tvAddTechMsg.setBackgroundResource(ok ? R.drawable.bg_msg_ok : R.drawable.bg_msg_err);
        tvAddTechMsg.setTextColor(requireContext().getColor(ok ? R.color.badge_avail_text : R.color.danger));
        tvAddTechMsg.setText(text);
        tvAddTechMsg.postDelayed(() -> { if (isAdded()) tvAddTechMsg.setVisibility(View.GONE); }, 2500);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}

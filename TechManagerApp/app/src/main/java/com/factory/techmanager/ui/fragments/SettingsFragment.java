package com.factory.techmanager.ui.fragments;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.factory.techmanager.R;
import com.factory.techmanager.data.Department;
import com.factory.techmanager.data.Evaluation;
import com.factory.techmanager.data.Report;
import com.factory.techmanager.data.Repository;
import com.factory.techmanager.data.Seed;
import com.factory.techmanager.data.SettingsModel;
import com.factory.techmanager.data.Technician;
import com.factory.techmanager.ui.adapters.EvalHistoryAdapter;
import com.factory.techmanager.ui.adapters.ExtraTechAdapter;
import com.factory.techmanager.ui.adapters.PrevReportAdapter;
import com.factory.techmanager.util.AppState;
import com.factory.techmanager.util.Lang;
import com.factory.techmanager.util.TextBuilders;
import com.factory.techmanager.util.ShareHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private Repository repo;
    private TextView tvSettingsTitle, tvPwSectionTitle, tvShiftSectionTitle, tvBreakSectionTitle, tvSettingsMsg,
            tvAdminPwTitle, tvAddTechTitle, tvAddTechMsg, tvExtraTechsTitle, tvAllReportsTitle, tvAllEvalTitle;
    private LinearLayout pwContainer;
    private Button btnCheckInTime, btnCheckOutTime, btnSaveSettings, btnChangeAdminPw, btnAddTech;
    private EditText etBreakMinutes, etAdminPwNew, etName, etNameAr, etFileNo, etWa, etNat;
    private Spinner spDept;
    private RecyclerView rvExtraTechs, rvAllReports, rvAllEval;
    private final Map<String, EditText> pwFields = new HashMap<>();
    private String pendingCheckIn, pendingCheckOut;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        repo = new Repository(requireContext());
        bindViews(v);
        applyLang();

        SettingsModel s = repo.getSettings();
        pendingCheckIn = s.defaultCheckIn;
        pendingCheckOut = s.defaultCheckOut;
        buildPwRows(s);
        btnCheckInTime.setText(pendingCheckIn);
        btnCheckOutTime.setText(pendingCheckOut);
        etBreakMinutes.setText(String.valueOf(s.breakMinutes));

        btnCheckInTime.setOnClickListener(x -> pickTime(true));
        btnCheckOutTime.setOnClickListener(x -> pickTime(false));
        btnSaveSettings.setOnClickListener(x -> saveSettings());
        btnChangeAdminPw.setOnClickListener(x -> changeAdminPw());
        btnAddTech.setOnClickListener(x -> addTech());

        setupDeptSpinner();
        rvExtraTechs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAllReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAllEval.setLayoutManager(new LinearLayoutManager(requireContext()));

        renderExtraTechs();
        renderAllReports();
        renderAllEvaluations();
        return v;
    }

    private void bindViews(View v) {
        tvSettingsTitle = v.findViewById(R.id.tvSettingsTitle);
        tvPwSectionTitle = v.findViewById(R.id.tvPwSectionTitle);
        tvShiftSectionTitle = v.findViewById(R.id.tvShiftSectionTitle);
        tvBreakSectionTitle = v.findViewById(R.id.tvBreakSectionTitle);
        tvSettingsMsg = v.findViewById(R.id.tvSettingsMsg);
        tvAdminPwTitle = v.findViewById(R.id.tvAdminPwTitle);
        tvAddTechTitle = v.findViewById(R.id.tvAddTechTitle);
        tvAddTechMsg = v.findViewById(R.id.tvAddTechMsg);
        tvExtraTechsTitle = v.findViewById(R.id.tvExtraTechsTitle);
        tvAllReportsTitle = v.findViewById(R.id.tvAllReportsTitle);
        tvAllEvalTitle = v.findViewById(R.id.tvAllEvalTitle);
        pwContainer = v.findViewById(R.id.pwContainer);
        btnCheckInTime = v.findViewById(R.id.btnCheckInTime);
        btnCheckOutTime = v.findViewById(R.id.btnCheckOutTime);
        btnSaveSettings = v.findViewById(R.id.btnSaveSettings);
        btnChangeAdminPw = v.findViewById(R.id.btnChangeAdminPw);
        btnAddTech = v.findViewById(R.id.btnAddTech);
        etBreakMinutes = v.findViewById(R.id.etBreakMinutes);
        etAdminPwNew = v.findViewById(R.id.etAdminPwNew);
        etName = v.findViewById(R.id.etName);
        etNameAr = v.findViewById(R.id.etNameAr);
        etFileNo = v.findViewById(R.id.etFileNo);
        etWa = v.findViewById(R.id.etWa);
        etNat = v.findViewById(R.id.etNat);
        spDept = v.findViewById(R.id.spDept);
        rvExtraTechs = v.findViewById(R.id.rvExtraTechs);
        rvAllReports = v.findViewById(R.id.rvAllReports);
        rvAllEval = v.findViewById(R.id.rvAllEval);
    }

    private void applyLang() {
        tvSettingsTitle.setText("⚙️ " + Lang.t("إعدادات الأقسام", "Department Settings"));
        tvPwSectionTitle.setText(Lang.t("كلمات المرور", "Passwords"));
        tvShiftSectionTitle.setText(Lang.t("أوقات الدوام الافتراضية", "Default Shift Times"));
        tvBreakSectionTitle.setText(Lang.t("مدة الاستراحة (دقيقة) - تُخصم تلقائياً من ساعات العمل", "Break duration (minutes) - auto-deducted from work hours"));
        btnSaveSettings.setText("💾 " + Lang.t("حفظ", "Save"));
        tvAdminPwTitle.setText(Lang.t("كلمة مرور الأدمن", "Admin Password"));
        etAdminPwNew.setHint(Lang.t("كلمة المرور الجديدة", "New Password"));
        btnChangeAdminPw.setText(Lang.t("تغيير", "Change"));
        tvAddTechTitle.setText("➕ " + Lang.t("إضافة فني جديد", "Add Technician"));
        etName.setHint(Lang.t("الاسم (English)", "Name (English)"));
        etNameAr.setHint(Lang.t("الاسم بالعربي", "Arabic Name"));
        etFileNo.setHint(Lang.t("رقم الملف", "File No."));
        etWa.setHint(Lang.t("واتساب", "WhatsApp"));
        etNat.setHint(Lang.t("الجنسية", "Nationality"));
        btnAddTech.setText("✅ " + Lang.t("إضافة فني", "Add Technician"));
        tvAllReportsTitle.setText("📊 " + Lang.t("كل التقارير", "All Reports"));
        tvAllEvalTitle.setText("⭐ " + Lang.t("كل التقييمات", "All Evaluations"));
    }

    private void buildPwRows(SettingsModel s) {
        pwContainer.removeAllViews();
        pwFields.clear();
        for (Department d : Seed.DEPTS) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(4), 0, dp(4));

            TextView label = new TextView(requireContext());
            label.setText(d.icon + " " + d.label(Lang.isArabic()));
            label.setTextSize(13.5f);
            label.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            EditText input = new EditText(requireContext());
            input.setText(s.pwFor(d.id));
            input.setBackgroundResource(R.drawable.bg_edittext);
            input.setPadding(dp(8), dp(6), dp(8), dp(6));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(input, lp);

            pwContainer.addView(row);
            pwFields.put(d.id, input);
        }
    }

    private void pickTime(boolean isCheckIn) {
        String current = isCheckIn ? pendingCheckIn : pendingCheckOut;
        int hh = 6, mm = 30;
        try {
            String[] p = current.split(":");
            hh = Integer.parseInt(p[0]);
            mm = Integer.parseInt(p[1]);
        } catch (Exception ignored) {}
        new TimePickerDialog(requireContext(), (view, hour, minute) -> {
            String t = String.format(java.util.Locale.US, "%02d:%02d", hour, minute);
            if (isCheckIn) { pendingCheckIn = t; btnCheckInTime.setText(t); }
            else { pendingCheckOut = t; btnCheckOutTime.setText(t); }
        }, hh, mm, true).show();
    }

    private void saveSettings() {
        SettingsModel s = repo.getSettings();
        for (Department d : Seed.DEPTS) {
            EditText et = pwFields.get(d.id);
            if (et != null && !et.getText().toString().trim().isEmpty()) {
                s.deptPasswords.put(d.id, et.getText().toString().trim());
            }
        }
        String brk = etBreakMinutes.getText().toString().trim();
        if (!brk.isEmpty()) {
            try { s.breakMinutes = Integer.parseInt(brk); } catch (Exception ignored) {}
        }
        s.defaultCheckIn = pendingCheckIn;
        s.defaultCheckOut = pendingCheckOut;
        repo.saveSettings(s);

        for (com.factory.techmanager.data.ReportWorker w : AppState.reportWorkers) {
            w.hours = repo.calcHours(w.checkIn, w.checkOut);
        }

        tvSettingsMsg.setVisibility(View.VISIBLE);
        tvSettingsMsg.setText("✓ " + Lang.t("تم الحفظ", "Saved"));
        tvSettingsMsg.postDelayed(() -> { if (isAdded()) tvSettingsMsg.setVisibility(View.GONE); }, 2000);
    }

    private void changeAdminPw() {
        String pw = etAdminPwNew.getText().toString().trim();
        if (pw.isEmpty()) return;
        SettingsModel s = repo.getSettings();
        s.adminPw = pw;
        repo.saveSettings(s);
        etAdminPwNew.setText("");
        Toast.makeText(requireContext(), Lang.t("تم تغيير كلمة المرور", "Password changed"), Toast.LENGTH_SHORT).show();
    }

    private void setupDeptSpinner() {
        List<String> labels = new ArrayList<>();
        for (Department d : Seed.DEPTS) labels.add(d.icon + " " + d.label(Lang.isArabic()));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDept.setAdapter(adapter);
    }

    private void addTech() {
        String name = etName.getText().toString().trim();
        String nameAr = etNameAr.getText().toString().trim();
        String fileNo = etFileNo.getText().toString().trim();
        String wa = etWa.getText().toString().trim();
        String nat = etNat.getText().toString().trim();
        String group = Seed.DEPTS.get(spDept.getSelectedItemPosition()).id;

        if (name.isEmpty() && nameAr.isEmpty()) { showAddMsg(Lang.t("أدخل الاسم", "Enter a name"), false); return; }
        if (fileNo.isEmpty()) { showAddMsg(Lang.t("أدخل رقم الملف", "Enter file number"), false); return; }
        if (repo.fileNoExists(fileNo)) { showAddMsg(Lang.t("رقم الملف موجود مسبقاً", "File number already exists"), false); return; }

        Technician t = new Technician("x" + System.currentTimeMillis(), group, fileNo,
                name.isEmpty() ? nameAr : name, nameAr.isEmpty() ? name : nameAr, wa, nat);
        repo.addTechnician(t);

        etName.setText(""); etNameAr.setText(""); etFileNo.setText(""); etWa.setText(""); etNat.setText("");
        showAddMsg("✓ " + Lang.t("تمت الإضافة", "Technician added"), true);
        renderExtraTechs();
    }

    private void showAddMsg(String text, boolean ok) {
        tvAddTechMsg.setVisibility(View.VISIBLE);
        tvAddTechMsg.setBackgroundResource(ok ? R.drawable.bg_msg_ok : R.drawable.bg_msg_err);
        tvAddTechMsg.setTextColor(requireContext().getColor(ok ? R.color.badge_avail_text : R.color.danger));
        tvAddTechMsg.setText(text);
        tvAddTechMsg.postDelayed(() -> { if (isAdded()) tvAddTechMsg.setVisibility(View.GONE); }, 2500);
    }

    private void renderExtraTechs() {
        List<Technician> extras = repo.getExtraTechs();
        tvExtraTechsTitle.setVisibility(extras.isEmpty() ? View.GONE : View.VISIBLE);
        tvExtraTechsTitle.setText(Lang.t("الفنيون المضافون", "Added Technicians"));
        rvExtraTechs.setAdapter(new ExtraTechAdapter(extras, t -> {
            repo.removeExtraTech(t.id);
            renderExtraTechs();
        }));
    }

    private void renderAllReports() {
        List<Report> reports = repo.getReports();
        List<Report> top = reports.size() > 30 ? reports.subList(0, 30) : reports;
        rvAllReports.setAdapter(new PrevReportAdapter(top, new PrevReportAdapter.Listener() {
            @Override public void onView(Report r) {
                String text = TextBuilders.buildReportText(r);
                new AlertDialog.Builder(requireContext())
                        .setTitle(r.reportNo != null ? r.reportNo : Lang.t("التقرير", "Report"))
                        .setMessage(text)
                        .setPositiveButton(Lang.t("إرسال عبر واتساب", "Send via WhatsApp"), (d, w) -> ShareHelper.sendWhatsAppText(requireContext(), text))
                        .setNegativeButton(Lang.t("إغلاق", "Close"), null)
                        .show();
            }
            @Override public void onDelete(Report r) {
                new AlertDialog.Builder(requireContext())
                        .setMessage(Lang.t("حذف هذا التقرير؟", "Delete this report?"))
                        .setPositiveButton(Lang.t("حذف", "Delete"), (d, w) -> { repo.deleteReport(r.id); renderAllReports(); })
                        .setNegativeButton(Lang.t("إلغاء", "Cancel"), null)
                        .show();
            }
        }, true));
    }

    private void renderAllEvaluations() {
        List<Evaluation> evals = repo.getEvaluations();
        evals.sort((a, b) -> {
            int c = b.month.compareTo(a.month);
            return c != 0 ? c : Integer.compare(b.score, a.score);
        });
        List<Evaluation> top = evals.size() > 80 ? evals.subList(0, 80) : evals;
        rvAllEval.setAdapter(new EvalHistoryAdapter(top, true, true, e -> {
            new AlertDialog.Builder(requireContext())
                    .setMessage(Lang.t("حذف هذا التقييم؟", "Delete this evaluation?"))
                    .setPositiveButton(Lang.t("حذف", "Delete"), (d, w) -> { repo.deleteEvaluation(e.id); renderAllEvaluations(); })
                    .setNegativeButton(Lang.t("إلغاء", "Cancel"), null)
                    .show();
        }));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}

package com.factory.techmanager.ui.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
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
import com.factory.techmanager.data.Repository;
import com.factory.techmanager.data.Seed;
import com.factory.techmanager.data.Technician;
import com.factory.techmanager.ui.adapters.EvalHistoryAdapter;
import com.factory.techmanager.ui.adapters.EvalRowAdapter;
import com.factory.techmanager.util.AppState;
import com.factory.techmanager.util.Lang;
import com.factory.techmanager.util.PdfBuilder;
import com.factory.techmanager.util.ShareHelper;
import com.factory.techmanager.util.StatusLabels;
import com.factory.techmanager.util.TextBuilders;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EvaluationFragment extends Fragment {

    private Repository repo;
    private TextView tvEvalTitle, tvMonthLabel, tvAvgBar, tvEvalMsg, tvEvalHistoryTitle;
    private Button btnMonthPicker, btnSaveEval, btnEvalWa, btnEvalText;
    private RecyclerView rvEvalList, rvEvalHistory;
    private String currentMonth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_evaluation, container, false);
        repo = new Repository(requireContext());

        tvEvalTitle = v.findViewById(R.id.tvEvalTitle);
        tvMonthLabel = v.findViewById(R.id.tvMonthLabel);
        tvAvgBar = v.findViewById(R.id.tvAvgBar);
        tvEvalMsg = v.findViewById(R.id.tvEvalMsg);
        tvEvalHistoryTitle = v.findViewById(R.id.tvEvalHistoryTitle);
        btnMonthPicker = v.findViewById(R.id.btnMonthPicker);
        btnSaveEval = v.findViewById(R.id.btnSaveEval);
        btnEvalWa = v.findViewById(R.id.btnEvalWa);
        btnEvalText = v.findViewById(R.id.btnEvalText);
        rvEvalList = v.findViewById(R.id.rvEvalList);
        rvEvalHistory = v.findViewById(R.id.rvEvalHistory);

        rvEvalList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEvalHistory.setLayoutManager(new LinearLayoutManager(requireContext()));

        currentMonth = new java.text.SimpleDateFormat("yyyy-MM", Locale.US).format(new java.util.Date());

        applyLang();
        btnMonthPicker.setOnClickListener(x -> showMonthPicker());
        btnSaveEval.setOnClickListener(x -> saveAllEvaluations());
        btnEvalWa.setOnClickListener(x -> sendEvalWa());
        btnEvalText.setOnClickListener(x -> sendEvalText());

        renderEvalList();
        renderEvalHistory();
        return v;
    }

    private void applyLang() {
        tvEvalTitle.setText("📊 " + Lang.t("التقييم الشهري", "Monthly Evaluation"));
        tvMonthLabel.setText(Lang.t("الشهر", "Month"));
        btnSaveEval.setText("💾 " + Lang.t("حفظ التقييمات", "Save Evaluations"));
        btnEvalWa.setText("📤 " + Lang.t("PDF واتساب", "PDF WhatsApp"));
        btnEvalText.setText("💬 " + Lang.t("إرسال نص", "Send Text"));
        tvEvalHistoryTitle.setText("📈 " + Lang.t("سجل التقييمات", "Evaluation History"));
        btnMonthPicker.setText(StatusLabels.monthLabel(currentMonth));
    }

    private void showMonthPicker() {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER);
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        row.setPadding(pad, pad, pad, pad);

        String[] parts = currentMonth.split("-");
        int curYear = Integer.parseInt(parts[0]);
        int curMonth = Integer.parseInt(parts[1]);

        NumberPicker monthPicker = new NumberPicker(requireContext());
        String[] namesAr = {"يناير","فبراير","مارس","أبريل","مايو","يونيو","يوليو","أغسطس","سبتمبر","أكتوبر","نوفمبر","ديسمبر"};
        String[] namesEn = {"January","February","March","April","May","June","July","August","September","October","November","December"};
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setDisplayedValues(Lang.isArabic() ? namesAr : namesEn);
        monthPicker.setValue(curMonth);

        NumberPicker yearPicker = new NumberPicker(requireContext());
        int nowYear = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(nowYear - 5);
        yearPicker.setMaxValue(nowYear + 1);
        yearPicker.setValue(curYear);

        row.addView(monthPicker, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(yearPicker, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        new AlertDialog.Builder(requireContext())
                .setTitle(Lang.t("اختر الشهر", "Select Month"))
                .setView(row)
                .setPositiveButton(Lang.t("تم", "OK"), (d, w) -> {
                    currentMonth = String.format(Locale.US, "%04d-%02d", yearPicker.getValue(), monthPicker.getValue());
                    btnMonthPicker.setText(StatusLabels.monthLabel(currentMonth));
                    renderEvalList();
                })
                .setNegativeButton(Lang.t("إلغاء", "Cancel"), null)
                .show();
    }

    private void renderEvalList() {
        boolean isAdmin = AppState.session.isAdmin();
        String myDeptId = isAdmin ? null : AppState.session.deptId;

        List<Technician> techs = new ArrayList<>();
        for (Technician t : repo.getAllTechs()) if (myDeptId == null || t.group.equals(myDeptId)) techs.add(t);

        Map<String, Evaluation> existingMap = new HashMap<>();
        for (Evaluation e : repo.getEvaluations()) if (e.month.equals(currentMonth)) existingMap.put(e.techId, e);

        List<Object> rows = new ArrayList<>();
        List<Department> groups = myDeptId == null ? Seed.DEPTS : java.util.Collections.singletonList(Seed.findDept(myDeptId));
        for (Department d : groups) {
            if (d == null) continue;
            List<Technician> deptTechs = new ArrayList<>();
            for (Technician t : techs) if (t.group.equals(d.id)) deptTechs.add(t);
            if (deptTechs.isEmpty()) continue;
            if (myDeptId == null) rows.add(d.label(Lang.isArabic()) + " (" + deptTechs.size() + ")");
            rows.addAll(deptTechs);
        }
        rvEvalList.setAdapter(new EvalRowAdapter(rows, existingMap));

        List<Evaluation> monthEvals = new ArrayList<>();
        for (Evaluation e : repo.getEvaluations()) {
            if (!e.month.equals(currentMonth)) continue;
            if (myDeptId != null && !e.deptId.equals(myDeptId)) continue;
            monthEvals.add(e);
        }
        if (!monthEvals.isEmpty()) {
            double sum = 0;
            for (Evaluation e : monthEvals) sum += e.score;
            double avg = sum / monthEvals.size();
            tvAvgBar.setVisibility(View.VISIBLE);
            tvAvgBar.setBackgroundResource(R.drawable.bg_msg_info);
            tvAvgBar.setPadding(24, 24, 24, 24);
            tvAvgBar.setText("⭐ " + Lang.t("متوسط التقييم لهذا الشهر", "Average score this month") + ": " +
                    String.format(Locale.US, "%.1f", avg) + "/10 (" + monthEvals.size() + " " + Lang.t("فني مُقيّم", "rated") + ")");
        } else {
            tvAvgBar.setVisibility(View.GONE);
        }
    }

    private void saveAllEvaluations() {
        int count = 0;
        for (int i = 0; i < rvEvalList.getChildCount(); i++) {
            View child = rvEvalList.getChildAt(i);
            Object tag = child.getTag();
            if (!(tag instanceof String)) continue;
            String techId = (String) tag;
            android.widget.Spinner sp = child.findViewById(R.id.spScore);
            android.widget.EditText et = child.findViewById(R.id.etNote);
            if (sp == null) continue;
            int pos = sp.getSelectedItemPosition();
            if (pos <= 0) continue;
            com.factory.techmanager.data.Technician tc = repo.findTechById(techId);
            if (tc == null) continue;
            String note = et != null ? et.getText().toString().trim() : "";
            repo.upsertEvaluation(techId, tc.name, tc.nameAr, tc.group, tc.fileNo, currentMonth, pos, note);
            count++;
        }
        tvEvalMsg.setVisibility(View.VISIBLE);
        if (count > 0) {
            tvEvalMsg.setBackgroundResource(R.drawable.bg_msg_ok);
            tvEvalMsg.setTextColor(requireContext().getColor(R.color.badge_avail_text));
            tvEvalMsg.setText("✓ " + String.format(Lang.t("تم حفظ %d تقييم", "Saved %d evaluation(s)"), count));
        } else {
            tvEvalMsg.setBackgroundResource(R.drawable.bg_msg_err);
            tvEvalMsg.setTextColor(requireContext().getColor(R.color.danger));
            tvEvalMsg.setText(Lang.t("لم يتم اختيار أي تقييم", "No scores selected"));
        }
        tvEvalMsg.postDelayed(() -> { if (isAdded()) tvEvalMsg.setVisibility(View.GONE); }, 2500);
        renderEvalList();
        renderEvalHistory();
    }

    private void renderEvalHistory() {
        boolean isAdmin = AppState.session.isAdmin();
        String myDeptId = isAdmin ? null : AppState.session.deptId;
        List<Evaluation> evals = new ArrayList<>();
        for (Evaluation e : repo.getEvaluations()) if (myDeptId == null || e.deptId.equals(myDeptId)) evals.add(e);
        evals.sort((a, b) -> {
            int c = b.month.compareTo(a.month);
            return c != 0 ? c : Integer.compare(b.score, a.score);
        });
        List<Evaluation> top = evals.size() > 60 ? evals.subList(0, 60) : evals;
        rvEvalHistory.setAdapter(new EvalHistoryAdapter(top, myDeptId == null, isAdmin, e -> {
            new AlertDialog.Builder(requireContext())
                    .setMessage(Lang.t("حذف هذا التقييم؟", "Delete this evaluation?"))
                    .setPositiveButton(Lang.t("حذف", "Delete"), (d, w) -> {
                        repo.deleteEvaluation(e.id);
                        renderEvalList();
                        renderEvalHistory();
                    })
                    .setNegativeButton(Lang.t("إلغاء", "Cancel"), null)
                    .show();
        }));
    }

    private List<Evaluation> evalsForSend() {
        boolean isAdmin = AppState.session.isAdmin();
        String myDeptId = isAdmin ? null : AppState.session.deptId;
        List<Evaluation> evals = new ArrayList<>();
        for (Evaluation e : repo.getEvaluations()) {
            if (!e.month.equals(currentMonth)) continue;
            if (myDeptId != null && !e.deptId.equals(myDeptId)) continue;
            evals.add(e);
        }
        evals.sort((a, b) -> {
            int da = deptIndex(a.deptId), db = deptIndex(b.deptId);
            return da != db ? Integer.compare(da, db) : Integer.compare(b.score, a.score);
        });
        return evals;
    }

    private int deptIndex(String deptId) {
        for (int i = 0; i < Seed.DEPTS.size(); i++) if (Seed.DEPTS.get(i).id.equals(deptId)) return i;
        return 99;
    }

    private void sendEvalWa() {
        List<Evaluation> evals = evalsForSend();
        if (evals.isEmpty()) {
            Toast.makeText(requireContext(), Lang.t("لا توجد تقييمات محفوظة لهذا الشهر", "No saved evaluations for this month"), Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            try {
                File pdf = PdfBuilder.buildEvalPdf(requireContext(), currentMonth, evals);
                requireActivity().runOnUiThread(() -> ShareHelper.shareFileToWhatsApp(requireContext(), pdf, "application/pdf",
                        Lang.t("تقييم الفنيين الشهري", "Monthly Evaluation")));
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), Lang.t("تعذر إنشاء PDF", "Could not build PDF"), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void sendEvalText() {
        List<Evaluation> evals = evalsForSend();
        String text = TextBuilders.buildEvalText(currentMonth, evals);
        if (text == null) {
            Toast.makeText(requireContext(), Lang.t("لا توجد تقييمات محفوظة لهذا الشهر", "No saved evaluations for this month"), Toast.LENGTH_SHORT).show();
            return;
        }
        ShareHelper.shareTextGeneric(requireContext(), text);
    }
}

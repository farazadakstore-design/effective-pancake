package com.factory.techmanager.ui.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import com.factory.techmanager.data.Report;
import com.factory.techmanager.data.ReportWorker;
import com.factory.techmanager.data.Repository;
import com.factory.techmanager.data.Seed;
import com.factory.techmanager.data.SettingsModel;
import com.factory.techmanager.data.TechStatus;
import com.factory.techmanager.data.Technician;
import com.factory.techmanager.ui.adapters.PrevReportAdapter;
import com.factory.techmanager.ui.adapters.WorkerRowAdapter;
import com.factory.techmanager.util.AppState;
import com.factory.techmanager.util.Lang;
import com.factory.techmanager.util.PdfBuilder;
import com.factory.techmanager.util.ShareHelper;
import com.factory.techmanager.util.TextBuilders;
import com.factory.techmanager.util.WordExporter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportFragment extends Fragment {

    private Repository repo;
    private TextView tvReportTitle, tvWorkersTitle, tvBreakInfo, tvAvailTitle, tvDonePrevLabel, tvTodoLabel, tvMaterialsLabel, tvReportMsg, tvPrevReportsTitle;
    private EditText etDate, etSupervisor, etSection, etContractNo, etDonePrev, etTodo, etMaterials;
    private RecyclerView rvWorkers, rvPrevReports;
    private Button btnAddWorker, btnAddAllAvailable, btnAddAvail, btnSaveReport, btnWa, btnWord, btnSendText;
    private Spinner spAvail;
    private ChipGroup chipAvail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_report, container, false);
        repo = new Repository(requireContext());
        bindViews(v);
        applyLang();
        setupDatePicker();
        prefillFields();
        setupWorkers();
        setupAvailSection();
        setupButtons();
        renderPrevReports();
        return v;
    }

    private void bindViews(View v) {
        tvReportTitle = v.findViewById(R.id.tvReportTitle);
        tvWorkersTitle = v.findViewById(R.id.tvWorkersTitle);
        tvBreakInfo = v.findViewById(R.id.tvBreakInfo);
        tvAvailTitle = v.findViewById(R.id.tvAvailTitle);
        tvDonePrevLabel = v.findViewById(R.id.tvDonePrevLabel);
        tvTodoLabel = v.findViewById(R.id.tvTodoLabel);
        tvMaterialsLabel = v.findViewById(R.id.tvMaterialsLabel);
        tvReportMsg = v.findViewById(R.id.tvReportMsg);
        tvPrevReportsTitle = v.findViewById(R.id.tvPrevReportsTitle);
        etDate = v.findViewById(R.id.etDate);
        etSupervisor = v.findViewById(R.id.etSupervisor);
        etSection = v.findViewById(R.id.etSection);
        etContractNo = v.findViewById(R.id.etContractNo);
        etDonePrev = v.findViewById(R.id.etDonePrev);
        etTodo = v.findViewById(R.id.etTodo);
        etMaterials = v.findViewById(R.id.etMaterials);
        rvWorkers = v.findViewById(R.id.rvWorkers);
        rvPrevReports = v.findViewById(R.id.rvPrevReports);
        btnAddWorker = v.findViewById(R.id.btnAddWorker);
        btnAddAllAvailable = v.findViewById(R.id.btnAddAllAvailable);
        btnAddAvail = v.findViewById(R.id.btnAddAvail);
        btnSaveReport = v.findViewById(R.id.btnSaveReport);
        btnWa = v.findViewById(R.id.btnWa);
        btnWord = v.findViewById(R.id.btnWord);
        btnSendText = v.findViewById(R.id.btnSendText);
        spAvail = v.findViewById(R.id.spAvail);
        chipAvail = v.findViewById(R.id.chipAvail);
    }

    private void applyLang() {
        tvReportTitle.setText("📋 " + Lang.t("تقرير اليوم", "Today's Report"));
        etDate.setHint(Lang.t("التاريخ", "Date"));
        etSupervisor.setHint(Lang.t("المشرف", "Supervisor"));
        etSection.setHint(Lang.t("القسم / الموقع", "Section / Site"));
        etContractNo.setHint(Lang.t("رقم العقد", "Contract No."));
        tvWorkersTitle.setText("👷 " + Lang.t("العمال", "Workers"));
        tvAvailTitle.setText("👥 " + Lang.t("العمال المتوفرون اليوم", "Available Workers Today"));
        btnAddAvail.setText("➕ " + Lang.t("إضافة", "Add"));
        tvDonePrevLabel.setText("📅 " + Lang.t("أعمال اليوم السابق", "Yesterday's Completed Work"));
        tvTodoLabel.setText("📌 " + Lang.t("الأعمال المطلوبة اليوم", "Today's Required Work"));
        tvMaterialsLabel.setText("⚠️ " + Lang.t("مواد عاجلة من المستودع", "Urgent Materials from Warehouse"));
        btnAddWorker.setText("➕ " + Lang.t("إضافة عامل", "Add Worker"));
        btnAddAllAvailable.setText(Lang.t("إضافة المتوفرين", "Add Available"));
        btnSaveReport.setText("💾 " + Lang.t("حفظ التقرير", "Save Report"));
        btnWa.setText("📤 " + Lang.t("PDF واتساب", "PDF WhatsApp"));
        btnWord.setText("📄 Word");
        btnSendText.setText("💬 " + Lang.t("إرسال نص", "Send Text"));
        tvPrevReportsTitle.setText(Lang.t("السجل السابق", "Previous Reports"));
        boolean hasSaved = AppState.savedReport != null;
        btnWa.setEnabled(hasSaved);
        btnWord.setEnabled(hasSaved);
        btnSendText.setEnabled(hasSaved);
    }

    private String myDeptId() {
        // Admin has no dept — original app defaults report context to "electricians" for admin.
        return AppState.session.isAdmin() ? "electricians" : AppState.session.deptId;
    }

    private void setupDatePicker() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        etDate.setText(AppState.reportDate != null ? AppState.reportDate : today);
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            try {
                String[] p = etDate.getText().toString().split("-");
                c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]));
            } catch (Exception ignored) {}
            new DatePickerDialog(requireContext(), (view, year, month, day) ->
                    etDate.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void prefillFields() {
        Department d = Seed.findDept(myDeptId());
        etSection.setText(AppState.reportSection != null ? AppState.reportSection : (d != null ? d.label(Lang.isArabic()) : ""));
        etSupervisor.setText(AppState.reportSupervisor != null ? AppState.reportSupervisor : Seed.defaultSupervisor(myDeptId()));
        etContractNo.setText(AppState.reportContractNo != null ? AppState.reportContractNo : "");
        etDonePrev.setText(AppState.reportDonePrev != null ? AppState.reportDonePrev : "");
        etTodo.setText(AppState.reportTodo != null ? AppState.reportTodo : "");
        etMaterials.setText(AppState.reportMaterials != null ? AppState.reportMaterials : "");

        SettingsModel s = repo.getSettings();
        StringBuilder info = new StringBuilder();
        info.append(Lang.t("⏰ الدوام الافتراضي: ", "⏰ Default shift: ")).append(s.defaultCheckIn).append(" → ").append(s.defaultCheckOut);
        if (s.breakMinutes > 0) {
            info.append(" · ").append(Lang.t("⏱ يُخصم تلقائياً ", "⏱ ")).append(s.breakMinutes)
                    .append(Lang.t(" دقيقة استراحة", " min break auto-deducted"));
        }
        info.append(" · ").append(Lang.t("(من الضبط)", "(from Settings)"));
        tvBreakInfo.setText(info.toString());
    }

    private void setupWorkers() {
        if (AppState.reportWorkers.isEmpty()) initReportWorkers();
        rvWorkers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvWorkers.setAdapter(new WorkerRowAdapter(AppState.reportWorkers, repo, new WorkerRowAdapter.Listener() {
            @Override public void onRemove(int position) {
                AppState.reportWorkers.remove(position);
                rvWorkers.getAdapter().notifyItemRemoved(position);
            }
            @Override public void onChanged() {}
        }));
    }

    private void initReportWorkers() {
        AppState.reportWorkers = new ArrayList<>();
        if (AppState.session.isAdmin()) return; // admin report starts with an empty worker list, as in the original
        SettingsModel s = repo.getSettings();
        for (Technician t : repo.getAllTechs()) {
            if (!t.group.equals(AppState.session.deptId)) continue;
            TechStatus st = repo.getStatus(t.id);
            String statusKey = statusKeyFor(st);
            ReportWorker w = new ReportWorker();
            w.techId = t.id;
            w.name = t.displayName(Lang.isArabic());
            w.fileNo = t.fileNo;
            w.status = statusKey;
            w.checkIn = s.defaultCheckIn;
            w.checkOut = s.defaultCheckOut;
            w.hours = repo.calcHours(w.checkIn, w.checkOut);
            AppState.reportWorkers.add(w);
        }
    }

    private String statusKeyFor(TechStatus s) {
        if ("vacation".equals(s.temp)) return "vacation";
        if ("sick".equals(s.temp)) return "sick";
        if ("permsite".equals(s.temp)) return "permsite";
        if ("absent".equals(s.temp)) return "absent";
        if ("onsite".equals(s.main)) return "onsite";
        return "factory";
    }

    private void setupAvailSection() {
        refreshAvailSpinner();
        refreshChips();
        btnAddAvail.setOnClickListener(v -> {
            int pos = spAvail.getSelectedItemPosition();
            if (pos <= 0) return;
            String name = (String) spAvail.getSelectedItem();
            if (!AppState.availWorkers.contains(name)) {
                AppState.availWorkers.add(name);
                refreshChips();
            }
            spAvail.setSelection(0);
        });
    }

    private void refreshAvailSpinner() {
        boolean isAdmin = AppState.session.isAdmin();
        List<String> names = new ArrayList<>();
        names.add(Lang.t("— المتوفرون في المصنع —", "— In Factory —"));
        for (Technician t : repo.getAllTechs()) {
            if (!isAdmin && !t.group.equals(AppState.session.deptId)) continue;
            TechStatus s = repo.getStatus(t.id);
            if (!"onsite".equals(s.main) && s.temp == null) names.add(t.displayName(Lang.isArabic()));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAvail.setAdapter(adapter);
    }

    private void refreshChips() {
        chipAvail.removeAllViews();
        if (AppState.availWorkers.isEmpty()) return;
        for (String name : AppState.availWorkers) {
            Chip chip = new Chip(requireContext());
            chip.setText(name);
            chip.setCloseIconVisible(true);
            chip.setChipBackgroundColorResource(R.color.light);
            chip.setTextColor(requireContext().getColor(R.color.primary_dark));
            chip.setOnCloseIconClickListener(v -> {
                AppState.availWorkers.remove(name);
                refreshChips();
            });
            chipAvail.addView(chip);
        }
    }

    private void setupButtons() {
        btnAddWorker.setOnClickListener(v -> {
            SettingsModel s = repo.getSettings();
            ReportWorker w = new ReportWorker();
            w.checkIn = s.defaultCheckIn;
            w.checkOut = s.defaultCheckOut;
            w.hours = repo.calcHours(w.checkIn, w.checkOut);
            AppState.reportWorkers.add(w);
            rvWorkers.getAdapter().notifyItemInserted(AppState.reportWorkers.size() - 1);
        });

        btnAddAllAvailable.setOnClickListener(v -> {
            boolean isAdmin = AppState.session.isAdmin();
            SettingsModel s = repo.getSettings();
            for (Technician t : repo.getAllTechs()) {
                if (!isAdmin && !t.group.equals(AppState.session.deptId)) continue;
                TechStatus st = repo.getStatus(t.id);
                if ("onsite".equals(st.main) || st.temp != null) continue;
                String nm = t.displayName(Lang.isArabic());
                boolean exists = false;
                for (ReportWorker rw : AppState.reportWorkers) {
                    if (t.id.equals(rw.techId) || nm.equals(rw.name)) { exists = true; break; }
                }
                if (exists) continue;
                ReportWorker w = new ReportWorker();
                w.techId = t.id;
                w.name = nm;
                w.fileNo = t.fileNo;
                w.status = "factory";
                w.checkIn = s.defaultCheckIn;
                w.checkOut = s.defaultCheckOut;
                w.hours = repo.calcHours(w.checkIn, w.checkOut);
                AppState.reportWorkers.add(w);
            }
            rvWorkers.getAdapter().notifyDataSetChanged();
        });

        btnSaveReport.setOnClickListener(v -> saveReport());
        btnWa.setOnClickListener(v -> sendWa());
        btnWord.setOnClickListener(v -> sendWord());
        btnSendText.setOnClickListener(v -> sendText());
    }

    private void saveReport() {
        try {
            if (etDate.getText().toString().trim().isEmpty()) {
                etDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
            }
            Report r = new Report();
            r.id = String.valueOf(System.currentTimeMillis());
            r.reportNo = repo.getNextReportNo(AppState.session.deptId);
            r.date = etDate.getText().toString().trim();
            r.supervisor = etSupervisor.getText().toString().trim();
            r.section = etSection.getText().toString().trim();
            r.contractNo = etContractNo.getText().toString().trim();
            r.deptId = AppState.session.deptId;
            r.workers = new ArrayList<>(AppState.reportWorkers);
            r.availWorkers = new ArrayList<>(AppState.availWorkers);
            r.donePrev = etDonePrev.getText().toString().trim();
            r.todo = etTodo.getText().toString().trim();
            r.materials = etMaterials.getText().toString().trim();
            r.createdAt = new Date().toString();
            repo.saveReport(r);
            AppState.savedReport = r;

            tvReportMsg.setVisibility(View.VISIBLE);
            tvReportMsg.setBackgroundResource(R.drawable.bg_msg_ok);
            tvReportMsg.setTextColor(requireContext().getColor(R.color.badge_avail_text));
            tvReportMsg.setText("✓ " + Lang.t("تم حفظ التقرير", "Report saved"));
            btnWa.setEnabled(true);
            btnWord.setEnabled(true);
            btnSendText.setEnabled(true);
            renderPrevReports();
            repo.clearOpenEndedTempStatuses();
        } catch (Exception e) {
            tvReportMsg.setVisibility(View.VISIBLE);
            tvReportMsg.setBackgroundResource(R.drawable.bg_msg_err);
            tvReportMsg.setTextColor(requireContext().getColor(R.color.danger));
            tvReportMsg.setText(Lang.t("⚠️ حدث خطأ أثناء الحفظ: ", "⚠️ Error while saving: ") + e.getMessage());
        }
    }

    private void sendWa() {
        if (AppState.savedReport == null) return;
        new Thread(() -> {
            try {
                File pdf = PdfBuilder.buildReportPdf(requireContext(), AppState.savedReport);
                requireActivity().runOnUiThread(() -> ShareHelper.shareFileToWhatsApp(requireContext(), pdf, "application/pdf",
                        Lang.t("تقرير نهاية الدوام", "End-of-Shift Report")));
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), Lang.t("تعذر إنشاء PDF", "Could not build PDF"), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void sendWord() {
        if (AppState.savedReport == null) return;
        new Thread(() -> {
            try {
                File doc = WordExporter.buildReportDoc(requireContext(), AppState.savedReport);
                requireActivity().runOnUiThread(() -> ShareHelper.shareFileGeneric(requireContext(), doc, "application/msword", null));
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), Lang.t("تعذر إنشاء ملف Word", "Could not build Word file"), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void sendText() {
        if (AppState.savedReport == null) return;
        String text = TextBuilders.buildReportText(AppState.savedReport);
        ShareHelper.shareTextGeneric(requireContext(), text);
    }

    private void renderPrevReports() {
        boolean isAdmin = AppState.session.isAdmin();
        List<Report> all = repo.getReports();
        List<Report> filtered = new ArrayList<>();
        for (Report r : all) if (isAdmin || r.deptId.equals(AppState.session.deptId)) filtered.add(r);
        List<Report> top = filtered.size() > 10 ? filtered.subList(0, 10) : filtered;
        rvPrevReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPrevReports.setAdapter(new PrevReportAdapter(top, new PrevReportAdapter.Listener() {
            @Override public void onView(Report r) { showReportDialog(r); }
            @Override public void onDelete(Report r) {}
        }, false));
    }

    private void showReportDialog(Report r) {
        String text = TextBuilders.buildReportText(r);
        new AlertDialog.Builder(requireContext())
                .setTitle(r.reportNo != null ? r.reportNo : Lang.t("التقرير", "Report"))
                .setMessage(text)
                .setPositiveButton(Lang.t("إرسال عبر واتساب", "Send via WhatsApp"), (d, w) -> ShareHelper.sendWhatsAppText(requireContext(), text))
                .setNegativeButton(Lang.t("إغلاق", "Close"), null)
                .show();
    }

    @Override
    public void onDestroyView() {
        // Persist in-progress form text so it survives bottom-nav tab switches.
        if (etDate != null) {
            AppState.reportDate = etDate.getText().toString();
            AppState.reportSupervisor = etSupervisor.getText().toString();
            AppState.reportSection = etSection.getText().toString();
            AppState.reportContractNo = etContractNo.getText().toString();
            AppState.reportDonePrev = etDonePrev.getText().toString();
            AppState.reportTodo = etTodo.getText().toString();
            AppState.reportMaterials = etMaterials.getText().toString();
        }
        super.onDestroyView();
    }
}

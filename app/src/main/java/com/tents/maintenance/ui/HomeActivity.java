package com.tents.maintenance.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.tents.maintenance.MaintenanceApp;
import com.tents.maintenance.R;
import com.tents.maintenance.model.Report;
import com.tents.maintenance.model.ReportSummary;
import com.tents.maintenance.model.Section;
import com.tents.maintenance.storage.AppStorage;
import com.tents.maintenance.util.LangUtil;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private AppStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        storage = MaintenanceApp.from(this).storage();

        findViewById(R.id.btnLang).setOnClickListener(v -> LangUtil.toggle());
        findViewById(R.id.btnAdmin).setOnClickListener(v -> startActivity(new Intent(this, AdminActivity.class)));

        bindSection(R.id.btnKhayat, "khayat");
        bindSection(R.id.btnNajjar, "najjar");
        bindSection(R.id.btnAbwab, "abwab");
        bindSection(R.id.btnAluminum, "aluminum");
        bindSection(R.id.btnKahraba, "kahraba");
        bindSection(R.id.btnMuno, "muno");
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((TextView) findViewById(R.id.btnLang)).setText(LangUtil.isArabic() ? "EN" : "ع");
        renderRecent();
    }

    private void bindSection(int viewId, String sectionId) {
        findViewById(viewId).setOnClickListener(v -> {
            Section section = Section.byId(sectionId);
            MaintenanceApp app = MaintenanceApp.from(this);
            app.currentSection = section;
            app.currentDraft = newDraft(section);
            startActivity(new Intent(this, ReportFormActivity.class));
        });
    }

    private Report newDraft(Section section) {
        Report r = new Report();
        r.sectionId = section.id;
        r.reportType = "kahraba".equals(section.id) ? "كهرباء وتكييف" : "عام";
        r.serialNumber = storage.nextSerialNumber(section.id);
        r.date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
        r.faults.add(new com.tents.maintenance.model.Fault());
        r.acUnits.add(new com.tents.maintenance.model.AcUnit());
        return r;
    }

    private void renderRecent() {
        LinearLayout container = findViewById(R.id.recentContainer);
        container.removeAllViews();
        List<ReportSummary> list = storage.loadMyReports();
        if (list.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.recent_empty);
            empty.setTextColor(getColor(R.color.muted));
            container.addView(empty);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        int shown = Math.min(list.size(), 8);
        for (int i = 0; i < shown; i++) {
            ReportSummary r = list.get(i);
            View item = inflater.inflate(R.layout.item_recent_report, container, false);
            Section sec = Section.byId(r.sectionId);
            String top = (r.serialNumber != null ? r.serialNumber : "-")
                    + " - " + getString(sec.nameRes)
                    + " - " + getString(R.string.contract_no) + " " + (r.contractNumber != null ? r.contractNumber : "-");
            ((TextView) item.findViewById(R.id.txtTop)).setText(top);
            ((TextView) item.findViewById(R.id.txtDate)).setText(r.date != null ? r.date : "-");
            ((TextView) item.findViewById(R.id.txtTech)).setText(
                    getString(R.string.recent_tech, r.technician != null ? r.technician : "-"));
            container.addView(item);
        }
    }
}

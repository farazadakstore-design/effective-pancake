package com.tents.maintenance.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tents.maintenance.MaintenanceApp;
import com.tents.maintenance.R;
import com.tents.maintenance.model.Report;
import com.tents.maintenance.model.Section;
import com.tents.maintenance.storage.AppStorage;
import com.tents.maintenance.util.LangUtil;
import com.tents.maintenance.util.PdfGenerator;
import com.tents.maintenance.util.ReportTextUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class SuccessActivity extends AppCompatActivity {

    private static final String FILE_PROVIDER_AUTHORITY = "com.tents.maintenance.fileprovider";

    private MaintenanceApp app;
    private AppStorage storage;
    private Report draft;
    private Section section;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        app = MaintenanceApp.from(this);
        storage = app.storage();
        draft = app.currentDraft;
        section = app.currentSection;
        if (draft == null || section == null) {
            finish();
            return;
        }

        findViewById(R.id.btnLang).setOnClickListener(v -> LangUtil.toggle());
        findViewById(R.id.btnAdmin).setOnClickListener(v -> startActivity(new Intent(this, AdminActivity.class)));

        ((TextView) findViewById(R.id.txtSuccessTitle)).setText(R.string.success_title);
        ((TextView) findViewById(R.id.txtSuccessSub)).setText(
                getString(R.string.success_sub, draft.contractNumber, getString(section.nameRes)));
        ((TextView) findViewById(R.id.txtSerialBadge)).setText(
                getString(R.string.report_serial_label) + ": " + draft.serialNumber);

        String phone = storage.loadOfficePhone();
        ((TextView) findViewById(R.id.btnWaPdf)).setText(getString(R.string.btn_wa_pdf, phone));

        findViewById(R.id.btnWaPdf).setOnClickListener(v -> withPdf(this::sendPdfToWhatsapp));
        findViewById(R.id.btnPdf).setOnClickListener(v -> withPdf(file -> shareFile(uriFor(file), "application/pdf")));
        findViewById(R.id.btnJson).setOnClickListener(v -> exportJson());
        findViewById(R.id.btnShare).setOnClickListener(v -> shareText(ReportTextUtil.summary(this, draft, section)));
        findViewById(R.id.btnWaText).setOnClickListener(v -> openWhatsAppText());
        findViewById(R.id.btnNew).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((TextView) findViewById(R.id.btnLang)).setText(LangUtil.isArabic() ? "EN" : "ع");
    }

    // ---------------- PDF ----------------

    private void withPdf(Consumer<File> action) {
        android.widget.Toast.makeText(this, R.string.toast_pdf_making, android.widget.Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            try {
                File file = PdfGenerator.generate(this, draft, section, LangUtil.current());
                mainHandler.post(() -> action.accept(file));
            } catch (Exception e) {
                mainHandler.post(() -> android.widget.Toast.makeText(this, R.string.toast_pdf_error, android.widget.Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void sendPdfToWhatsapp(File file) {
        Uri uri = uriFor(file);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/pdf");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.putExtra(Intent.EXTRA_TEXT, ReportTextUtil.summary(this, draft, section));
        send.setPackage("com.whatsapp");
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(send);
        } catch (ActivityNotFoundException e) {
            android.widget.Toast.makeText(this, R.string.toast_no_whatsapp, android.widget.Toast.LENGTH_LONG).show();
            shareFile(uri, "application/pdf");
        }
    }

    private Uri uriFor(File file) {
        return FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, file);
    }

    private void shareFile(Uri uri, String mime) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(mime);
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, null));
    }

    private void shareText(String text) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, null));
    }

    private void openWhatsAppText() {
        String phone = AppStorage.toIntlPhone(storage.loadOfficePhone());
        String text = ReportTextUtil.summary(this, draft, section);
        Uri uri = Uri.parse("https://wa.me/" + phone + "?text=" + Uri.encode(text));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            android.widget.Toast.makeText(this, R.string.toast_send_error, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------- JSON export ----------------

    private void exportJson() {
        executor.execute(() -> {
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(draft);
                File dir = new File(getFilesDir(), "json");
                if (!dir.exists()) dir.mkdirs();
                String name = "report_" + (draft.contractNumber != null && !draft.contractNumber.isEmpty()
                        ? draft.contractNumber.replaceAll("[^A-Za-z0-9_\\-\\u0600-\\u06FF]", "_") : draft.id) + ".json";
                File out = new File(dir, name);
                try (FileWriter w = new FileWriter(out)) {
                    w.write(json);
                }
                mainHandler.post(() -> {
                    android.widget.Toast.makeText(this, R.string.toast_json_done, android.widget.Toast.LENGTH_SHORT).show();
                    shareFile(uriFor(out), "application/json");
                });
            } catch (IOException e) {
                mainHandler.post(() -> android.widget.Toast.makeText(this, R.string.toast_send_error, android.widget.Toast.LENGTH_SHORT).show());
            }
        });
    }
}

package com.tents.maintenance.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.tents.maintenance.MaintenanceApp;
import com.tents.maintenance.R;
import com.tents.maintenance.model.Section;
import com.tents.maintenance.model.Technician;
import com.tents.maintenance.storage.AppStorage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    private static final String FILE_PROVIDER_AUTHORITY = "com.tents.maintenance.fileprovider";

    private AppStorage storage;
    private Map<String, List<Technician>> techs;
    private Section currentSection = Section.ALL.get(0);

    private EditText inputPhone, inputCode, inputName;
    private LinearLayout techListContainer;

    private final ActivityResultLauncher<String> importLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onFilePicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        storage = MaintenanceApp.from(this).storage();
        techs = storage.loadTechs();

        inputPhone = findViewById(R.id.inputPhone);
        inputCode = findViewById(R.id.inputCode);
        inputName = findViewById(R.id.inputName);
        techListContainer = findViewById(R.id.techListContainer);

        inputPhone.setText(storage.loadOfficePhone());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        findViewById(R.id.btnSavePhone).setOnClickListener(v -> {
            String val = inputPhone.getText().toString().trim();
            if (val.isEmpty()) {
                Toast.makeText(this, R.string.toast_phone_invalid, Toast.LENGTH_SHORT).show();
                return;
            }
            storage.saveOfficePhone(val);
            Toast.makeText(this, R.string.toast_phone_saved, Toast.LENGTH_SHORT).show();
        });

        setupSectionSpinner();

        findViewById(R.id.btnAddTech).setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            String code = inputCode.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, R.string.toast_write_name, Toast.LENGTH_SHORT).show();
                return;
            }
            List<Technician> list = techs.computeIfAbsent(currentSection.id, k -> new ArrayList<>());
            list.add(new Technician(code.isEmpty() ? "-" : code, name, null));
            storage.saveTechs(techs);
            inputCode.setText("");
            inputName.setText("");
            renderTechList();
        });

        findViewById(R.id.btnExport).setOnClickListener(v -> exportTechs());
        findViewById(R.id.btnImport).setOnClickListener(v -> importLauncher.launch("application/json"));

        renderTechList();
    }

    private void setupSectionSpinner() {
        Spinner spinner = findViewById(R.id.spinnerSection);
        List<String> labels = new ArrayList<>();
        for (Section s : Section.ALL) labels.add(getString(s.nameRes));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                currentSection = Section.ALL.get(position);
                renderTechList();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void renderTechList() {
        techListContainer.removeAllViews();
        List<Technician> list = techs.get(currentSection.id);
        if (list == null || list.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.admin_no_tech);
            empty.setTextColor(getColor(R.color.muted));
            techListContainer.addView(empty);
            return;
        }
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(this);
        for (Technician t : list) {
            android.view.View row = inflater.inflate(R.layout.item_tech_row, techListContainer, false);
            ((TextView) row.findViewById(R.id.txtTechName)).setText(t.name + "  (" + t.code + ")");
            row.findViewById(R.id.btnDelete).setOnClickListener(v -> {
                list.remove(t);
                storage.saveTechs(techs);
                renderTechList();
            });
            techListContainer.addView(row);
        }
    }

    // ---------------- export / import ----------------

    private void exportTechs() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(techs);
            File dir = new File(getFilesDir(), "json");
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, "technicians.json");
            try (FileWriter w = new FileWriter(out)) {
                w.write(json);
            }
            Uri uri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, out);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/json");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, null));
        } catch (IOException e) {
            Toast.makeText(this, R.string.toast_send_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void onFilePicked(Uri uri) {
        if (uri == null) return;
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) throw new IOException("empty stream");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);

            Gson gson = new Gson();
            Type type = new TypeToken<LinkedHashMap<String, List<Technician>>>() {}.getType();
            Map<String, List<Technician>> imported = gson.fromJson(sb.toString(), type);
            if (imported == null || imported.isEmpty()) throw new IOException("invalid shape");
            for (List<Technician> v : imported.values()) {
                if (v == null) throw new IOException("invalid section");
            }
            techs = imported;
            storage.saveTechs(techs);
            Toast.makeText(this, R.string.toast_import_ok, Toast.LENGTH_SHORT).show();
            renderTechList();
        } catch (Exception e) {
            Toast.makeText(this, R.string.toast_invalid_file, Toast.LENGTH_SHORT).show();
        }
    }
}

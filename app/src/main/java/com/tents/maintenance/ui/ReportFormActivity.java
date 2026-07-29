package com.tents.maintenance.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.tents.maintenance.MaintenanceApp;
import com.tents.maintenance.R;
import com.tents.maintenance.model.AcUnit;
import com.tents.maintenance.model.Fault;
import com.tents.maintenance.model.Report;
import com.tents.maintenance.model.Section;
import com.tents.maintenance.model.Technician;
import com.tents.maintenance.storage.AppStorage;
import com.tents.maintenance.util.ImageUtil;
import com.tents.maintenance.util.LangUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportFormActivity extends AppCompatActivity {

    private static final String FILE_PROVIDER_AUTHORITY = "com.tents.maintenance.fileprovider";

    private MaintenanceApp app;
    private AppStorage storage;
    private Report draft;
    private Section section;
    private List<Technician> techList;

    private EditText inputContract, inputTent, inputAgent, inputDate, inputLocManual, inputTechManual, inputRemaining;
    private Button btnGps;
    private LinearLayout gpsResultContainer, dynamicContainer;
    private Spinner spinnerTech;
    private SignatureView sigTech, sigCustomer;
    private TextView txtSectionTitle, txtSerial;

    private boolean locationHandled = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // --- pending photo target while a camera/gallery pick is in flight ---
    private String pendingKind; // "fault" | "ac"
    private String pendingItemId;
    private String pendingField; // "before" | "after"
    private Uri pendingCaptureUri;
    private File pendingCaptureFile;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) doLaunchCamera(); else Toast.makeText(this, R.string.camera_permission_needed, Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grants -> {
                boolean fine = Boolean.TRUE.equals(grants.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarse = Boolean.TRUE.equals(grants.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (fine || coarse) doFetchLocation();
                else Toast.makeText(this, R.string.gps_permission_needed, Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && pendingCaptureUri != null) handlePickedPhoto(pendingCaptureUri);
            });

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) handlePickedPhoto(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_form);

        app = MaintenanceApp.from(this);
        storage = app.storage();
        draft = app.currentDraft;
        section = app.currentSection;
        if (draft == null || section == null) {
            finish();
            return;
        }
        techList = storage.loadTechs().get(section.id);
        if (techList == null) techList = new ArrayList<>();

        bindViews();
        setupHeader();
        setupBasicFields();
        setupGps();
        setupTechnician();
        renderDynamicSection();
        setupSignatures();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSubmit).setOnClickListener(v -> onSubmit());
    }

    private void bindViews() {
        txtSectionTitle = findViewById(R.id.txtSectionTitle);
        txtSerial = findViewById(R.id.txtSerial);
        inputContract = findViewById(R.id.inputContract);
        inputTent = findViewById(R.id.inputTent);
        inputAgent = findViewById(R.id.inputAgent);
        inputDate = findViewById(R.id.inputDate);
        btnGps = findViewById(R.id.btnGps);
        gpsResultContainer = findViewById(R.id.gpsResultContainer);
        inputLocManual = findViewById(R.id.inputLocManual);
        spinnerTech = findViewById(R.id.spinnerTech);
        inputTechManual = findViewById(R.id.inputTechManual);
        dynamicContainer = findViewById(R.id.dynamicContainer);
        inputRemaining = findViewById(R.id.inputRemaining);
        sigTech = findViewById(R.id.sigTech);
        sigCustomer = findViewById(R.id.sigCustomer);
    }

    private void setupHeader() {
        txtSectionTitle.setText(section.icon + " " + getString(R.string.report_for) + " " + getString(section.nameRes));
        txtSerial.setText(draft.serialNumber);
    }

    private void setupBasicFields() {
        inputContract.setText(draft.contractNumber);
        watch(inputContract, s -> draft.contractNumber = s);
        inputTent.setText(draft.tentSize);
        watch(inputTent, s -> draft.tentSize = s);
        inputAgent.setText(draft.agent);
        watch(inputAgent, s -> draft.agent = s);
        inputRemaining.setText(draft.remainingFaults);
        watch(inputRemaining, s -> draft.remainingFaults = s);
        inputLocManual.setText(draft.locationManual);
        watch(inputLocManual, s -> draft.locationManual = s);

        inputDate.setText(draft.date);
        inputDate.setOnClickListener(v -> openDatePicker());
        findViewById(R.id.inputDate).setFocusable(false);
    }

    private void openDatePicker() {
        Calendar cal = Calendar.getInstance();
        try {
            if (draft.date != null && !draft.date.isEmpty()) {
                cal.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(draft.date));
            }
        } catch (Exception ignored) {
        }
        new android.app.DatePickerDialog(this, (DatePicker view, int y, int m, int d) -> {
            String date = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
            draft.date = date;
            inputDate.setText(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private interface TextSink { void accept(String s); }

    private void watch(EditText et, TextSink sink) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { sink.accept(s.toString()); }
        });
    }

    // ---------------- GPS ----------------

    private void setupGps() {
        btnGps.setOnClickListener(v -> requestLocation());
        renderGpsResult();
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            return;
        }
        doFetchLocation();
    }

    private void doFetchLocation() {
        Toast.makeText(this, R.string.gps_locating, Toast.LENGTH_SHORT).show();
        locationHandled = false;
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            Location last = null;
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last == null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (last != null) {
                onLocationFound(last);
                return;
            }
            String provider = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
            lm.requestSingleUpdate(provider, new LocationListener() {
                @Override public void onLocationChanged(@NonNull Location location) { onLocationFound(location); }
                @Override public void onProviderDisabled(@NonNull String provider) {}
                @Override public void onProviderEnabled(@NonNull String provider) {}
            }, Looper.getMainLooper());
            mainHandler.postDelayed(() -> {
                if (!locationHandled) Toast.makeText(this, R.string.gps_failed, Toast.LENGTH_SHORT).show();
            }, 10000);
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.gps_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void onLocationFound(Location loc) {
        if (locationHandled) return;
        locationHandled = true;
        draft.locationLat = loc.getLatitude();
        draft.locationLng = loc.getLongitude();
        renderGpsResult();
        Toast.makeText(this, R.string.gps_done, Toast.LENGTH_SHORT).show();
    }

    private void renderGpsResult() {
        gpsResultContainer.removeAllViews();
        TextView tv = new TextView(this);
        if (draft.locationLat == null || draft.locationLng == null) {
            tv.setText(R.string.gps_none);
            tv.setTextColor(getColor(R.color.muted));
            tv.setTextSize(13);
        } else {
            String txt = "📍 " + String.format(Locale.US, "%.5f, %.5f", draft.locationLat, draft.locationLng)
                    + "\n" + getString(R.string.gps_open_maps);
            tv.setText(txt);
            tv.setTextColor(getColor(R.color.brand_dark));
            tv.setTextSize(13);
            tv.setBackgroundResource(R.drawable.bg_gps_box);
            tv.setPadding(24, 20, 24, 20);
            tv.setClickable(true);
            tv.setOnClickListener(v -> {
                Uri uri = Uri.parse("https://www.google.com/maps?q=" + draft.locationLat + "," + draft.locationLng);
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            });
        }
        gpsResultContainer.addView(tv);
    }

    // ---------------- technician ----------------

    private void setupTechnician() {
        String lang = LangUtil.current();
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.tech_select_hint));
        for (Technician t : techList) labels.add(t.displayName(lang) + " (" + t.code + ")");
        labels.add(getString(R.string.tech_manual_option));
        int manualIndex = labels.size() - 1;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTech.setAdapter(adapter);

        int matchIndex = -1;
        for (int i = 0; i < techList.size(); i++) {
            if (techList.get(i).displayName(lang).equals(draft.technician)) { matchIndex = i + 1; break; }
        }
        if (matchIndex >= 0) {
            spinnerTech.setSelection(matchIndex);
            inputTechManual.setVisibility(View.GONE);
        } else if (draft.technician != null && !draft.technician.isEmpty()) {
            spinnerTech.setSelection(manualIndex);
            inputTechManual.setVisibility(View.VISIBLE);
            inputTechManual.setText(draft.technician);
        }

        spinnerTech.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == manualIndex) {
                    inputTechManual.setVisibility(View.VISIBLE);
                    draft.technician = inputTechManual.getText().toString();
                } else if (position == 0) {
                    inputTechManual.setVisibility(View.GONE);
                    draft.technician = "";
                } else {
                    inputTechManual.setVisibility(View.GONE);
                    draft.technician = techList.get(position - 1).displayName(lang);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        watch(inputTechManual, s -> draft.technician = s);
    }

    // ---------------- faults / AC units / electrical checklist ----------------

    private void renderDynamicSection() {
        dynamicContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (section.isElectricalSection()) {
            View checklist = inflater.inflate(R.layout.card_electrical_checklist, dynamicContainer, false);
            CheckBox chkWiring = checklist.findViewById(R.id.chkWiring);
            CheckBox chkDb = checklist.findViewById(R.id.chkDb);
            CheckBox chkLighting = checklist.findViewById(R.id.chkLighting);
            chkWiring.setChecked(draft.electricalChecks.wiring);
            chkDb.setChecked(draft.electricalChecks.db);
            chkLighting.setChecked(draft.electricalChecks.lighting);
            chkWiring.setOnCheckedChangeListener((b, checked) -> draft.electricalChecks.wiring = checked);
            chkDb.setOnCheckedChangeListener((b, checked) -> draft.electricalChecks.db = checked);
            chkLighting.setOnCheckedChangeListener((b, checked) -> draft.electricalChecks.lighting = checked);
            dynamicContainer.addView(checklist);

            LinearLayout card = createCard(R.string.ac_title);
            for (int i = 0; i < draft.acUnits.size(); i++) {
                bindAcCard(card, inflater, draft.acUnits.get(i), i, draft.acUnits.size() > 1);
            }
            Button add = smallOutlineButton(getString(R.string.add_ac));
            add.setOnClickListener(v -> { draft.acUnits.add(new AcUnit()); renderDynamicSection(); });
            card.addView(add);
            dynamicContainer.addView(card);
        } else {
            LinearLayout card = createCard(R.string.faults_title);
            for (int i = 0; i < draft.faults.size(); i++) {
                bindFaultCard(card, inflater, draft.faults.get(i), i, draft.faults.size() > 1);
            }
            Button add = smallOutlineButton(getString(R.string.add_fault));
            add.setOnClickListener(v -> { draft.faults.add(new Fault()); renderDynamicSection(); });
            card.addView(add);
            dynamicContainer.addView(card);
        }
    }

    private LinearLayout createCard(int titleRes) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        int pad = dp(16);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        card.setLayoutParams(lp);
        TextView title = new TextView(this);
        title.setText(titleRes);
        title.setTextSize(16);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setTextColor(getColor(R.color.text_main));
        title.setPadding(0, 0, 0, dp(6));
        card.addView(title);
        return card;
    }

    private Button smallOutlineButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTextColor(getColor(R.color.brand));
        b.setBackgroundResource(R.drawable.bg_btn_outline);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(40));
        lp.topMargin = dp(4);
        b.setLayoutParams(lp);
        b.setPadding(dp(14), 0, dp(14), 0);
        return b;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void bindFaultCard(LinearLayout parent, LayoutInflater inflater, Fault f, int index, boolean showRemove) {
        View v = inflater.inflate(R.layout.item_fault_card, parent, false);
        ((TextView) v.findViewById(R.id.txtCardLabel)).setText(getString(R.string.fault_label) + " " + (index + 1));
        TextView remove = v.findViewById(R.id.btnRemove);
        remove.setVisibility(showRemove ? View.VISIBLE : View.GONE);
        remove.setOnClickListener(x -> { draft.faults.remove(f); renderDynamicSection(); });

        EditText desc = v.findViewById(R.id.inputDescription);
        desc.setText(f.description);
        watch(desc, s -> f.description = s);
        EditText repair = v.findViewById(R.id.inputRepair);
        repair.setText(f.repair);
        watch(repair, s -> f.repair = s);

        LinearLayout photosRow = v.findViewById(R.id.photosRow);
        photosRow.addView(buildPhotoSlot(inflater, photosRow, getString(R.string.photo_before), f.beforePhotoPath, "fault", f.id, "before"));
        photosRow.addView(buildPhotoSlot(inflater, photosRow, getString(R.string.photo_after), f.afterPhotoPath, "fault", f.id, "after"));

        parent.addView(v);
    }

    private void bindAcCard(LinearLayout parent, LayoutInflater inflater, AcUnit u, int index, boolean showRemove) {
        View v = inflater.inflate(R.layout.item_ac_card, parent, false);
        ((TextView) v.findViewById(R.id.txtCardLabel)).setText(getString(R.string.ac_unit_label) + " " + (index + 1));
        TextView remove = v.findViewById(R.id.btnRemove);
        remove.setVisibility(showRemove ? View.VISIBLE : View.GONE);
        remove.setOnClickListener(x -> { draft.acUnits.remove(u); renderDynamicSection(); });

        EditText unit = v.findViewById(R.id.inputUnitNumber);
        unit.setText(u.unitNumber);
        watch(unit, s -> u.unitNumber = s);
        EditText fault = v.findViewById(R.id.inputFault);
        fault.setText(u.fault);
        watch(fault, s -> u.fault = s);
        EditText repair = v.findViewById(R.id.inputRepair);
        repair.setText(u.repair);
        watch(repair, s -> u.repair = s);
        EditText gas = v.findViewById(R.id.inputGas);
        gas.setText(u.gasPressure);
        watch(gas, s -> u.gasPressure = s);
        EditText volt = v.findViewById(R.id.inputVoltage);
        volt.setText(u.voltage);
        watch(volt, s -> u.voltage = s);
        EditText cooling = v.findViewById(R.id.inputCooling);
        cooling.setText(u.coolingTemp);
        watch(cooling, s -> u.coolingTemp = s);

        LinearLayout photosRow = v.findViewById(R.id.photosRow);
        photosRow.addView(buildPhotoSlot(inflater, photosRow, getString(R.string.photo_before), u.beforePhotoPath, "ac", u.id, "before"));
        photosRow.addView(buildPhotoSlot(inflater, photosRow, getString(R.string.photo_after), u.afterPhotoPath, "ac", u.id, "after"));

        parent.addView(v);
    }

    private View buildPhotoSlot(LayoutInflater inflater, ViewGroup parent, String label, @Nullable String photoPath,
                                 String kind, String itemId, String field) {
        View v = inflater.inflate(R.layout.item_photo_slot, parent, false);
        ((TextView) v.findViewById(R.id.txtSlotLabel)).setText(label);
        ImageView preview = v.findViewById(R.id.imgPreview);
        if (photoPath != null) {
            Bitmap bmp = BitmapFactory.decodeFile(photoPath);
            if (bmp != null) {
                preview.setImageBitmap(bmp);
                preview.setVisibility(View.VISIBLE);
            }
        }
        v.findViewById(R.id.btnCamera).setOnClickListener(x -> {
            pendingKind = kind; pendingItemId = itemId; pendingField = field;
            launchCamera();
        });
        v.findViewById(R.id.btnGallery).setOnClickListener(x -> {
            pendingKind = kind; pendingItemId = itemId; pendingField = field;
            pickImageLauncher.launch("image/*");
        });
        return v;
    }

    // ---------------- photo capture ----------------

    private void launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        doLaunchCamera();
    }

    private void doLaunchCamera() {
        try {
            pendingCaptureFile = ImageUtil.newCaptureFile(this);
            pendingCaptureUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, pendingCaptureFile);
            takePictureLauncher.launch(pendingCaptureUri);
        } catch (Exception e) {
            Toast.makeText(this, R.string.toast_pdf_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePickedPhoto(Uri uri) {
        String kind = pendingKind, itemId = pendingItemId, field = pendingField;
        new Thread(() -> {
            try {
                String prefix = draft.id + "_" + itemId + "_" + field;
                String path = ImageUtil.savePhoto(this, uri, prefix);
                mainHandler.post(() -> {
                    applyPhotoPath(kind, itemId, field, path);
                    renderDynamicSection();
                });
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this, R.string.toast_pdf_error, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void applyPhotoPath(String kind, String itemId, String field, String path) {
        if ("fault".equals(kind)) {
            for (Fault f : draft.faults) {
                if (f.id.equals(itemId)) {
                    if ("before".equals(field)) f.beforePhotoPath = path; else f.afterPhotoPath = path;
                }
            }
        } else {
            for (AcUnit u : draft.acUnits) {
                if (u.id.equals(itemId)) {
                    if ("before".equals(field)) u.beforePhotoPath = path; else u.afterPhotoPath = path;
                }
            }
        }
    }

    // ---------------- signatures ----------------

    private void setupSignatures() {
        findViewById(R.id.btnClearSigTech).setOnClickListener(v -> { sigTech.clear(); draft.signaturePath = null; });
        findViewById(R.id.btnClearSigCustomer).setOnClickListener(v -> { sigCustomer.clear(); draft.customerSignaturePath = null; });
        if (draft.signaturePath != null) {
            Bitmap bmp = BitmapFactory.decodeFile(draft.signaturePath);
            if (bmp != null) sigTech.loadBitmap(bmp);
        }
        if (draft.customerSignaturePath != null) {
            Bitmap bmp = BitmapFactory.decodeFile(draft.customerSignaturePath);
            if (bmp != null) sigCustomer.loadBitmap(bmp);
        }
    }

    // ---------------- submit ----------------

    private void onSubmit() {
        if (draft.contractNumber == null || draft.contractNumber.trim().isEmpty()) {
            Toast.makeText(this, R.string.err_contract, Toast.LENGTH_SHORT).show(); return;
        }
        if (draft.tentSize == null || draft.tentSize.trim().isEmpty()) {
            Toast.makeText(this, R.string.err_tent, Toast.LENGTH_SHORT).show(); return;
        }
        if (draft.technician == null || draft.technician.trim().isEmpty()) {
            Toast.makeText(this, R.string.err_tech, Toast.LENGTH_SHORT).show(); return;
        }
        if (!sigTech.hasContent()) {
            Toast.makeText(this, R.string.err_sig, Toast.LENGTH_SHORT).show(); return;
        }

        try {
            Bitmap techBmp = sigTech.exportBitmap();
            if (techBmp != null) draft.signaturePath = ImageUtil.saveBitmap(this, techBmp, draft.id + "_sig_tech");
            if (sigCustomer.hasContent()) {
                Bitmap custBmp = sigCustomer.exportBitmap();
                if (custBmp != null) draft.customerSignaturePath = ImageUtil.saveBitmap(this, custBmp, draft.id + "_sig_cust");
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.toast_pdf_error, Toast.LENGTH_SHORT).show();
            return;
        }

        draft.createdAt = System.currentTimeMillis();
        storage.saveMyReport(draft);
        startActivity(new Intent(this, SuccessActivity.class));
        finish();
    }
}

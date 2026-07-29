package com.factory.techmanager.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Persistence layer. Replaces the web app's localStorage with SharedPreferences,
 * storing the same JSON shapes so the logic ported from the original JS stays 1:1.
 */
public class Repository {
    private static final String PREFS = "dept_app_prefs";

    private static final String K_EXTRA_TECHS = "extra_techs";
    private static final String K_SETTINGS = "dept_settings";
    private static final String K_STATUSES = "dept_statuses";
    private static final String K_REPORTS = "dept_reports";
    private static final String K_EVALUATIONS = "dept_evaluations";
    private static final String K_LANG = "dept_lang";
    private static final String K_SEQ_PREFIX = "report_seq_";

    private final SharedPreferences prefs;

    public Repository(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ───────────────────────── LANGUAGE ─────────────────────────
    public String getLang() {
        return prefs.getString(K_LANG, "ar");
    }

    public void setLang(String lang) {
        prefs.edit().putString(K_LANG, lang).apply();
    }

    // ───────────────────────── TECHNICIANS ─────────────────────────
    public List<Technician> getExtraTechs() {
        List<Technician> list = new ArrayList<>();
        String raw = prefs.getString(K_EXTRA_TECHS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) list.add(Technician.fromJson(arr.getJSONObject(i)));
        } catch (JSONException ignored) {}
        return list;
    }

    public void saveExtraTechs(List<Technician> techs) {
        JSONArray arr = new JSONArray();
        try {
            for (Technician t : techs) arr.put(t.toJson());
        } catch (JSONException ignored) {}
        prefs.edit().putString(K_EXTRA_TECHS, arr.toString()).apply();
    }

    public List<Technician> getAllTechs() {
        List<Technician> all = new ArrayList<>(Seed.ALL_TECHS);
        all.addAll(getExtraTechs());
        return all;
    }

    public Technician findTechById(String id) {
        if (id == null) return null;
        for (Technician t : getAllTechs()) if (t.id.equals(id)) return t;
        return null;
    }

    public boolean fileNoExists(String fileNo) {
        for (Technician t : getAllTechs()) if (t.fileNo != null && t.fileNo.equals(fileNo)) return true;
        return false;
    }

    public void addTechnician(Technician t) {
        List<Technician> extras = getExtraTechs();
        extras.add(t);
        saveExtraTechs(extras);
    }

    public void removeExtraTech(String id) {
        List<Technician> extras = getExtraTechs();
        List<Technician> filtered = new ArrayList<>();
        for (Technician t : extras) if (!t.id.equals(id)) filtered.add(t);
        saveExtraTechs(filtered);
    }

    // ───────────────────────── SETTINGS ─────────────────────────
    public SettingsModel getSettings() {
        String raw = prefs.getString(K_SETTINGS, null);
        if (raw == null) return SettingsModel.defaults();
        try {
            return SettingsModel.fromJson(new JSONObject(raw));
        } catch (JSONException e) {
            return SettingsModel.defaults();
        }
    }

    public void saveSettings(SettingsModel s) {
        try {
            prefs.edit().putString(K_SETTINGS, s.toJson().toString()).apply();
        } catch (JSONException ignored) {}
    }

    // ───────────────────────── STATUSES ─────────────────────────
    private JSONObject getStatusesRaw() {
        String raw = prefs.getString(K_STATUSES, "{}");
        try {
            return new JSONObject(raw);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    private void saveStatusesRaw(JSONObject obj) {
        prefs.edit().putString(K_STATUSES, obj.toString()).apply();
    }

    public TechStatus getStatus(String techId) {
        JSONObject all = getStatusesRaw();
        JSONObject o = all.optJSONObject(techId);
        if (o == null) return new TechStatus();
        TechStatus s = TechStatus.fromJson(o);
        // Auto-clear expired temp status (mirrors JS getStatus())
        if (s.tempEnd != null && !s.tempEnd.isEmpty()) {
            try {
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date end = fmt.parse(s.tempEnd);
                if (end != null && end.before(new Date())) {
                    s.temp = null;
                    s.tempEnd = null;
                    setStatus(techId, s);
                }
            } catch (Exception ignored) {}
        }
        return s;
    }

    public void setStatus(String techId, String field, String value) {
        TechStatus s = getStatus(techId);
        if ("main".equals(field)) s.main = value;
        else if ("temp".equals(field)) s.temp = (value == null || value.isEmpty()) ? null : value;
        else if ("tempEnd".equals(field)) s.tempEnd = (value == null || value.isEmpty()) ? null : value;
        setStatus(techId, s);
    }

    private void setStatus(String techId, TechStatus s) {
        JSONObject all = getStatusesRaw();
        try {
            all.put(techId, s.toJson());
        } catch (JSONException ignored) {}
        saveStatusesRaw(all);
    }

    /** Clears open-ended temp statuses (no end date) — called after a report is saved, matches JS day-reset. */
    public void clearOpenEndedTempStatuses() {
        JSONObject all = getStatusesRaw();
        java.util.Iterator<String> keys = all.keys();
        List<String> ids = new ArrayList<>();
        while (keys.hasNext()) ids.add(keys.next());
        for (String id : ids) {
            JSONObject o = all.optJSONObject(id);
            if (o == null) continue;
            TechStatus s = TechStatus.fromJson(o);
            if (s.temp != null && (s.tempEnd == null || s.tempEnd.isEmpty())) {
                s.temp = null;
                try { all.put(id, s.toJson()); } catch (JSONException ignored) {}
            }
        }
        saveStatusesRaw(all);
    }

    // ───────────────────────── REPORTS ─────────────────────────
    public List<Report> getReports() {
        List<Report> list = new ArrayList<>();
        String raw = prefs.getString(K_REPORTS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) list.add(Report.fromJson(arr.getJSONObject(i)));
        } catch (JSONException ignored) {}
        return list;
    }

    private void saveAllReports(List<Report> reports) {
        JSONArray arr = new JSONArray();
        try {
            for (Report r : reports) arr.put(r.toJson());
        } catch (JSONException ignored) {}
        prefs.edit().putString(K_REPORTS, arr.toString()).apply();
    }

    public void saveReport(Report r) {
        List<Report> reports = getReports();
        reports.add(0, r);
        if (reports.size() > 200) reports = reports.subList(0, 200);
        saveAllReports(reports);
    }

    public void deleteReport(String id) {
        List<Report> reports = getReports();
        List<Report> filtered = new ArrayList<>();
        for (Report r : reports) if (!r.id.equals(id)) filtered.add(r);
        saveAllReports(filtered);
    }

    public Report findReport(String id) {
        for (Report r : getReports()) if (r.id.equals(id)) return r;
        return null;
    }

    public String getNextReportNo(String deptId) {
        String prefix = Seed.deptPrefix(deptId);
        String key = K_SEQ_PREFIX + deptId;
        int seq = prefs.getInt(key, 0) + 1;
        prefs.edit().putInt(key, seq).apply();
        return prefix + String.format(Locale.US, "%03d", seq);
    }

    // ───────────────────────── EVALUATIONS ─────────────────────────
    public List<Evaluation> getEvaluations() {
        List<Evaluation> list = new ArrayList<>();
        String raw = prefs.getString(K_EVALUATIONS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) list.add(Evaluation.fromJson(arr.getJSONObject(i)));
        } catch (JSONException ignored) {}
        return list;
    }

    public void saveEvaluations(List<Evaluation> evals) {
        JSONArray arr = new JSONArray();
        try {
            for (Evaluation e : evals) arr.put(e.toJson());
        } catch (JSONException ignored) {}
        prefs.edit().putString(K_EVALUATIONS, arr.toString()).apply();
    }

    public void upsertEvaluation(String techId, String techName, String techNameAr, String deptId,
                                  String fileNo, String month, int score, String note) {
        List<Evaluation> evals = getEvaluations();
        String key = techId + "_" + month;
        Evaluation found = null;
        for (Evaluation e : evals) if (e.id.equals(key)) { found = e; break; }
        Evaluation obj = new Evaluation();
        obj.id = key;
        obj.techId = techId;
        obj.techName = techName;
        obj.techNameAr = techNameAr;
        obj.deptId = deptId;
        obj.fileNo = fileNo;
        obj.month = month;
        obj.score = score;
        obj.note = note;
        obj.updatedAt = new Date().toString();
        if (found != null) evals.set(evals.indexOf(found), obj);
        else evals.add(obj);
        saveEvaluations(evals);
    }

    public void deleteEvaluation(String id) {
        List<Evaluation> evals = getEvaluations();
        List<Evaluation> filtered = new ArrayList<>();
        for (Evaluation e : evals) if (!e.id.equals(id)) filtered.add(e);
        saveEvaluations(filtered);
    }

    public Evaluation findEvalForMonth(String techId, String month) {
        for (Evaluation e : getEvaluations()) if (e.techId.equals(techId) && e.month.equals(month)) return e;
        return null;
    }

    // ───────────────────────── HELPERS ─────────────────────────
    /** Work hours = (checkOut - checkIn) in 24h, minus configured break minutes, floored at 0. Formatted "0.0". */
    public String calcHours(String checkIn, String checkOut) {
        if (checkIn == null || checkOut == null || checkIn.isEmpty() || checkOut.isEmpty()) return "";
        try {
            String[] p1 = checkIn.split(":");
            String[] p2 = checkOut.split(":");
            int h1 = Integer.parseInt(p1[0]), m1 = Integer.parseInt(p1[1]);
            int h2 = Integer.parseInt(p2[0]), m2 = Integer.parseInt(p2[1]);
            int d = (h2 * 60 + m2) - (h1 * 60 + m1);
            if (d < 0) d += 1440;
            int breakMin = getSettings().breakMinutes;
            d -= breakMin;
            if (d < 0) d = 0;
            double hours = d / 60.0;
            return String.format(Locale.US, "%.1f", hours);
        } catch (Exception e) {
            return "";
        }
    }
}

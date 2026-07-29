package com.tents.maintenance.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tents.maintenance.model.Report;
import com.tents.maintenance.model.ReportSummary;
import com.tents.maintenance.model.Section;
import com.tents.maintenance.model.Technician;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Local persistence layer. Mirrors what the original web app kept in localStorage:
 * technician lists per section, the running per-section report-serial counter,
 * a capped list of recent report summaries, the office WhatsApp number and the
 * chosen UI language.
 */
public class AppStorage {
    private static final String PREFS = "app_storage";
    private static final String KEY_TECHS = "tech_lists_v1";
    private static final String KEY_MY_REPORTS = "my_reports_v1";
    private static final String KEY_SEQ = "report_seq_v1";
    private static final String KEY_PHONE = "office_whatsapp_v1";
    private static final String KEY_LANG = "lang_v1";
    private static final int MAX_RECENT = 200;

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public AppStorage(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- language ----------
    public String getLanguage() {
        return prefs.getString(KEY_LANG, "ar");
    }

    public void setLanguage(String lang) {
        prefs.edit().putString(KEY_LANG, lang).apply();
    }

    // ---------- office phone ----------
    public String loadOfficePhone() {
        return prefs.getString(KEY_PHONE, "0554972472");
    }

    public void saveOfficePhone(String phone) {
        prefs.edit().putString(KEY_PHONE, phone).apply();
    }

    /** Converts a local Saudi number (05xxxxxxxx) to international format (9665xxxxxxxx). */
    public static String toIntlPhone(String local) {
        String digits = local == null ? "" : local.replaceAll("\\D", "");
        if (digits.startsWith("966")) return digits;
        if (digits.startsWith("0")) return "966" + digits.substring(1);
        if (digits.length() == 9) return "966" + digits;
        return digits;
    }

    // ---------- technicians ----------
    public Map<String, List<Technician>> loadTechs() {
        String raw = prefs.getString(KEY_TECHS, null);
        if (raw != null) {
            try {
                Type type = new TypeToken<LinkedHashMap<String, List<Technician>>>() {}.getType();
                Map<String, List<Technician>> map = gson.fromJson(raw, type);
                if (map != null) return map;
            } catch (Exception ignored) {
            }
        }
        Map<String, List<Technician>> defaults = defaultTechs();
        saveTechs(defaults);
        return defaults;
    }

    public void saveTechs(Map<String, List<Technician>> techs) {
        prefs.edit().putString(KEY_TECHS, gson.toJson(techs)).apply();
    }

    // ---------- report serial numbers ----------
    public String nextSerialNumber(String sectionId) {
        Map<String, Integer> seq = loadSeq();
        int next = (seq.containsKey(sectionId) && seq.get(sectionId) != null ? seq.get(sectionId) : 0) + 1;
        seq.put(sectionId, next);
        saveSeq(seq);
        String code = Section.byId(sectionId).code;
        return code + "-" + String.format(Locale.US, "%04d", next);
    }

    private Map<String, Integer> loadSeq() {
        String raw = prefs.getString(KEY_SEQ, null);
        if (raw == null) return new LinkedHashMap<>();
        try {
            Type type = new TypeToken<LinkedHashMap<String, Integer>>() {}.getType();
            Map<String, Integer> map = gson.fromJson(raw, type);
            return map != null ? map : new LinkedHashMap<>();
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private void saveSeq(Map<String, Integer> seq) {
        prefs.edit().putString(KEY_SEQ, gson.toJson(seq)).apply();
    }

    // ---------- recent reports ----------
    public List<ReportSummary> loadMyReports() {
        String raw = prefs.getString(KEY_MY_REPORTS, null);
        if (raw == null) return new ArrayList<>();
        try {
            Type type = new TypeToken<ArrayList<ReportSummary>>() {}.getType();
            List<ReportSummary> list = gson.fromJson(raw, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void saveMyReport(Report r) {
        List<ReportSummary> list = loadMyReports();
        list.add(0, new ReportSummary(r));
        if (list.size() > MAX_RECENT) list = new ArrayList<>(list.subList(0, MAX_RECENT));
        prefs.edit().putString(KEY_MY_REPORTS, gson.toJson(list)).apply();
    }

    // ---------- default technician seed data (matches the original app) ----------
    private Map<String, List<Technician>> defaultTechs() {
        Map<String, List<Technician>> m = new LinkedHashMap<>();

        List<Technician> khayat = new ArrayList<>();
        khayat.add(new Technician("2030", "جاكير ظفير أحمد", "Jakir Zafoor Ahmed"));
        khayat.add(new Technician("6012", "مير والي شاه قبول شاه", "Mir Wali Shah Qabool Shah"));
        khayat.add(new Technician("2220", "نيرمول شاندرا ناما", "Nirmol Chandra Nama"));
        khayat.add(new Technician("8004", "محمد أياز أصيل زاديا", "Mohammed Ayaz Aseel Zadia"));
        khayat.add(new Technician("2007", "نور الدين مقبل أحمد", "Nur Uddin Muqbil Ahmed"));
        m.put("khayat", khayat);

        List<Technician> najjar = new ArrayList<>();
        najjar.add(new Technician("2058", "مانيراج براجاباتي", "Maniraj Prajapati"));
        najjar.add(new Technician("6014", "وليد عبد النبي", "Waleed Abdel Nabi"));
        najjar.add(new Technician("2140", "روج محمد ميا", "Roj Mohammed Mia"));
        najjar.add(new Technician("8224", "راشبال سينغ", "Rashpal Singh"));
        najjar.add(new Technician("2057", "أوم براكاش تشودري", "Om Prakash Chaudhary"));
        najjar.add(new Technician("9283", "هاركاش شارما", "Harkash Sharma"));
        najjar.add(new Technician("8015", "عبدالله جان حسين", "Abdullah Jan Hussain"));
        najjar.add(new Technician("8021", "دانيش خان سليم خان", "Danish Khan Salim Khan"));
        najjar.add(new Technician("9461", "ديراج كومار", "Dheeraj Kumar"));
        najjar.add(new Technician("8022", "لقمان علي محمد جميل", "Luqman Ali Mohammed Jameel"));
        m.put("najjar", najjar);

        m.put("abwab", new ArrayList<>());
        m.put("aluminum", new ArrayList<>());

        List<Technician> kahraba = new ArrayList<>();
        kahraba.add(new Technician("2004", "راسيك مونوركاندي", "Rasik Munorkandi"));
        kahraba.add(new Technician("8116", "تبارك علي واحيد", "Tabarak Ali Waheed"));
        kahraba.add(new Technician("2045", "خورشيد والي خان", "Khurshid Wali Khan"));
        kahraba.add(new Technician("2031", "شهيد الإسلام إنامول الحق", "Shahid Ul Islam Enamul Haq"));
        kahraba.add(new Technician("9323", "أمير علام", "Amir Alam"));
        kahraba.add(new Technician("2027", "محمد عبد القادر باتواري", "Mohammed Abdul Qader Patwari"));
        kahraba.add(new Technician("2032", "حسن الدين إدريش ميان", "Hasan Uddin Idrish Miah"));
        kahraba.add(new Technician("2083", "بارفيندر سينغ دارشان لال", "Parvinder Singh Darshan Lal"));
        kahraba.add(new Technician("2084", "رامبراويش ياداف سري كيشون ياداف", "Rambravesh Yadav Sri Kishun Yadav"));
        kahraba.add(new Technician("8154", "بريندار كومار", "Brindar Kumar"));
        kahraba.add(new Technician("7150", "ماجد فؤاد", "Majed Fouad"));
        kahraba.add(new Technician("9465", "موساييستا أكابال", "Musayesta Akabal"));
        kahraba.add(new Technician("8137", "أفتاب حسين م الأنصاري", "Aftab Hussain M Al-Ansari"));
        m.put("kahraba", kahraba);

        List<Technician> muno = new ArrayList<>();
        muno.add(new Technician("2136", "رافي كومار جامنا داس", "Ravi Kumar Jamna Das"));
        muno.add(new Technician("2147", "مينتو داس سوناتان داس", "Minto Das Sonatan Das"));
        muno.add(new Technician("8119", "زافيد أختر صافي", "Zavid Akhtar Safi"));
        muno.add(new Technician("2026", "أنامول الحق رفيق أحمد", "Anamul Haq Rafiq Ahmed"));
        muno.add(new Technician("8118", "برافيند كومار جايسوال", "Pravind Kumar Jaiswal"));
        muno.add(new Technician("8267", "مد أجاز علام", "Md Ajaz Alam"));
        muno.add(new Technician("8266", "دانلال ياداف", "Dhanlal Yadav"));
        muno.add(new Technician("8269", "مد ساجد", "Md Sajid"));
        m.put("muno", muno);

        return m;
    }
}

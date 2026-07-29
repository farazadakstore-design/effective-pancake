package com.factory.techmanager.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Static seed data ported from the original web app (DEPTS + ALL_TECHS). */
public class Seed {

    public static final List<Department> DEPTS = Arrays.asList(
            new Department("electricians", "كهرباء", "Electricians", "⚡", "2"),
            new Department("carpenters", "نجارين", "Carpenters", "🪚", "1"),
            new Department("aluminum", "ألمنيوم", "Aluminum", "🔩", "3"),
            new Department("tailors", "خياطين", "Tailors", "🧵", "4")
    );

    public static Department findDept(String id) {
        for (Department d : DEPTS) if (d.id.equals(id)) return d;
        return null;
    }

    public static final List<Technician> ALL_TECHS = new ArrayList<>();

    static {
        // ── كهرباء / Electricians ──
        add("e1", "electricians", "2004", "Rasik Munnurkandy", "راسيك", "", "الهند");
        add("e2", "electricians", "8116", "Tabarak Ali Waheed", "تبارك", "", "الهند");
        add("e3", "electricians", "2045", "Khursheed Wali Khan", "خورشيد والي خان", "", "الهند");
        add("e4", "electricians", "2031", "Shahidul Islam Enamul Hoque", "شهيد الإسلام إنامول الحق", "", "بنغلادش");
        add("e5", "electricians", "9323", "Amir Aalam", "امير علام", "", "الهند");
        add("e6", "electricians", "2027", "Mohammed Abdul Kader Patwary", "محمد عبد القادر باتواري", "", "بنغلادش");
        add("e7", "electricians", "2032", "Hasan Uddin Eadrish Miah", "حسن الدين إدريش مياه", "", "بنغلادش");
        add("e8", "electricians", "2083", "Parwinder Singh Darshan Lal", "بارويندر سينغ دارشان لال", "", "الهند");
        add("e9", "electricians", "2084", "Rampravesh Yadav Sri Kishun", "رامبرافيش ياداف سري كيشون ياداف", "", "الهند");
        add("e10", "electricians", "7150", "Majad Fouad", "ماجد فؤاد", "", "سوري");
        add("e11", "electricians", "8154", "Birendra Kumar", "برندار كمار", "", "نيبال");
        add("e13", "electricians", "8137", "Aftab Hussain M Ansari", "افتاب حسين م الانصاري", "", "الهند");
        add("e14", "electricians", "9465", "Mosaesta Akabal", "موساييستا أكابال", "", "الهند");

        // ── خياطين / Tailors ──
        add("t1", "tailors", "2030", "Jakir Zafor Ahmed", "جاكير ظفور أحمد", "0552361413", "بنغلادش");
        add("t2", "tailors", "6012", "Mir Wali Shah Qabool Shah", "مير والي شاه قبول شاه", "0555034182", "باكستان");
        add("t6", "tailors", "9317", "Fares Karam Abdelazim", "فارس كرم عبد العظيم", "0556317760", "");
        add("t3", "tailors", "2220", "Nirmol Chandra Nama", "نيرمول شاندرا ناما", "0554972532", "بنغلادش");
        add("t4", "tailors", "8004", "Muhammad Ayaz Asil Zadea", "محمد أياز أصيل زاديا", "0554972415", "باكستان");
        add("t7", "tailors", "9332", "Adel Saber Abdelwahab", "عادل صابر عبد الوهاب", "", "");
        add("t5", "tailors", "2007", "Nuruddin Mukbul Ahammed", "نور الدين مقبل أحمد", "0554972458", "بنغلادش");

        // ── نجارين / Carpenters ──
        add("c1", "carpenters", "2058", "Maniraj Prajapati", "مانيراج براجاباتي", "0555034265", "الهند");
        add("c2", "carpenters", "6014", "Walid Abdelnaby", "وليد عبد النبي", "0526753719", "مصر");
        add("c3", "carpenters", "2140", "Roj Mohammad Miya", "روج محمد ميا", "", "الهند");
        add("c4", "carpenters", "8224", "Rachpal Singh", "راشبال سينغ", "0586839470", "الهند");
        add("c5", "carpenters", "2057", "Om Prakash Chaudhary", "أوم براكاش تشودري", "", "نيبال");
        add("c6", "carpenters", "9283", "Harkesh Sharma", "هركاش", "", "الهند");
        add("c7", "carpenters", "8015", "Abdullah Jan Hussain", "عبدالله جان حسين", "", "باكستان");
        add("c8", "carpenters", "8021", "Danish Khan Saleem Khan", "دانيش خان سليم خان", "", "باكستان");
        add("c10", "carpenters", "8022", "Luqman Ali Muhammad Jamil", "لقمان علي محمد جميل", "", "باكستان");
        add("c11", "carpenters", "8276", "Santosh Kumar Sharma", "سانتوش كومار شارما", "", "الهند");
        add("c12", "carpenters", "8275", "Munna", "منّا", "", "الهند");
        add("c13", "carpenters", "9461", "Dheeraj Kumar", "ديراج كومار", "", "الهند");

        // ── ألمنيوم / Aluminum ──
        add("d1", "aluminum", "2136", "Ravi Kumar Jamna Dass", "رافي كومار جامنا داس", "", "الهند");
        add("d2", "aluminum", "8118", "Pravind Kumar Jaiswal", "برافيند كومار جايسوال", "", "الهند");
        add("d3", "aluminum", "2147", "Mintu Das Sonatan Das", "مينتو داس سوناتان داس", "", "بنغلادش");
        add("d4", "aluminum", "8119", "Zaved Aktar Safique", "زافيد أكتر صافي", "", "الهند");
        add("d5", "aluminum", "2026", "Anamul Hoque Rofik Ahammed", "أنامل الحق رفيق أحمد", "", "بنغلادش");
        add("d6", "aluminum", "8267", "Md Ajaz Alam", "مدّ أجاز عالم", "", "الهند");
        add("d7", "aluminum", "8266", "Dhanlal Yadav", "دهنلال ياداف", "", "الهند");
        add("d8", "aluminum", "8269", "Md Sajid", "مدّ ساجد", "", "الهند");
        add("d9", "aluminum", "8270", "Mohammad Sonu", "محمد سونو", "", "الهند");
    }

    private static void add(String id, String group, String fileNo, String name, String nameAr, String wa, String nat) {
        ALL_TECHS.add(new Technician(id, group, fileNo, name, nameAr, wa, nat));
    }

    /** Dept prefix used for report numbering (E/C/A/T, X fallback). */
    public static String deptPrefix(String deptId) {
        if (deptId == null) return "X";
        switch (deptId) {
            case "electricians": return "E";
            case "carpenters": return "C";
            case "aluminum": return "A";
            case "tailors": return "T";
            default: return "X";
        }
    }

    /** Default daily-report supervisor per department. */
    public static String defaultSupervisor(String deptId) {
        if (deptId == null) return "";
        switch (deptId) {
            case "electricians": return "Rasik Munnurkandy";
            case "tailors": return "Jakir Zafor Ahmed";
            case "carpenters": return "Maniraj Prajapati";
            case "aluminum": return "Ravi Kumar Jamna Dass";
            default: return "";
        }
    }
}

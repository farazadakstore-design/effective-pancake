package com.factory.techmanager.util;

import java.util.Locale;

public class StatusLabels {
    public static String label(String key) {
        if (key == null) return "—";
        switch (key) {
            case "factory": return Lang.t("مصنع", "Factory");
            case "onsite": return Lang.t("موقع", "On Site");
            case "vacation": return Lang.t("إجازة", "Vacation");
            case "sick": return Lang.t("مرض", "Sick");
            case "permsite": return Lang.t("موقع دائم", "Perm Site");
            case "absent": return Lang.t("غياب", "Absent");
            default: return key;
        }
    }

    public static final String[] REPORT_STATUS_KEYS = {"factory", "onsite", "vacation", "sick", "permsite", "absent"};

    public static String monthLabel(String month) {
        if (month == null || !month.contains("-")) return "";
        String[] p = month.split("-");
        int y = Integer.parseInt(p[0]);
        int m = Integer.parseInt(p[1]);
        String[] namesAr = {"يناير","فبراير","مارس","أبريل","مايو","يونيو","يوليو","أغسطس","سبتمبر","أكتوبر","نوفمبر","ديسمبر"};
        String[] namesEn = {"January","February","March","April","May","June","July","August","September","October","November","December"};
        String name = Lang.isArabic() ? namesAr[m - 1] : namesEn[m - 1];
        return String.format(Locale.US, "%s %d", name, y);
    }

    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

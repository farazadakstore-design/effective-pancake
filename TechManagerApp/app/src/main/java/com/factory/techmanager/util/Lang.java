package com.factory.techmanager.util;

/** Mirrors the original app's in-app language switch (independent of device locale). */
public class Lang {
    public static volatile String cur = "ar";

    public static boolean isArabic() {
        return "ar".equals(cur);
    }

    public static String t(String ar, String en) {
        return isArabic() ? ar : en;
    }

    public static void toggle() {
        cur = isArabic() ? "en" : "ar";
    }
}

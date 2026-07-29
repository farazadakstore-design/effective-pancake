package com.tents.maintenance.util;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/** Wraps AndroidX's per-app language API so the whole app can switch AR/EN at runtime,
 *  exactly like the "EN / ع" button in the original web app. The chosen language is
 *  persisted automatically by AppCompat (see AppLocalesMetadataHolderService in the manifest). */
public class LangUtil {

    public static String current() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) return "ar";
        String tag = locales.get(0) != null ? locales.get(0).getLanguage() : "ar";
        return "en".equals(tag) ? "en" : "ar";
    }

    public static boolean isArabic() {
        return "ar".equals(current());
    }

    public static void ensureDefault() {
        if (AppCompatDelegate.getApplicationLocales().isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar"));
        }
    }

    public static void toggle() {
        set(isArabic() ? "en" : "ar");
    }

    public static void set(String lang) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang));
    }
}

package com.tents.maintenance.model;

import com.tents.maintenance.R;

import java.util.Arrays;
import java.util.List;

/** Static definition of the 6 maintenance sections (mirrors SECTIONS in the original web app). */
public class Section {
    public final String id;
    public final int nameRes;
    public final String icon;
    public final String code; // used as serial-number prefix

    private Section(String id, int nameRes, String icon, String code) {
        this.id = id;
        this.nameRes = nameRes;
        this.icon = icon;
        this.code = code;
    }

    public static final List<Section> ALL = Arrays.asList(
            new Section("khayat", R.string.sec_khayat, "🧵", "T"),
            new Section("najjar", R.string.sec_najjar, "\uD83E\uDE9A", "C"),
            new Section("abwab", R.string.sec_abwab, "🚪", "D"),
            new Section("aluminum", R.string.sec_aluminum, "🪟", "A"),
            new Section("kahraba", R.string.sec_kahraba, "⚡", "E"),
            new Section("muno", R.string.sec_muno, "🧰", "M")
    );

    public boolean isElectricalSection() {
        return "kahraba".equals(id);
    }

    public static Section byId(String id) {
        for (Section s : ALL) if (s.id.equals(id)) return s;
        return ALL.get(0);
    }
}

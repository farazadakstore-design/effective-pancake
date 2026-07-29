package com.tents.maintenance.model;

public class Technician {
    public String code;
    public String name;
    public String nameEn;

    public Technician() {}

    public Technician(String code, String name, String nameEn) {
        this.code = code;
        this.name = name;
        this.nameEn = nameEn;
    }

    /** Display name for the given language ("ar"/"en"), falling back to the Arabic name. */
    public String displayName(String lang) {
        if ("en".equals(lang) && nameEn != null && !nameEn.isEmpty()) return nameEn;
        return name;
    }
}

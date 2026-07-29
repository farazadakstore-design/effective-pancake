package com.factory.techmanager.data;

public class Department {
    public final String id;
    public final String ar;
    public final String en;
    public final String icon;
    public final String defaultPw;

    public Department(String id, String ar, String en, String icon, String defaultPw) {
        this.id = id;
        this.ar = ar;
        this.en = en;
        this.icon = icon;
        this.defaultPw = defaultPw;
    }

    public String label(boolean arabic) {
        return arabic ? ar : en;
    }
}

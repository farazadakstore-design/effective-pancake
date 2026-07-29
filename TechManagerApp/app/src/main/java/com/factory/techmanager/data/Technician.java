package com.factory.techmanager.data;

import org.json.JSONException;
import org.json.JSONObject;

public class Technician {
    public String id;
    public String group;
    public String fileNo;
    public String name;
    public String nameAr;
    public String wa;
    public String nat;

    public Technician() {}

    public Technician(String id, String group, String fileNo, String name, String nameAr, String wa, String nat) {
        this.id = id;
        this.group = group;
        this.fileNo = fileNo;
        this.name = name;
        this.nameAr = nameAr;
        this.wa = wa;
        this.nat = nat;
    }

    public String displayName(boolean arabic) {
        String n = arabic ? nameAr : name;
        return n == null || n.isEmpty() ? (arabic ? name : nameAr) : n;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("group", group);
        o.put("fileNo", fileNo);
        o.put("name", name == null ? "" : name);
        o.put("nameAr", nameAr == null ? "" : nameAr);
        o.put("wa", wa == null ? "" : wa);
        o.put("nat", nat == null ? "" : nat);
        return o;
    }

    public static Technician fromJson(JSONObject o) throws JSONException {
        Technician t = new Technician();
        t.id = o.optString("id");
        t.group = o.optString("group");
        t.fileNo = o.optString("fileNo");
        t.name = o.optString("name");
        t.nameAr = o.optString("nameAr");
        t.wa = o.optString("wa");
        t.nat = o.optString("nat");
        return t;
    }
}

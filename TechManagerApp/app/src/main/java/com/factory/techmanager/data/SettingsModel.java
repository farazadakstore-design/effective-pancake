package com.factory.techmanager.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SettingsModel {
    public String adminPw = "123";
    public int breakMinutes = 60;
    public String defaultCheckIn = "06:30";
    public String defaultCheckOut = "17:30";
    public Map<String, String> deptPasswords = new HashMap<>(); // key: deptId

    public String pwFor(String deptId) {
        String v = deptPasswords.get(deptId);
        if (v != null && !v.isEmpty()) return v;
        Department d = Seed.findDept(deptId);
        return d != null ? d.defaultPw : "";
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("adminPw", adminPw);
        o.put("breakMinutes", breakMinutes);
        o.put("defaultCheckIn", defaultCheckIn);
        o.put("defaultCheckOut", defaultCheckOut);
        for (Map.Entry<String, String> e : deptPasswords.entrySet()) {
            o.put("pw_" + e.getKey(), e.getValue());
        }
        return o;
    }

    public static SettingsModel fromJson(JSONObject o) {
        SettingsModel s = new SettingsModel();
        s.adminPw = o.optString("adminPw", "123");
        s.breakMinutes = o.optInt("breakMinutes", 60);
        s.defaultCheckIn = o.optString("defaultCheckIn", "06:30");
        s.defaultCheckOut = o.optString("defaultCheckOut", "17:30");
        for (Department d : Seed.DEPTS) {
            String key = "pw_" + d.id;
            if (o.has(key)) s.deptPasswords.put(d.id, o.optString(key, d.defaultPw));
        }
        return s;
    }

    public static SettingsModel defaults() {
        SettingsModel s = new SettingsModel();
        for (Department d : Seed.DEPTS) s.deptPasswords.put(d.id, d.defaultPw);
        return s;
    }
}

package com.factory.techmanager.data;

import org.json.JSONException;
import org.json.JSONObject;

public class Evaluation {
    public String id; // techId_month
    public String techId;
    public String techName;
    public String techNameAr;
    public String deptId;
    public String fileNo;
    public String month; // yyyy-MM
    public int score;
    public String note;
    public String updatedAt;

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("techId", techId);
        o.put("techName", techName == null ? "" : techName);
        o.put("techNameAr", techNameAr == null ? "" : techNameAr);
        o.put("deptId", deptId);
        o.put("fileNo", fileNo == null ? "" : fileNo);
        o.put("month", month);
        o.put("score", score);
        o.put("note", note == null ? "" : note);
        o.put("updatedAt", updatedAt);
        return o;
    }

    public static Evaluation fromJson(JSONObject o) {
        Evaluation e = new Evaluation();
        e.id = o.optString("id");
        e.techId = o.optString("techId");
        e.techName = o.optString("techName");
        e.techNameAr = o.optString("techNameAr");
        e.deptId = o.optString("deptId");
        e.fileNo = o.optString("fileNo");
        e.month = o.optString("month");
        e.score = o.optInt("score", 0);
        e.note = o.optString("note");
        e.updatedAt = o.optString("updatedAt");
        return e;
    }

    public String displayName(boolean arabic) {
        String n = arabic ? techNameAr : techName;
        if (n == null || n.isEmpty()) n = arabic ? techName : techNameAr;
        return n == null ? "" : n;
    }
}

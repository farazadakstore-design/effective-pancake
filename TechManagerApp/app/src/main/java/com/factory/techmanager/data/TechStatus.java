package com.factory.techmanager.data;

import org.json.JSONException;
import org.json.JSONObject;

/** main: "factory" | "onsite"   temp: null | "vacation" | "sick" | "permsite" | "absent" */
public class TechStatus {
    public String main = "factory";
    public String temp = null;
    public String tempEnd = null; // yyyy-MM-dd or null

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("main", main == null ? "factory" : main);
        o.put("temp", temp == null ? JSONObject.NULL : temp);
        o.put("tempEnd", tempEnd == null ? JSONObject.NULL : tempEnd);
        return o;
    }

    public static TechStatus fromJson(JSONObject o) {
        TechStatus s = new TechStatus();
        s.main = o.optString("main", "factory");
        s.temp = o.isNull("temp") ? null : o.optString("temp", null);
        s.tempEnd = o.isNull("tempEnd") ? null : o.optString("tempEnd", null);
        return s;
    }
}

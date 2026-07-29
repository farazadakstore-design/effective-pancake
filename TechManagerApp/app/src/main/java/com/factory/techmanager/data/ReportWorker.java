package com.factory.techmanager.data;

import org.json.JSONException;
import org.json.JSONObject;

public class ReportWorker {
    public String techId; // nullable - null means manually typed row
    public String name = "";
    public String fileNo = "";
    public String contractNo = "";
    public String status = "factory"; // factory|onsite|vacation|sick|permsite|absent
    public String checkIn = "";
    public String checkOut = "";
    public String hours = "";

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("techId", techId == null ? JSONObject.NULL : techId);
        o.put("name", name);
        o.put("fileNo", fileNo);
        o.put("contractNo", contractNo);
        o.put("status", status);
        o.put("checkIn", checkIn);
        o.put("checkOut", checkOut);
        o.put("hours", hours);
        return o;
    }

    public static ReportWorker fromJson(JSONObject o) {
        ReportWorker w = new ReportWorker();
        w.techId = o.isNull("techId") ? null : o.optString("techId", null);
        w.name = o.optString("name", "");
        w.fileNo = o.optString("fileNo", "");
        w.contractNo = o.optString("contractNo", "");
        w.status = o.optString("status", "factory");
        w.checkIn = o.optString("checkIn", "");
        w.checkOut = o.optString("checkOut", "");
        w.hours = o.optString("hours", "");
        return w;
    }
}

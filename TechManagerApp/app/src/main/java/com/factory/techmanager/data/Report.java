package com.factory.techmanager.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Report {
    public String id;
    public String reportNo;
    public String date;
    public String supervisor;
    public String section;
    public String contractNo;
    public String deptId;
    public List<ReportWorker> workers = new ArrayList<>();
    public List<String> availWorkers = new ArrayList<>();
    public String donePrev;
    public String todo;
    public String materials;
    public String createdAt;

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("reportNo", reportNo);
        o.put("date", date);
        o.put("supervisor", supervisor);
        o.put("section", section);
        o.put("contractNo", contractNo == null ? "" : contractNo);
        o.put("deptId", deptId);
        JSONArray wArr = new JSONArray();
        for (ReportWorker w : workers) wArr.put(w.toJson());
        o.put("workers", wArr);
        JSONArray aArr = new JSONArray();
        for (String s : availWorkers) aArr.put(s);
        o.put("availWorkers", aArr);
        o.put("donePrev", donePrev == null ? "" : donePrev);
        o.put("todo", todo == null ? "" : todo);
        o.put("materials", materials == null ? "" : materials);
        o.put("createdAt", createdAt);
        return o;
    }

    public static Report fromJson(JSONObject o) throws JSONException {
        Report r = new Report();
        r.id = o.optString("id");
        r.reportNo = o.optString("reportNo");
        r.date = o.optString("date");
        r.supervisor = o.optString("supervisor");
        r.section = o.optString("section");
        r.contractNo = o.optString("contractNo");
        r.deptId = o.optString("deptId");
        JSONArray wArr = o.optJSONArray("workers");
        if (wArr != null) {
            for (int i = 0; i < wArr.length(); i++) r.workers.add(ReportWorker.fromJson(wArr.getJSONObject(i)));
        }
        JSONArray aArr = o.optJSONArray("availWorkers");
        if (aArr != null) {
            for (int i = 0; i < aArr.length(); i++) r.availWorkers.add(aArr.getString(i));
        }
        r.donePrev = o.optString("donePrev");
        r.todo = o.optString("todo");
        r.materials = o.optString("materials");
        r.createdAt = o.optString("createdAt");
        return r;
    }
}

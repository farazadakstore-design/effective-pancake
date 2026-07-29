package com.tents.maintenance.model;

public class ReportSummary {
    public String id;
    public String serialNumber;
    public String sectionId;
    public String contractNumber;
    public String technician;
    public String date;
    public long createdAt;

    public ReportSummary(Report r) {
        this.id = r.id;
        this.serialNumber = r.serialNumber;
        this.sectionId = r.sectionId;
        this.contractNumber = r.contractNumber;
        this.technician = r.technician;
        this.date = r.date;
        this.createdAt = r.createdAt;
    }
}

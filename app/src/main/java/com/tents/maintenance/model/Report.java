package com.tents.maintenance.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Report {
    public String id = UUID.randomUUID().toString();
    public String serialNumber;
    public String sectionId;
    public String reportType = "";
    public String contractNumber = "";
    public String tentSize = "";
    public String agent = "";
    public String date = "";
    public Double locationLat;
    public Double locationLng;
    public String locationManual = "";
    public String technician = "";
    public List<Fault> faults = new ArrayList<>();
    public List<AcUnit> acUnits = new ArrayList<>();
    public ElectricalChecks electricalChecks = new ElectricalChecks();
    public String remainingFaults = "";
    public String signaturePath;
    public String customerSignaturePath;
    public long createdAt = System.currentTimeMillis();

    public boolean isElectrical() {
        return "kahraba".equals(sectionId);
    }
}

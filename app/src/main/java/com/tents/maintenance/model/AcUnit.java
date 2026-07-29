package com.tents.maintenance.model;

import java.util.UUID;

public class AcUnit {
    public String id = UUID.randomUUID().toString();
    public String unitNumber = "";
    public String fault = "";
    public String repair = "";
    public String gasPressure = "";
    public String voltage = "";
    public String coolingTemp = "";
    public String beforePhotoPath;
    public String afterPhotoPath;
}

package com.tents.maintenance.model;

import java.util.UUID;

public class Fault {
    public String id = UUID.randomUUID().toString();
    public String description = "";
    public String repair = "";
    public String beforePhotoPath; // absolute file path, or null
    public String afterPhotoPath;  // absolute file path, or null
}

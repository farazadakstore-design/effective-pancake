package com.tents.maintenance;

import android.app.Application;

import com.tents.maintenance.model.Report;
import com.tents.maintenance.model.Section;
import com.tents.maintenance.storage.AppStorage;
import com.tents.maintenance.util.LangUtil;

/**
 * Holds the in-memory report draft while the user moves between the form and
 * success screens (equivalent to the "state.draft" object in the original web
 * app). Only completed reports are persisted (as a summary + exported files);
 * an in-progress draft is intentionally kept in memory only.
 */
public class MaintenanceApp extends Application {

    private AppStorage storage;
    public Section currentSection;
    public Report currentDraft;

    @Override
    public void onCreate() {
        super.onCreate();
        LangUtil.ensureDefault();
        storage = new AppStorage(this);
    }

    public AppStorage storage() {
        return storage;
    }

    public static MaintenanceApp from(android.content.Context ctx) {
        return (MaintenanceApp) ctx.getApplicationContext();
    }
}

package com.factory.techmanager.util;

import com.factory.techmanager.data.Report;
import com.factory.techmanager.data.ReportWorker;
import com.factory.techmanager.data.Session;

import java.util.ArrayList;
import java.util.List;

/** In-memory app state that mirrors the JS globals: session, reportWorkers, availWorkers, savedReport. */
public class AppState {
    public static Session session;
    public static List<ReportWorker> reportWorkers = new ArrayList<>();
    public static List<String> availWorkers = new ArrayList<>();
    public static Report savedReport;
    public static String selectedLoginDept; // transient selection on the login screen

    // Transient report-form field values, kept alive while switching bottom-nav tabs
    public static String reportDate, reportSupervisor, reportSection, reportContractNo,
            reportDonePrev, reportTodo, reportMaterials;

    public static void resetForLogout() {
        session = null;
        reportWorkers = new ArrayList<>();
        availWorkers = new ArrayList<>();
        savedReport = null;
        selectedLoginDept = null;
        reportDate = reportSupervisor = reportSection = reportContractNo = reportDonePrev = reportTodo = reportMaterials = null;
    }

    public static void resetReportState() {
        reportWorkers = new ArrayList<>();
        availWorkers = new ArrayList<>();
        savedReport = null;
    }
}

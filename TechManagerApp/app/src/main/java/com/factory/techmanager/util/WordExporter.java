package com.factory.techmanager.util;

import android.content.Context;

import com.factory.techmanager.data.Department;
import com.factory.techmanager.data.Report;
import com.factory.techmanager.data.ReportWorker;
import com.factory.techmanager.data.Seed;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.factory.techmanager.util.StatusLabels.esc;

/**
 * Exports a Word-openable .doc file using the same MS-Office HTML trick the original
 * web app used (Content-Type application/msword with an HTML body) — Word opens this natively.
 */
public class WordExporter {

    public static File buildReportDoc(Context ctx, Report r) throws IOException {
        Department dept = Seed.findDept(r.deptId);
        String deptName = dept != null ? dept.label(Lang.isArabic()) : r.deptId;

        StringBuilder rowsHtml = new StringBuilder();
        for (ReportWorker w : r.workers) {
            rowsHtml.append("<tr>")
                    .append(td(w.fileNo)).append(td(w.name)).append(td(w.contractNo))
                    .append(td(StatusLabels.label(w.status))).append(td(w.checkIn)).append(td(w.checkOut)).append(td(w.hours))
                    .append("</tr>");
        }
        if (r.workers.isEmpty()) rowsHtml.append("<tr><td colspan=\"7\" style=\"border:1px solid #999;padding:5px;\">-</td></tr>");

        StringBuilder avail = new StringBuilder();
        for (int i = 0; i < r.availWorkers.size(); i++) {
            if (i > 0) avail.append(" · ");
            avail.append(esc(r.availWorkers.get(i)));
        }

        String materialsBlock = (r.materials != null && !r.materials.isEmpty())
                ? "<h3 style=\"color:#c0392b;\">⚠️ " + Lang.t("مواد عاجلة", "Urgent Materials") + "</h3><p>" + nl2br(esc(r.materials)) + "</p>"
                : "";
        String contractRow = (r.contractNo != null && !r.contractNo.isEmpty())
                ? "<tr><th>" + Lang.t("رقم العقد", "Contract No.") + "</th><td colspan=\"3\">" + esc(r.contractNo) + "</td></tr>"
                : "";
        String rno = (r.reportNo != null && !r.reportNo.isEmpty())
                ? "<span class=\"rno\">" + esc(r.reportNo) + "</span>" : "";

        String html = "<html xmlns:o=\"urn:schemas-microsoft-com:office:office\" " +
                "xmlns:w=\"urn:schemas-microsoft-com:office:word\" xmlns=\"http://www.w3.org/TR/REC-html40\">" +
                "<head><meta charset=\"UTF-8\"><style>" +
                "body{font-family:Arial,sans-serif;direction:" + (Lang.isArabic() ? "rtl" : "ltr") + ";margin:2cm;color:#222;}" +
                "h2{color:#154f42;margin-bottom:4px;} h3{color:#1e6f5c;font-size:13px;margin:14px 0 4px;}" +
                "table{width:100%;border-collapse:collapse;margin-bottom:10px;font-size:12px;}" +
                "th{border:1px solid #999;padding:5px 8px;background:#dde;text-align:right;} td{border:1px solid #999;padding:5px 8px;}" +
                ".rno{background:#154f42;color:#fff;padding:2px 10px;border-radius:4px;font-weight:bold;}" +
                "p{border:1px solid #ddd;padding:6px;min-height:20px;font-size:12px;margin:0 0 8px;}" +
                "</style></head><body>" +
                "<div style=\"display:flex;justify-content:space-between;align-items:center;\">" +
                "<h2>" + Lang.t("تقرير نهاية الدوام", "End-of-Shift Report") + "</h2>" + rno + "</div>" +
                "<table><tr><th>" + Lang.t("التاريخ", "Date") + "</th><td>" + esc(r.date) + "</td>" +
                "<th>" + Lang.t("القسم", "Dept") + "</th><td>" + esc(deptName) + "</td></tr>" +
                contractRow +
                "<tr><th>" + Lang.t("الموقع", "Section") + "</th><td>" + esc(r.section) + "</td>" +
                "<th>" + Lang.t("المشرف", "Supervisor") + "</th><td>" + esc(r.supervisor) + "</td></tr></table>" +
                "<h3>👷 " + Lang.t("العمال", "Workers") + "</h3><table><thead><tr>" +
                th(Lang.t("رقم الملف", "File No.")) + th(Lang.t("الاسم", "Name")) + th(Lang.t("رقم العقد", "Contract No.")) +
                th(Lang.t("الحالة", "Status")) + th(Lang.t("دخول", "In")) + th(Lang.t("خروج", "Out")) + th(Lang.t("ساعات", "Hours")) +
                "</tr></thead><tbody>" + rowsHtml + "</tbody></table>" +
                "<h3>👥 " + Lang.t("العمال المتوفرون", "Available Workers") + "</h3><p>" + (avail.length() > 0 ? avail : "-") + "</p>" +
                "<h3>📅 " + Lang.t("أعمال اليوم السابق", "Yesterday Completed") + "</h3><p>" + nl2br(esc(r.donePrev)) + "</p>" +
                "<h3>📌 " + Lang.t("الأعمال المطلوبة", "Today Required Work") + "</h3><p>" + nl2br(esc(r.todo)) + "</p>" +
                materialsBlock +
                "<p style=\"font-size:10px;color:#aaa;border:none;margin-top:20px;\">Generated: " +
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()) + "</p>" +
                "</body></html>";

        String safeName = (r.section != null && !r.section.isEmpty() ? r.section : r.deptId)
                .replaceAll("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]+", "_");
        String fileName = (r.reportNo != null && !r.reportNo.isEmpty() ? r.reportNo : "report") + "_" + safeName + "_" + r.date + ".doc";

        File dir = new File(ctx.getCacheDir(), "exports");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, fileName);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            w.write(html);
        }
        return file;
    }

    private static String td(String v) {
        return "<td style=\"border:1px solid #999;padding:5px;\">" + (v == null || v.isEmpty() ? "-" : esc(v)) + "</td>";
    }

    private static String th(String v) {
        return "<th style=\"border:1px solid #999;padding:5px;\">" + esc(v) + "</th>";
    }

    private static String nl2br(String s) {
        if (s == null || s.isEmpty()) return "-";
        return s.replace("\n", "<br>");
    }
}

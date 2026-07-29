package com.factory.techmanager.util;

import com.factory.techmanager.data.Department;
import com.factory.techmanager.data.Evaluation;
import com.factory.techmanager.data.Report;
import com.factory.techmanager.data.ReportWorker;
import com.factory.techmanager.data.Seed;

import java.util.List;
import java.util.Locale;

/** Plain-text renderings used for the "send as text" / WhatsApp text fallback actions. */
public class TextBuilders {

    public static String buildReportText(Report r) {
        Department dept = Seed.findDept(r.deptId);
        String deptName = dept != null ? dept.label(Lang.isArabic()) : r.deptId;
        StringBuilder sb = new StringBuilder();
        sb.append("📋 ").append(Lang.t("تقرير نهاية الدوام", "End-of-Shift Report"));
        if (r.reportNo != null && !r.reportNo.isEmpty()) sb.append(" ").append(r.reportNo);
        sb.append("\n📅 ").append(Lang.t("التاريخ", "Date")).append(": ").append(r.date);
        sb.append("\n🏭 ").append(Lang.t("القسم", "Dept")).append(": ").append(deptName);
        sb.append("\n📍 ").append(Lang.t("الموقع", "Section")).append(": ").append(orDash(r.section));
        sb.append("\n👤 ").append(Lang.t("المشرف", "Supervisor")).append(": ").append(orDash(r.supervisor));
        sb.append("\n\n👥 ").append(Lang.t("العمال المتوفرون اليوم", "Available Workers Today")).append(":\n");
        sb.append(r.availWorkers.isEmpty() ? "—" : String.join(Lang.isArabic() ? "، " : ", ", r.availWorkers));
        sb.append("\n\n👷 ").append(Lang.t("العمال", "Workers")).append(":\n");
        for (ReportWorker w : r.workers) {
            sb.append(orEmpty(w.fileNo)).append('\t').append(w.name == null || w.name.isEmpty() ? "?" : w.name)
                    .append('\t').append(orEmpty(w.contractNo)).append('\t').append(StatusLabels.label(w.status))
                    .append('\t').append(orEmpty(w.checkIn)).append('\t').append(orEmpty(w.checkOut)).append('\n');
        }
        sb.append("\n📅 ").append(Lang.t("أعمال اليوم السابق", "Yesterday Completed")).append(":\n").append(orDash(r.donePrev));
        sb.append("\n\n📌 ").append(Lang.t("الأعمال المطلوبة اليوم", "Today Required Work")).append(":\n").append(orDash(r.todo));
        if (r.materials != null && !r.materials.isEmpty()) {
            sb.append("\n\n⚠️ ").append(Lang.t("مواد عاجلة", "Urgent Materials")).append(":\n").append(r.materials);
        }
        return sb.toString();
    }

    public static String buildEvalText(String month, List<Evaluation> evals) {
        if (evals == null || evals.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        sb.append("⭐ ").append(Lang.t("تقييم الفنيين الشهري", "Monthly Technician Evaluation"))
                .append(" — ").append(StatusLabels.monthLabel(month)).append("\n\n");
        String currentDept = null;
        double sum = 0;
        for (Evaluation e : evals) {
            if (!e.deptId.equals(currentDept)) {
                currentDept = e.deptId;
                Department d = Seed.findDept(e.deptId);
                sb.append("🏭 ").append(d != null ? d.label(Lang.isArabic()) : e.deptId).append(":\n");
            }
            sb.append("  - ");
            if (e.fileNo != null && !e.fileNo.isEmpty()) sb.append("[#").append(e.fileNo).append("] ");
            sb.append(e.displayName(Lang.isArabic())).append(": ").append(e.score).append("/10");
            if (e.note != null && !e.note.isEmpty()) sb.append(" — ").append(e.note);
            sb.append("\n");
            sum += e.score;
        }
        double avg = sum / evals.size();
        sb.append("\n⭐ ").append(Lang.t("المتوسط العام", "Overall Average")).append(": ")
                .append(String.format(Locale.US, "%.1f", avg)).append("/10 (")
                .append(evals.size()).append(" ").append(Lang.t("فني", "technicians")).append(")");
        return sb.toString();
    }

    private static String orDash(String s) { return s == null || s.isEmpty() ? "—" : s; }
    private static String orEmpty(String s) { return s == null ? "" : s; }
}

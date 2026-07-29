package com.factory.techmanager.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;

import com.factory.techmanager.data.Department;
import com.factory.techmanager.data.Evaluation;
import com.factory.techmanager.data.Report;
import com.factory.techmanager.data.ReportWorker;
import com.factory.techmanager.data.Seed;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Builds A4 PDF documents natively with android.graphics.pdf.PdfDocument,
 * replacing the original app's html2canvas + jsPDF pipeline.
 */
public class PdfBuilder {

    private static final int PAGE_W = 595;  // A4 @ 72dpi
    private static final int PAGE_H = 842;
    private static final int MARGIN = 36;
    private static final int RIGHT = PAGE_W - MARGIN; // text starts from the right (RTL)

    private final PdfDocument document = new PdfDocument();
    private PdfDocument.Page page;
    private Canvas canvas;
    private float y;
    private int pageNum = 0;

    private void newPage() {
        if (page != null) document.finishPage(page);
        pageNum++;
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create();
        page = document.startPage(info);
        canvas = page.getCanvas();
        y = MARGIN;
    }

    private void ensureSpace(float needed) {
        if (canvas == null) { newPage(); return; }
        if (y + needed > PAGE_H - MARGIN) newPage();
    }

    private TextPaint paint(float size, boolean bold, int color) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(size);
        p.setFakeBoldText(bold);
        p.setColor(color);
        return p;
    }

    private void drawTitle(String text, String badge) {
        ensureSpace(30);
        TextPaint p = paint(16, true, Color.parseColor("#154F42"));
        canvas.drawText(text, RIGHT, y + 16, alignRight(p));
        if (badge != null && !badge.isEmpty()) {
            TextPaint bp = paint(11, true, Color.WHITE);
            float w = bp.measureText(badge) + 16;
            Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
            bg.setColor(Color.parseColor("#154F42"));
            canvas.drawRoundRect(MARGIN, y - 2, MARGIN + w, y + 18, 5, 5, bg);
            canvas.drawText(badge, MARGIN + 8, y + 13, bp);
        }
        y += 26;
    }

    private Paint alignRight(TextPaint p) {
        p.setTextAlign(Paint.Align.RIGHT);
        return p;
    }

    private void drawKV(String label1, String value1, String label2, String value2) {
        ensureSpace(20);
        TextPaint lp = paint(9.5f, true, Color.parseColor("#154F42"));
        TextPaint vp = paint(10, false, Color.BLACK);
        float half = (PAGE_W - 2 * MARGIN) / 2f;
        canvas.drawText(label1, RIGHT, y + 12, alignRight(lp));
        canvas.drawText(value1 == null || value1.isEmpty() ? "-" : value1, RIGHT - 4, y + 26, alignRight(vp));
        if (label2 != null) {
            canvas.drawText(label2, RIGHT - half, y + 12, alignRight(lp));
            canvas.drawText(value2 == null || value2.isEmpty() ? "-" : value2, RIGHT - half - 4, y + 26, alignRight(vp));
        }
        y += 34;
        canvas.drawLine(MARGIN, y - 6, RIGHT, y - 6, thinLine());
    }

    private Paint thinLine() {
        Paint p = new Paint();
        p.setColor(Color.parseColor("#DDDDDD"));
        p.setStrokeWidth(0.7f);
        return p;
    }

    private void drawSectionHeader(String text) {
        ensureSpace(22);
        y += 6;
        TextPaint p = paint(11.5f, true, Color.parseColor("#1E6F5C"));
        canvas.drawText(text, RIGHT, y + 12, alignRight(p));
        y += 20;
    }

    /** Simple table with column headers/rows, columns rendered right-to-left (RTL). Widths sum should equal content width. */
    private void drawTable(String[] headers, List<String[]> rows, float[] weights) {
        float contentW = PAGE_W - 2 * MARGIN;
        float[] widths = new float[weights.length];
        float sum = 0; for (float w : weights) sum += w;
        for (int i = 0; i < weights.length; i++) widths[i] = contentW * (weights[i] / sum);

        ensureSpace(20);
        // header
        Paint hbg = new Paint(); hbg.setColor(Color.parseColor("#EEEEFF"));
        canvas.drawRect(MARGIN, y, RIGHT, y + 18, hbg);
        TextPaint hp = paint(8.5f, true, Color.parseColor("#154F42"));
        float x = RIGHT;
        for (int i = 0; i < headers.length; i++) {
            canvas.drawText(headers[i], x - 3, y + 13, alignRight(hp));
            x -= widths[i];
        }
        y += 18;
        canvas.drawLine(MARGIN, y, RIGHT, y, thinLine());

        TextPaint cp = paint(8.5f, false, Color.BLACK);
        for (String[] row : rows) {
            ensureSpace(18);
            float rx = RIGHT;
            for (int i = 0; i < row.length && i < widths.length; i++) {
                String val = row[i] == null || row[i].isEmpty() ? "-" : row[i];
                canvas.drawText(truncate(cp, val, widths[i] - 6), rx - 3, y + 13, alignRight(cp));
                rx -= widths[i];
            }
            y += 18;
            canvas.drawLine(MARGIN, y, RIGHT, y, thinLine());
        }
        y += 8;
    }

    private String truncate(Paint p, String text, float maxWidth) {
        if (p.measureText(text) <= maxWidth) return text;
        String t = text;
        while (t.length() > 1 && p.measureText(t + "…") > maxWidth) t = t.substring(0, t.length() - 1);
        return t + "…";
    }

    private void drawParagraph(String label, String value, boolean danger) {
        drawSectionHeader(label);
        TextPaint p = paint(9.5f, false, danger ? Color.parseColor("#C0392B") : Color.BLACK);
        String text = (value == null || value.isEmpty()) ? "-" : value;
        StaticLayout layout = buildLayout(text, p, (int) (PAGE_W - 2 * MARGIN - 12));
        ensureSpace(layout.getHeight() + 16);
        Paint border = new Paint();
        border.setColor(Color.parseColor(danger ? "#FFAAAA" : "#DDDDDD"));
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(1f);
        canvas.drawRect(MARGIN, y, RIGHT, y + layout.getHeight() + 12, border);
        canvas.save();
        canvas.translate(RIGHT - 6 - layout.getWidth(), y + 6);
        layout.draw(canvas);
        canvas.restore();
        y += layout.getHeight() + 18;
    }

    private StaticLayout buildLayout(String text, TextPaint paint, int width) {
        Layout.Alignment align = Lang.isArabic() ? Layout.Alignment.ALIGN_NORMAL : Layout.Alignment.ALIGN_NORMAL;
        StaticLayout.Builder b = StaticLayout.Builder.obtain(text, 0, text.length(), paint, Math.max(width, 50))
                .setAlignment(align)
                .setLineSpacing(2f, 1f)
                .setIncludePad(false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            b.setTextDirection(Lang.isArabic() ? TextDirectionHeuristics.RTL : TextDirectionHeuristics.LTR);
        }
        return b.build();
    }

    private void drawFooter() {
        ensureSpace(20);
        TextPaint p = paint(7.5f, false, Color.parseColor("#999999"));
        String gen = Lang.t("تم الإنشاء", "Generated") + ": " +
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
        canvas.drawLine(MARGIN, y, RIGHT, y, thinLine());
        canvas.drawText(gen, RIGHT, y + 12, alignRight(p));
        y += 20;
    }

    private File finish(Context ctx, String fileName) throws IOException {
        if (page != null) document.finishPage(page);
        File dir = new File(ctx.getCacheDir(), "exports");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            document.writeTo(fos);
        } finally {
            document.close();
        }
        return file;
    }

    // ───────────────────────── PUBLIC BUILDERS ─────────────────────────

    public static File buildReportPdf(Context ctx, Report r) throws IOException {
        PdfBuilder b = new PdfBuilder();
        b.newPage();
        Department dept = Seed.findDept(r.deptId);
        String deptName = dept != null ? dept.label(Lang.isArabic()) : r.deptId;

        b.drawTitle(Lang.t("تقرير نهاية الدوام", "End-of-Shift Report"), r.reportNo);
        b.drawKV(Lang.t("التاريخ", "Date"), r.date, Lang.t("القسم", "Dept"), deptName);
        if (r.contractNo != null && !r.contractNo.isEmpty()) {
            b.drawKV(Lang.t("رقم العقد", "Contract No."), r.contractNo, null, null);
        }
        b.drawKV(Lang.t("الموقع", "Section"), r.section, Lang.t("المشرف", "Supervisor"), r.supervisor);

        b.drawSectionHeader("👷 " + Lang.t("العمال", "Workers"));
        String[] headers = {
                Lang.t("رقم الملف", "File No."), Lang.t("الاسم", "Name"), Lang.t("رقم العقد", "Contract No."),
                Lang.t("الحالة", "Status"), Lang.t("دخول", "In"), Lang.t("خروج", "Out"), Lang.t("ساعات", "Hours")
        };
        List<String[]> rows = new java.util.ArrayList<>();
        for (ReportWorker w : r.workers) {
            rows.add(new String[]{w.fileNo, w.name, w.contractNo, StatusLabels.label(w.status), w.checkIn, w.checkOut, w.hours});
        }
        b.drawTable(headers, rows, new float[]{1.6f, 3f, 1.8f, 1.8f, 1.3f, 1.3f, 1.2f});

        StringBuilder avail = new StringBuilder();
        for (int i = 0; i < r.availWorkers.size(); i++) {
            if (i > 0) avail.append(" · ");
            avail.append(r.availWorkers.get(i));
        }
        b.drawParagraph("👥 " + Lang.t("العمال المتوفرون اليوم", "Available Workers Today"), avail.toString(), false);
        b.drawParagraph("📅 " + Lang.t("أعمال اليوم السابق", "Yesterday Completed"), r.donePrev, false);
        b.drawParagraph("📌 " + Lang.t("الأعمال المطلوبة اليوم", "Today Required Work"), r.todo, false);
        if (r.materials != null && !r.materials.isEmpty()) {
            b.drawParagraph("⚠️ " + Lang.t("مواد عاجلة", "Urgent Materials"), r.materials, true);
        }
        b.drawFooter();

        String safeName = safe(r.section != null && !r.section.isEmpty() ? r.section : r.deptId);
        String fileName = "report_" + safeName + "_" + safe(r.date) + ".pdf";
        return b.finish(ctx, fileName);
    }

    public static File buildEvalPdf(Context ctx, String month, List<Evaluation> evals) throws IOException {
        PdfBuilder b = new PdfBuilder();
        b.newPage();
        b.drawTitle("⭐ " + Lang.t("تقييم الفنيين الشهري", "Monthly Technician Evaluation"), null);
        TextPaint mp = b.paint(10, false, Color.DKGRAY);
        b.canvas.drawText(Lang.t("الشهر", "Month") + ": " + StatusLabels.monthLabel(month), RIGHT, b.y + 10, b.alignRight(mp));
        b.y += 24;

        String[] headers = {
                Lang.t("رقم الملف", "File No."), Lang.t("الاسم", "Name"), Lang.t("القسم", "Dept"),
                Lang.t("التقييم", "Score"), Lang.t("ملاحظة", "Note")
        };
        List<String[]> rows = new java.util.ArrayList<>();
        double sum = 0;
        for (Evaluation e : evals) {
            Department d = Seed.findDept(e.deptId);
            String dname = d != null ? d.label(Lang.isArabic()) : e.deptId;
            rows.add(new String[]{e.fileNo, e.displayName(Lang.isArabic()), dname, e.score + "/10", e.note});
            sum += e.score;
        }
        b.drawTable(headers, rows, new float[]{1.3f, 2.6f, 1.6f, 1f, 2.5f});

        double avg = evals.isEmpty() ? 0 : sum / evals.size();
        TextPaint ap = b.paint(11, true, Color.BLACK);
        b.ensureSpace(20);
        b.canvas.drawText("⭐ " + Lang.t("المتوسط العام", "Overall Average") + ": " +
                String.format(Locale.US, "%.1f", avg) + "/10", RIGHT, b.y + 12, b.alignRight(ap));
        b.y += 22;
        b.drawFooter();

        String fileName = "evaluation_" + safe(month) + ".pdf";
        return b.finish(ctx, fileName);
    }

    private static String safe(String s) {
        if (s == null) return "report";
        return s.replaceAll("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]+", "_");
    }
}

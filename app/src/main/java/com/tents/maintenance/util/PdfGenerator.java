package com.tents.maintenance.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.tents.maintenance.R;
import com.tents.maintenance.model.AcUnit;
import com.tents.maintenance.model.Fault;
import com.tents.maintenance.model.Report;
import com.tents.maintenance.model.Section;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Builds a branded, paginated A4 PDF for a completed report, replaying the same
 * sections the original web app rendered via html2canvas/jsPDF: header, info grid,
 * faults/AC units with before/after photos, notes and signatures.
 */
public class PdfGenerator {

    private static final float PAGE_W = 595f;
    private static final float PAGE_H = 842f;
    private static final float MARGIN = 32f;
    private static final float CONTENT_W = PAGE_W - MARGIN * 2;

    private static final int BRAND = Color.parseColor("#0F766E");
    private static final int BRAND_DARK = Color.parseColor("#0B5B55");
    private static final int BORDER = Color.parseColor("#E5E7EB");
    private static final int MUTED = Color.parseColor("#6B7280");
    private static final int TEXT_MAIN = Color.parseColor("#111827");
    private static final int NOTES_BG = Color.parseColor("#FFFBEB");
    private static final int NOTES_BORDER = Color.parseColor("#FDE68A");
    private static final int CARD_BG = Color.parseColor("#FAFAFA");
    private static final int INFO_BG = Color.parseColor("#F8FAFC");

    private final Context ctx;
    private final Report r;
    private final Section section;
    private final String lang;
    private PdfDocument doc;
    private PdfDocument.Page page;
    private Canvas canvas;
    private float y;
    private int pageNum;

    private PdfGenerator(Context ctx, Report r, Section section, String lang) {
        this.ctx = ctx;
        this.r = r;
        this.section = section;
        this.lang = lang;
    }

    public static File generate(Context ctx, Report r, Section section, String lang) throws IOException {
        PdfGenerator g = new PdfGenerator(ctx, r, section, lang);
        return g.build();
    }

    private String s(int resId) {
        return ctx.getString(resId);
    }

    private File build() throws IOException {
        doc = new PdfDocument();
        pageNum = 0;
        startPage();

        drawHeader();
        drawInfoGrid();

        if (section.isElectricalSection()) {
            drawSectionTitle(s(R.string.electrical_checklist_title));
            drawChecklist();
            drawSectionTitle(s(R.string.ac_title));
            int i = 1;
            for (AcUnit u : r.acUnits) drawAcUnit(u, i++);
        } else {
            drawSectionTitle(s(R.string.faults_title));
            int i = 1;
            for (Fault f : r.faults) drawFault(f, i++);
        }

        if (r.remainingFaults != null && !r.remainingFaults.trim().isEmpty()) {
            drawSectionTitle(s(R.string.remaining_label));
            drawNotesBox(r.remainingFaults);
        }

        drawSignatures();
        finishPage();

        File dir = new File(ctx.getFilesDir(), "pdf");
        if (!dir.exists()) dir.mkdirs();
        String name = "report_" + (r.contractNumber != null && !r.contractNumber.isEmpty() ? sanitize(r.contractNumber) : r.id) + ".pdf";
        File out = new File(dir, name);
        try (FileOutputStream fos = new FileOutputStream(out)) {
            doc.writeTo(fos);
        }
        doc.close();
        return out;
    }

    private String sanitize(String s) {
        return s.replaceAll("[^A-Za-z0-9_\\-\\u0600-\\u06FF]", "_");
    }

    // ---------------- pagination ----------------

    private void startPage() {
        pageNum++;
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder((int) PAGE_W, (int) PAGE_H, pageNum).create();
        page = doc.startPage(info);
        canvas = page.getCanvas();
        Paint bg = new Paint();
        bg.setColor(Color.WHITE);
        canvas.drawRect(0, 0, PAGE_W, PAGE_H, bg);
        y = MARGIN;
        drawFooter();
        if (pageNum > 1) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(BRAND);
            p.setTextSize(11);
            p.setFakeBoldText(true);
            canvas.drawText(s(R.string.pdf_title), MARGIN, y + 10, p);
            y += 24;
        }
    }

    private void finishPage() {
        doc.finishPage(page);
    }

    private void ensureSpace(float needed) {
        if (y + needed > PAGE_H - MARGIN - 14) {
            finishPage();
            startPage();
        }
    }

    private void drawFooter() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.parseColor("#9CA3AF"));
        p.setTextSize(8);
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(s(R.string.app_name) + "  •  " + pageNum, PAGE_W / 2f, PAGE_H - 14, p);
    }

    // ---------------- header ----------------

    private void drawHeader() {
        float headerH = 78;
        RectF band = new RectF(MARGIN, y, PAGE_W - MARGIN, y + headerH);
        Paint bandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bandPaint.setColor(BRAND);
        canvas.drawRoundRect(band, 12, 12, bandPaint);

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.WHITE);
        titlePaint.setTextSize(18);
        titlePaint.setFakeBoldText(true);
        canvas.drawText(s(R.string.pdf_title), MARGIN + 16, y + 30, titlePaint);

        Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subPaint.setColor(Color.parseColor("#E6FFFB"));
        subPaint.setTextSize(12);
        canvas.drawText(ctx.getString(section.nameRes), MARGIN + 16, y + 50, subPaint);

        Paint idPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        idPaint.setColor(Color.parseColor("#DFF5F1"));
        idPaint.setTextSize(9);
        idPaint.setTextAlign(Paint.Align.RIGHT);
        String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(r.createdAt));
        canvas.drawText(s(R.string.report_serial_label) + ": " + safe(r.serialNumber), PAGE_W - MARGIN - 16, y + 24, idPaint);
        canvas.drawText(stamp, PAGE_W - MARGIN - 16, y + 40, idPaint);

        y += headerH + 16;
    }

    // ---------------- info grid ----------------

    private void drawInfoGrid() {
        String[][] cells = new String[][]{
                {s(R.string.contract_no), safe(r.contractNumber)},
                {s(R.string.tent_size), safe(r.tentSize)},
                {s(R.string.agent), safe(r.agent)},
                {s(R.string.date), safe(r.date)},
                {s(R.string.tech_name), safe(r.technician)},
                {s(R.string.location), locationText()},
        };
        float colW = (CONTENT_W - 10) / 2f;
        float rowH = 40f;
        ensureSpace(rowH * 3 + 20);
        for (int i = 0; i < cells.length; i++) {
            int row = i / 2, col = i % 2;
            float x = MARGIN + col * (colW + 10);
            float top = y + row * (rowH + 8);
            RectF box = new RectF(x, top, x + colW, top + rowH);
            Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
            bg.setColor(INFO_BG);
            canvas.drawRoundRect(box, 8, 8, bg);
            Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
            border.setStyle(Paint.Style.STROKE);
            border.setColor(BORDER);
            canvas.drawRoundRect(box, 8, 8, border);

            Paint lbl = new Paint(Paint.ANTI_ALIAS_FLAG);
            lbl.setColor(MUTED);
            lbl.setTextSize(8);
            lbl.setFakeBoldText(true);
            canvas.drawText(cells[i][0], x + 10, top + 15, lbl);

            Paint val = new Paint(Paint.ANTI_ALIAS_FLAG);
            val.setColor(TEXT_MAIN);
            val.setTextSize(12);
            val.setFakeBoldText(true);
            canvas.drawText(truncate(cells[i][1], val, colW - 20), x + 10, top + 31, val);
        }
        y += rowH * 3 + 26;
    }

    private String locationText() {
        if (r.locationLat != null && r.locationLng != null) {
            return String.format(Locale.US, "%.5f, %.5f", r.locationLat, r.locationLng);
        }
        if (r.locationManual != null && !r.locationManual.isEmpty()) return r.locationManual;
        return "-";
    }

    private String safe(String v) {
        return v == null || v.trim().isEmpty() ? "-" : v;
    }

    private String truncate(String text, Paint p, float maxWidth) {
        if (p.measureText(text) <= maxWidth) return text;
        String ellipsis = "…";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (p.measureText(sb.toString() + c + ellipsis) > maxWidth) break;
            sb.append(c);
        }
        return sb + ellipsis;
    }

    // ---------------- section helpers ----------------

    private void drawSectionTitle(String text) {
        ensureSpace(30);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(BRAND);
        p.setTextSize(14);
        p.setFakeBoldText(true);
        canvas.drawText(text, MARGIN, y + 14, p);
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(BRAND);
        line.setStrokeWidth(2f);
        canvas.drawLine(MARGIN, y + 20, PAGE_W - MARGIN, y + 20, line);
        y += 30;
    }

    private void drawChecklist() {
        String[] items = new String[]{
                s(R.string.check_wiring) + (r.electricalChecks.wiring ? "  ✔" : "  ✘"),
                s(R.string.check_db) + (r.electricalChecks.db ? "  ✔" : "  ✘"),
                s(R.string.check_lighting) + (r.electricalChecks.lighting ? "  ✔" : "  ✘"),
        };
        ensureSpace(70);
        RectF box = new RectF(MARGIN, y, PAGE_W - MARGIN, y + 60);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(CARD_BG);
        canvas.drawRoundRect(box, 10, 10, bg);
        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setColor(BORDER);
        canvas.drawRoundRect(box, 10, 10, border);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(TEXT_MAIN);
        tp.setTextSize(11);
        float ty = y + 18;
        for (String item : items) {
            canvas.drawText(item, MARGIN + 12, ty, tp);
            ty += 18;
        }
        y += 70;
    }

    private void drawFault(Fault f, int index) {
        float cardTop = y;
        float estimatedHeight = 90 + (hasPhoto(f.beforePhotoPath) || hasPhoto(f.afterPhotoPath) ? 150 : 0);
        ensureSpace(Math.min(estimatedHeight, PAGE_H - MARGIN * 2 - 40));
        cardTop = y;
        float innerY = cardTop + 14;
        float startY = innerY;

        Paint header = new Paint(Paint.ANTI_ALIAS_FLAG);
        header.setColor(TEXT_MAIN);
        header.setTextSize(12);
        header.setFakeBoldText(true);
        canvas.drawText(s(R.string.fault_label) + " " + index, MARGIN + 14, innerY, header);
        innerY += 16;

        innerY = drawLabeledParagraph(s(R.string.fault_desc_hint), f.description, innerY);
        innerY = drawLabeledParagraph(s(R.string.repair_label), f.repair, innerY);
        innerY = drawPhotosRow(f.beforePhotoPath, f.afterPhotoPath, innerY);

        float cardHeight = innerY - cardTop + 10;
        drawCardBorder(cardTop, cardHeight);
        y = cardTop + cardHeight + 12;
    }

    private void drawAcUnit(AcUnit u, int index) {
        float cardTop = y;
        ensureSpace(160);
        cardTop = y;
        float innerY = cardTop + 14;

        Paint header = new Paint(Paint.ANTI_ALIAS_FLAG);
        header.setColor(TEXT_MAIN);
        header.setTextSize(12);
        header.setFakeBoldText(true);
        canvas.drawText(s(R.string.ac_unit_label) + " " + index + (u.unitNumber != null && !u.unitNumber.isEmpty() ? " - " + u.unitNumber : ""), MARGIN + 14, innerY, header);
        innerY += 16;

        innerY = drawLabeledParagraph(s(R.string.fault_label), u.fault, innerY);
        innerY = drawLabeledParagraph(s(R.string.repair_label), u.repair, innerY);
        innerY = drawLabeledLine(s(R.string.gas_pressure), safe(u.gasPressure), innerY);
        innerY = drawLabeledLine(s(R.string.voltage), safe(u.voltage), innerY);
        innerY = drawLabeledLine(s(R.string.cooling_temp), safe(u.coolingTemp), innerY);
        innerY = drawPhotosRow(u.beforePhotoPath, u.afterPhotoPath, innerY);

        float cardHeight = innerY - cardTop + 10;
        drawCardBorder(cardTop, cardHeight);
        y = cardTop + cardHeight + 12;
    }

    private void drawCardBorder(float top, float height) {
        RectF box = new RectF(MARGIN, top, PAGE_W - MARGIN, top + height);
        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setColor(BORDER);
        canvas.drawRoundRect(box, 10, 10, border);
    }

    private boolean hasPhoto(String path) {
        return path != null && new File(path).exists();
    }

    private float drawLabeledLine(String label, String value, float startY) {
        Paint lbl = new Paint(Paint.ANTI_ALIAS_FLAG);
        lbl.setColor(BRAND);
        lbl.setTextSize(9);
        lbl.setFakeBoldText(true);
        canvas.drawText(label, MARGIN + 14, startY, lbl);
        Paint val = new Paint(Paint.ANTI_ALIAS_FLAG);
        val.setColor(TEXT_MAIN);
        val.setTextSize(11);
        canvas.drawText(value, MARGIN + 14, startY + 13, val);
        return startY + 24;
    }

    private float drawLabeledParagraph(String label, String value, float startY) {
        Paint lbl = new Paint(Paint.ANTI_ALIAS_FLAG);
        lbl.setColor(BRAND);
        lbl.setTextSize(9);
        lbl.setFakeBoldText(true);
        canvas.drawText(label, MARGIN + 14, startY, lbl);
        startY += 4;

        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(TEXT_MAIN);
        tp.setTextSize(11);
        String text = (value == null || value.trim().isEmpty()) ? "-" : value;
        float width = CONTENT_W - 28;
        StaticLayout layout = StaticLayout.Builder.obtain(text, 0, text.length(), tp, (int) width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1f)
                .build();
        canvas.save();
        canvas.translate(MARGIN + 14, startY + 10);
        layout.draw(canvas);
        canvas.restore();
        return startY + layout.getHeight() + 12;
    }

    private float drawPhotosRow(String beforePath, String afterPath, float startY) {
        boolean before = hasPhoto(beforePath);
        boolean after = hasPhoto(afterPath);
        if (!before && !after) return startY;

        float boxW = (CONTENT_W - 28 - 10) / 2f;
        float boxH = 130f;
        float x = MARGIN + 14;

        if (before) drawPhotoBox(beforePath, x, startY, boxW, boxH, s(R.string.photo_before));
        if (after) drawPhotoBox(afterPath, x + boxW + 10, startY, boxW, boxH, s(R.string.photo_after));

        return startY + boxH + 18;
    }

    private void drawPhotoBox(String path, float x, float top, float w, float h, String caption) {
        Bitmap bmp = decodeScaled(path, (int) w * 2);
        RectF box = new RectF(x, top, x + w, top + h);
        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setColor(BORDER);
        if (bmp != null) {
            canvas.save();
            android.graphics.Path clip = new android.graphics.Path();
            clip.addRoundRect(box, 8, 8, android.graphics.Path.Direction.CW);
            canvas.clipPath(clip);
            RectF dst = fitRect(bmp.getWidth(), bmp.getHeight(), box);
            canvas.drawBitmap(bmp, null, dst, null);
            canvas.restore();
            bmp.recycle();
        } else {
            Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
            bg.setColor(CARD_BG);
            canvas.drawRoundRect(box, 8, 8, bg);
        }
        canvas.drawRoundRect(box, 8, 8, border);

        Paint cap = new Paint(Paint.ANTI_ALIAS_FLAG);
        cap.setColor(MUTED);
        cap.setTextSize(9);
        cap.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(caption, x + w / 2f, top + h + 12, cap);
    }

    /** Scales+centers a bitmap rect to fill the destination box while preserving aspect (center-crop). */
    private RectF fitRect(int bw, int bh, RectF box) {
        float boxRatio = box.width() / box.height();
        float bmpRatio = (float) bw / bh;
        float w, h;
        if (bmpRatio > boxRatio) {
            h = box.height();
            w = h * bmpRatio;
        } else {
            w = box.width();
            h = w / bmpRatio;
        }
        float cx = box.centerX(), cy = box.centerY();
        return new RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
    }

    private Bitmap decodeScaled(String path, int reqWidth) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            int inSample = 1;
            while ((bounds.outWidth / inSample) > reqWidth * 2) inSample *= 2;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = inSample;
            return BitmapFactory.decodeFile(path, opts);
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------- notes & signatures ----------------

    private void drawNotesBox(String text) {
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(TEXT_MAIN);
        tp.setTextSize(11);
        float width = CONTENT_W - 28;
        StaticLayout layout = StaticLayout.Builder.obtain(text, 0, text.length(), tp, (int) width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(3f, 1f)
                .build();
        float boxHeight = layout.getHeight() + 28;
        ensureSpace(boxHeight + 10);
        RectF box = new RectF(MARGIN, y, PAGE_W - MARGIN, y + boxHeight);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(NOTES_BG);
        canvas.drawRoundRect(box, 10, 10, bg);
        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setColor(NOTES_BORDER);
        canvas.drawRoundRect(box, 10, 10, border);
        canvas.save();
        canvas.translate(MARGIN + 14, y + 16);
        layout.draw(canvas);
        canvas.restore();
        y += boxHeight + 16;
    }

    private void drawSignatures() {
        ensureSpace(110);
        float top = y + 10;
        Paint divider = new Paint(Paint.ANTI_ALIAS_FLAG);
        divider.setColor(BORDER);
        divider.setStrokeWidth(1f);
        canvas.drawLine(MARGIN, top, PAGE_W - MARGIN, top, divider);

        float colW = CONTENT_W / 3f;
        drawSignatureBox(r.signaturePath, MARGIN, top + 10, colW, s(R.string.signature));
        drawSignatureBox(r.customerSignaturePath, MARGIN + colW, top + 10, colW, s(R.string.signature_customer));

        Paint nameP = new Paint(Paint.ANTI_ALIAS_FLAG);
        nameP.setColor(TEXT_MAIN);
        nameP.setTextSize(11);
        nameP.setFakeBoldText(true);
        nameP.setTextAlign(Paint.Align.CENTER);
        float x3 = MARGIN + colW * 2 + colW / 2f;
        canvas.drawText(safe(r.technician), x3, top + 60, nameP);
        Paint dateP = new Paint(Paint.ANTI_ALIAS_FLAG);
        dateP.setColor(MUTED);
        dateP.setTextSize(10);
        dateP.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(safe(r.date), x3, top + 76, dateP);

        y = top + 90;
    }

    private void drawSignatureBox(String path, float x, float top, float w, String label) {
        Bitmap bmp = hasPhoto(path) ? decodeScaled(path, (int) w * 2) : null;
        if (bmp != null) {
            RectF box = new RectF(x + 6, top, x + w - 6, top + 50);
            RectF dst = fitRect(bmp.getWidth(), bmp.getHeight(), box);
            canvas.drawBitmap(bmp, null, dst, null);
            bmp.recycle();
        }
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(Color.parseColor("#9CA3AF"));
        canvas.drawLine(x + 6, top + 56, x + w - 12, top + 56, line);
        Paint lbl = new Paint(Paint.ANTI_ALIAS_FLAG);
        lbl.setColor(MUTED);
        lbl.setTextSize(9);
        canvas.drawText(label, x + 6, top + 68, lbl);
    }
}

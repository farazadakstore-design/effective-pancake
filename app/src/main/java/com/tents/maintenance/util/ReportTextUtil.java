package com.tents.maintenance.util;

import android.content.Context;

import com.tents.maintenance.R;
import com.tents.maintenance.model.AcUnit;
import com.tents.maintenance.model.Fault;
import com.tents.maintenance.model.Report;
import com.tents.maintenance.model.Section;

import java.util.Locale;

/** Plain-text summary of a report, used for the WhatsApp text share and generic share sheet
 *  (mirrors reportSummaryText() in the original web app). */
public class ReportTextUtil {

    public static String summary(Context ctx, Report r, Section section) {
        StringBuilder sb = new StringBuilder();
        sb.append(ctx.getString(R.string.report_for)).append(' ').append(ctx.getString(section.nameRes)).append('\n');
        sb.append(ctx.getString(R.string.report_serial_label)).append(": ").append(nz(r.serialNumber)).append('\n');
        sb.append(ctx.getString(R.string.contract_no)).append(": ").append(nz(r.contractNumber)).append('\n');
        sb.append(ctx.getString(R.string.tent_size)).append(": ").append(nz(r.tentSize)).append('\n');
        sb.append(ctx.getString(R.string.agent)).append(": ").append(nz(r.agent)).append('\n');
        sb.append(ctx.getString(R.string.date)).append(": ").append(nz(r.date)).append('\n');
        sb.append(ctx.getString(R.string.tech_name)).append(": ").append(nz(r.technician)).append('\n');

        if (r.locationLat != null && r.locationLng != null) {
            sb.append(ctx.getString(R.string.location)).append(": https://www.google.com/maps?q=")
                    .append(String.format(Locale.US, "%.5f,%.5f", r.locationLat, r.locationLng)).append('\n');
        } else if (r.locationManual != null && !r.locationManual.isEmpty()) {
            sb.append(ctx.getString(R.string.location)).append(": ").append(r.locationManual).append('\n');
        }

        if (section.isElectricalSection()) {
            sb.append('\n').append(ctx.getString(R.string.electrical_checklist_title)).append(":\n");
            sb.append("- ").append(ctx.getString(R.string.check_wiring)).append(": ").append(r.electricalChecks.wiring ? "✔" : "—").append('\n');
            sb.append("- ").append(ctx.getString(R.string.check_db)).append(": ").append(r.electricalChecks.db ? "✔" : "—").append('\n');
            sb.append("- ").append(ctx.getString(R.string.check_lighting)).append(": ").append(r.electricalChecks.lighting ? "✔" : "—").append('\n');
            int i = 1;
            for (AcUnit u : r.acUnits) {
                sb.append('\n').append(ctx.getString(R.string.ac_unit_label)).append(' ').append(i++)
                        .append(" (").append(nz(u.unitNumber)).append("):\n");
                sb.append(ctx.getString(R.string.fault_label)).append(": ").append(nz(u.fault)).append('\n');
                sb.append(ctx.getString(R.string.repair_label)).append(": ").append(nz(u.repair)).append('\n');
                sb.append(ctx.getString(R.string.gas_pressure)).append(": ").append(nz(u.gasPressure)).append('\n');
                sb.append(ctx.getString(R.string.voltage)).append(": ").append(nz(u.voltage)).append('\n');
                sb.append(ctx.getString(R.string.cooling_temp)).append(": ").append(nz(u.coolingTemp)).append('\n');
            }
        } else {
            int i = 1;
            for (Fault f : r.faults) {
                sb.append('\n').append(ctx.getString(R.string.fault_label)).append(' ').append(i++).append(":\n");
                sb.append(ctx.getString(R.string.fault_desc_hint)).append(": ").append(nz(f.description)).append('\n');
                sb.append(ctx.getString(R.string.repair_label)).append(": ").append(nz(f.repair)).append('\n');
            }
        }

        if (r.remainingFaults != null && !r.remainingFaults.isEmpty()) {
            sb.append('\n').append(ctx.getString(R.string.remaining_label)).append(": ").append(r.remainingFaults).append('\n');
        }
        return sb.toString();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}

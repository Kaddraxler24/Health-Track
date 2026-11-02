package io.github.kaddraxler24.healthk.wear;

import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService;
import androidx.wear.watchface.complications.datasource.ComplicationRequest;

/** Complication du cadran : jours avant la prochaine injection (J-3, « auj. », ✓). */
public class InjComplication extends ComplicationDataSourceService {

    @Override
    public void onComplicationRequest(ComplicationRequest request, ComplicationRequestListener listener) {
        int n = Store.injDays(this);
        String v = Store.injDoneToday(this) ? "✓" : n == Integer.MIN_VALUE ? "—" : n > 0 ? "J-" + n : n == 0 ? "auj." : "+" + (-n) + " j";
        String line = Store.injLine(this);
        ComplicationData d = BasdaiComplication.data(this, v, "Inj.", line != null ? line : "Prochaine injection", MainActivity.class);
        try { listener.onComplicationData(d); } catch (Exception ignored) {}
    }

    @Override
    public ComplicationData getPreviewData(ComplicationType type) {
        return type == ComplicationType.SHORT_TEXT ? BasdaiComplication.data(this, "J-3", "Inj.", "Prochaine injection", MainActivity.class) : null;
    }
}

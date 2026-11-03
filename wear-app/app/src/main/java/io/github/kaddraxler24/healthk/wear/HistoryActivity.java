package io.github.kaddraxler24.healthk.wear;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import org.json.JSONObject;

/** Historique : courbe des 14 derniers BASDAI et liste jour par jour. */
public class HistoryActivity extends HkActivity {

    @Override
    protected void onResume() {
        super.onResume();
        page(true);
        clock();
        text("Historique", 16, WHITE, true).setPadding(0, dp(2), 0, 0);
        List<double[]> ser = Store.series(this);
        List<String> days = Store.seriesDays(this);
        if (ser.isEmpty()) { space(16); text("Aucune évaluation pour l'instant.", 12, T2, false); return; }
        JSONObject avg = Store.hist(this).optJSONObject("avg");
        LinearLayout c = card(null);
        label(c, ser.size() + " dernières évaluations", T2);
        double[] vals = new double[ser.size()];
        double sum = 0;
        for (int i = 0; i < vals.length; i++) { vals[i] = ser.get(i)[1]; sum += vals[i]; }
        Widgets.Bars bars = new Widgets.Bars(this);
        bars.set(vals);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, dp(56)); bl.topMargin = dp(8);
        c.addView(bars, bl);
        LinearLayout r = statRow(c);
        double mean = Math.round(sum / vals.length * 10) / 10.0;
        double min = 10, max = 0; for (double v : vals) { min = Math.min(min, v); max = Math.max(max, v); }
        stat(r, Store.fr1(mean), "Moyenne", Store.color(mean));
        stat(r, Store.fr1(min), "Mini", Store.color(min));
        stat(r, Store.fr1(max), "Maxi", Store.color(max));
        if (avg != null && vals.length >= 4) {
            double first = 0, second = 0; int h = vals.length / 2;
            for (int i = 0; i < h; i++) first += vals[i];
            for (int i = vals.length - h; i < vals.length; i++) second += vals[i];
            double d = Math.round((second - first) / h * 10) / 10.0;
            text(c, d <= -0.3 ? "↘ En baisse de " + Store.fr1(-d) : d >= 0.3 ? "↗ En hausse de " + Store.fr1(d) : "→ Stable", 11, d >= 0.3 ? CORAL : d <= -0.3 ? GREEN : T2, true).setPadding(0, dp(8), 0, 0);
        }
        LinearLayout list = card(null);
        label(list, "Jour par jour", T2);
        for (int i = ser.size() - 1; i >= 0; i--) {
            double b = ser.get(i)[1];
            String day = i < days.size() ? days.get(i) : "";
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(7), 0, dp(7));
            TextView d = text(row, Store.dayLabel(day) + (Store.isLocal(this, day) ? "  ⌚" : ""), 12, WHITE, false);
            d.setGravity(Gravity.START);
            d.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
            TextView v = new TextView(this);
            v.setText(Store.fr1(b));
            v.setTextSize(13);
            v.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
            v.setTextColor(0xFF000000);
            v.setGravity(Gravity.CENTER);
            v.setPadding(dp(8), dp(2), dp(8), dp(2));
            v.setBackground(shape(Store.color(b), 10));
            row.addView(v, new LinearLayout.LayoutParams(dp(46), -2));
            list.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        text("⌚ = noté sur la montre, en attente de synchro", 9, T2, false).setPadding(0, dp(8), 0, 0);
    }
}

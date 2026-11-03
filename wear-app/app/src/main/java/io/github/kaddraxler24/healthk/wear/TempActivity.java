package io.github.kaddraxler24.healthk.wear;

import android.os.Bundle;
import android.widget.TextView;

/** Noter une température (0,1 °C par cran de lunette). */
public class TempActivity extends HkActivity {

    private int t = 370;   // dixièmes de degré
    private Widgets.Ring value;
    private TextView warn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        page(false);
        text("Température", 12, CORAL, true);
        String last = Store.prefs(this).getString("lastTemp", null);
        org.json.JSONObject ht = Store.hist(this).optJSONObject("temp");
        if (last == null && ht != null) last = Store.fr1(ht.optDouble("v")) + " °C";
        if (last != null) text("Dernière : " + last, 9, T2, false);
        value = dial(v -> change(-1), v -> change(1), 82);
        warn = text("", 10, T2, false);
        warn.setPadding(0, dp(2), 0, 0);
        show();
        button("Enregistrer", TEAL, WHITE, v -> save());
    }

    private void change(int d) { t = Math.max(350, Math.min(420, t + d)); show(); }

    @Override
    protected void onRotary(int s) { if (value != null) change(s); }

    private void show() {
        double v = t / 10.0;
        int c = v >= 38 ? CORAL : v >= 37.5 ? ORANGE : BLUE;
        value.valueSp = 27;
        value.set((float) ((v - 35) / 7), c, Store.fr1(v) + "°", "");
        warn.setText(v >= 38 ? "Fièvre : pas d'injection sans avis médical" : v >= 37.5 ? "Légère élévation" : "Normale");
        warn.setTextColor(v >= 38 ? CORAL : v >= 37.5 ? ORANGE : T2);
    }

    private void save() {
        double v = t / 10.0;
        try { Store.saveTemp(this, v); } catch (Exception ignored) {}
        value = null;
        page(false);
        text("Enregistré ✓", 11, MINT, true);
        text(Store.fr1(v) + " °C", 34, v >= 38 ? CORAL : WHITE, true).setPadding(0, dp(4), 0, dp(4));
        if (v >= 38) text("Fièvre : pas d'injection IMRALDI sans avis médical.", 10, CORAL, false);
        button("Fermer", GREY, WHITE, x -> finish());
    }
}

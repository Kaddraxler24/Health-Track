package io.github.kaddraxler24.healthk.wear;

import android.os.Bundle;
import android.widget.TextView;

/** Noter une température (0,1 °C par cran de lunette). */
public class TempActivity extends HkActivity {

    private int t = 370;   // dixièmes de degré
    private TextView value, warn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        page(false);
        text("Température", 13, MINT, true);
        String last = Store.prefs(this).getString("lastTemp", null);
        if (last != null) text("Dernière : " + last, 10, T2, false);
        value = stepper(v -> change(-1), v -> change(1));
        warn = text("", 11, T2, false);
        show();
        button("Enregistrer", TEAL, WHITE, v -> save());
    }

    private void change(int d) { t = Math.max(350, Math.min(420, t + d)); show(); }

    @Override
    protected void onRotary(int s) { if (value != null) change(s); }

    private void show() {
        double v = t / 10.0;
        value.setText(Store.fr1(v) + "°");
        value.setTextColor(v >= 38 ? 0xFFF2685E : v >= 37.5 ? 0xFFF0A030 : WHITE);
        warn.setText(v >= 38 ? "Fièvre : pas d'injection sans avis médical" : v >= 37.5 ? "Légère élévation" : "");
    }

    private void save() {
        double v = t / 10.0;
        try { Store.saveTemp(this, v); } catch (Exception ignored) {}
        value = null;
        page(false);
        text("Enregistré ✓", 12, MINT, true);
        text(Store.fr1(v) + " °C", 34, v >= 38 ? 0xFFF2685E : WHITE, true);
        if (v >= 38) text("Fièvre : pas d'injection IMRALDI sans avis médical.", 11, 0xFFF2685E, false);
        button("Fermer", GREY, WHITE, x -> finish());
    }
}

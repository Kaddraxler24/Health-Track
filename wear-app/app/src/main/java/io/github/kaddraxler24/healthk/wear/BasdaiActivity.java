package io.github.kaddraxler24.healthk.wear;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;

/** Évaluation BASDAI complète, une question par écran. Une seule évaluation par jour. */
public class BasdaiActivity extends HkActivity {

    static final String[] TITLE = { "", "Fatigue", "Douleur cou, dos, hanches", "Articulations (douleur, gonflement)",
        "Points sensibles (talons, bassin…)", "Raideur du matin : intensité", "Raideur du matin : durée",
        "Douleur abdominale", "Reflux gastrique" };
    static final String[] HINT = { "", "0 = aucune · 10 = extrême", "0 = aucune · 10 = extrême", "0 = aucune · 10 = extrême",
        "0 = aucune · 10 = extrême", "0 = aucune · 10 = extrême", "en minutes", "facultatif · 0 à 10", "facultatif · 0 à 10" };
    static final int STEPS = 8;

    private final int[] q = new int[6];
    private int q6m = 0, step = 1, abd = 0, reflux = 0;
    private boolean abdSet = false, refluxSet = false;
    private Widgets.Ring value;
    private String day;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        day = Store.basdaiDay();
        JSONObject last = Store.lastAnswers(this);
        for (int i = 1; i <= 5; i++) q[i] = last != null ? Math.max(0, Math.min(10, last.optInt("q" + i, 5))) : 5;
        int m = Store.morningMin(this, Store.today());
        q6m = m >= 0 ? m : 0;
        JSONObject hl = Store.hist(this).optJSONObject("last");
        if (hl != null) { abd = Math.max(0, Math.min(10, hl.optInt("abd", 0))); }
        if (b != null) { step = b.getInt("step", 1); abd = b.getInt("abd", abd); reflux = b.getInt("reflux", 0); abdSet = b.getBoolean("abdSet"); refluxSet = b.getBoolean("refluxSet"); int[] s = b.getIntArray("q"); if (s != null) System.arraycopy(s, 0, q, 0, 6); q6m = b.getInt("q6m", q6m); }
        double done = Store.doneScore(this, day);
        if (done >= 0) result(done, false); else question();
    }

    @Override
    protected void onSaveInstanceState(Bundle o) { super.onSaveInstanceState(o); o.putInt("step", step); o.putIntArray("q", q); o.putInt("q6m", q6m); o.putInt("abd", abd); o.putInt("reflux", reflux); o.putBoolean("abdSet", abdSet); o.putBoolean("refluxSet", refluxSet); }

    private void question() {
        page(false);
        dots(STEPS, step);
        text(step <= 6 ? "Q" + step : step == 7 ? "Ventre" : "Estomac", 11, MINT, true);
        TextView t = text(TITLE[step], 14, WHITE, true);
        t.setMaxLines(2);
        text(HINT[step], 9, T2, false).setPadding(0, dp(2), 0, 0);
        value = dial(v -> change(-1), v -> change(1), 78);
        show();
        if (step == 6) {
            int m = Store.morningMin(this, Store.today());
            if (m >= 0) text("Mesuré ce matin : " + Store.minutes(m), 9, T2, false);
        }
        if (step <= 6) {
            button(step < 6 ? "Suivant" : "Continuer", TEAL, WHITE, v -> next());
            if (step == 1 && Store.lastAnswers(this) != null) button("Comme la dernière fois", GREY, WHITE, v -> { step = 6; question(); });
        } else {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            col.addView(row, new LinearLayout.LayoutParams(-1, -2));
            TextView skip = button(row, "Passer", GREY, WHITE, v -> { if (step == 7) abdSet = false; else refluxSet = false; advance(); });
            TextView ok = button(row, step == 7 ? "Suivant" : "Terminer", TEAL, WHITE, v -> { if (step == 7) abdSet = true; else refluxSet = true; advance(); });
            LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, -2, 1); a.topMargin = dp(6); a.rightMargin = dp(3);
            LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, -2, 1); b.topMargin = dp(6); b.leftMargin = dp(3);
            skip.setLayoutParams(a); ok.setLayoutParams(b);
        }
    }

    private void change(int d) {
        if (step <= 5) q[step] = Math.max(0, Math.min(10, q[step] + d));
        else if (step == 6) q6m = Math.max(0, Math.min(180, q6m + d * 5));
        else if (step == 7) { abd = Math.max(0, Math.min(10, abd + d)); abdSet = true; }
        else { reflux = Math.max(0, Math.min(10, reflux + d)); refluxSet = true; }
        show();
    }

    private void show() {
        if (value == null) return;
        if (step == 6) { value.valueSp = q6m >= 100 ? 21 : 25; value.set(Math.min(1f, q6m / 120f), ORANGE, Store.minutes(q6m), ""); return; }
        int v = step <= 5 ? q[step] : step == 7 ? abd : reflux;
        value.valueSp = 38;
        value.set(v / 10f, v < 4 ? GREEN : v < 7 ? ORANGE : CORAL, String.valueOf(v), "sur 10");
    }

    @Override
    protected void onRotary(int s) { if (value != null) change(s); }

    private void next() { step++; question(); }

    private void advance() {
        if (step < STEPS) { step++; question(); return; }
        double b = Store.basdai(q, q6m);
        try { Store.saveBasdai(this, day, q, q6m, b, abdSet ? abd : -1, refluxSet ? reflux : -1); } catch (Exception ignored) {}
        result(b, true);
    }

    private void result(double b, boolean fresh) {
        value = null;
        page(false);
        text(fresh ? "Enregistré ✓" : "Déjà fait aujourd'hui", 11, MINT, true);
        Widgets.Ring r = new Widgets.Ring(this, 8);
        r.valueSp = 32;
        r.set((float) (b / 10), Store.color(b), Store.fr1(b), "BASDAI");
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(dp(100), dp(100));
        rl.gravity = android.view.Gravity.CENTER_HORIZONTAL; rl.topMargin = dp(4); rl.bottomMargin = dp(4);
        col.addView(r, rl);
        text(Store.level(b), 12, WHITE, true);
        if (fresh) text(Store.waiting(this) > 0 ? "Envoi au téléphone dès qu'il est joignable" : "Ajouté à ton historique", 9, T2, false).setPadding(0, dp(3), 0, 0);
        else text("Une évaluation par jour · la prochaine demain", 9, T2, false).setPadding(0, dp(3), 0, 0);
        button("Fermer", GREY, WHITE, v -> finish());
    }
}

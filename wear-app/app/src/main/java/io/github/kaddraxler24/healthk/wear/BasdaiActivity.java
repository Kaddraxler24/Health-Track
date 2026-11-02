package io.github.kaddraxler24.healthk.wear;

import android.os.Bundle;
import android.widget.TextView;
import org.json.JSONObject;

/** Évaluation BASDAI complète, une question par écran. Une seule évaluation par jour. */
public class BasdaiActivity extends HkActivity {

    static final String[] TITLE = { "", "Fatigue", "Douleur cou, dos, hanches", "Articulations (douleur, gonflement)",
        "Points sensibles (talons, bassin…)", "Raideur du matin : intensité", "Raideur du matin : durée" };
    static final String[] HINT = { "", "0 = aucune · 10 = extrême", "0 = aucune · 10 = extrême", "0 = aucune · 10 = extrême",
        "0 = aucune · 10 = extrême", "0 = aucune · 10 = extrême", "en minutes" };

    private final int[] q = new int[6];
    private int q6m = 0, step = 1;
    private TextView value;
    private String day;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        day = Store.basdaiDay();
        JSONObject last = Store.lastAnswers(this);
        for (int i = 1; i <= 5; i++) q[i] = last != null ? Math.max(0, Math.min(10, last.optInt("q" + i, 5))) : 5;
        int m = Store.morningMin(this, Store.today());
        q6m = m >= 0 ? m : 0;
        if (b != null) { step = b.getInt("step", 1); int[] s = b.getIntArray("q"); if (s != null) System.arraycopy(s, 0, q, 0, 6); q6m = b.getInt("q6m", q6m); }
        double done = Store.doneScore(this, day);
        if (done >= 0) result(done, false); else question();
    }

    @Override
    protected void onSaveInstanceState(Bundle o) { super.onSaveInstanceState(o); o.putInt("step", step); o.putIntArray("q", q); o.putInt("q6m", q6m); }

    private void question() {
        page(false);
        text("Q" + step + " · " + step + "/6", 12, MINT, true);
        text(TITLE[step], 14, WHITE, true);
        text(HINT[step], 10, T2, false);
        value = stepper(v -> change(-1), v -> change(1));
        show();
        if (step == 6) {
            int m = Store.morningMin(this, Store.today());
            if (m >= 0) text("Mesuré ce matin : " + Store.minutes(m), 10, T2, false);
        }
        button(step < 6 ? "Suivant" : "Terminer", TEAL, WHITE, v -> next());
        if (step == 1 && Store.lastAnswers(this) != null)
            button("Comme la dernière fois", GREY, WHITE, v -> { step = 6; question(); });
        else if (step > 1)
            button("Retour", 0xFF000000, T2, v -> { step--; question(); });
    }

    private void change(int d) {
        if (step <= 5) q[step] = Math.max(0, Math.min(10, q[step] + d));
        else q6m = Math.max(0, Math.min(180, q6m + d * 5));
        show();
    }

    private void show() { value.setText(step <= 5 ? String.valueOf(q[step]) : Store.minutes(q6m)); if (step == 6) value.setTextSize(30); }

    @Override
    protected void onRotary(int s) { if (value != null) change(s); }

    private void next() {
        if (step < 6) { step++; question(); return; }
        double b = Store.basdai(q, q6m);
        try { Store.saveBasdai(this, day, q, q6m, b); } catch (Exception ignored) {}
        result(b, true);
    }

    private void result(double b, boolean fresh) {
        value = null;
        page(false);
        text(fresh ? "Enregistré ✓" : "Déjà fait aujourd'hui", 12, MINT, true);
        text("BASDAI", 13, T2, false);
        text(Store.fr1(b), 40, Store.color(b), true);
        text(Store.level(b), 12, WHITE, false);
        if (fresh) text(Store.waiting(this) > 0 ? "Envoyé au téléphone dès qu'il est joignable" : "Envoyé au téléphone", 10, T2, false);
        else text("Une évaluation par jour : la prochaine demain.", 10, T2, false);
        button("Fermer", GREY, WHITE, v -> finish());
    }
}

package io.github.kaddraxler24.healthk.wear;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import org.json.JSONObject;

/** Raideur du matin : chrono (garde l'heure de départ même si l'appli est fermée) ou durée choisie. */
public class ChronoActivity extends HkActivity {

    private final Handler h = new Handler(Looper.getMainLooper());
    private TextView elapsed;
    private int pick = 30;
    private TextView pickView;
    private final Runnable tick = new Runnable() { public void run() { showElapsed(); h.postDelayed(this, 10000); } };

    @Override
    protected void onResume() { super.onResume(); render(); }

    @Override
    protected void onPause() { super.onPause(); h.removeCallbacks(tick); }

    private long start() {
        JSONObject c = Store.obj(Store.prefs(this).getString("chrono", null));
        return c.optString("date").equals(Store.today()) ? c.optLong("start", 0) : 0;
    }

    private void render() {
        h.removeCallbacks(tick);
        elapsed = null; pickView = null;
        page(false);
        long st = start();
        if (st > 0) {
            text("Raideur du matin", 12, MINT, true);
            text("depuis " + Store.hhmm(st), 11, T2, false);
            elapsed = text("", 40, 0xFFF0A030, true);
            button("Raideur partie", TEAL, WHITE, v -> save((int) Math.max(0, Math.round((System.currentTimeMillis() - st) / 60000.0))));
            button("Annuler le chrono", 0xFF000000, T2, v -> { Store.prefs(this).edit().remove("chrono").apply(); Reminders.chronoNotif(this, 0); render(); });
            h.post(tick);
            return;
        }
        int m = Store.morningMin(this, Store.today());
        text(m >= 0 ? "Ce matin : " + Store.minutes(m) : "Raideur du matin", 13, MINT, true);
        button("Démarrer le chrono", TEAL, WHITE, v -> {
            long now = System.currentTimeMillis();
            try { JSONObject c = new JSONObject(); c.put("date", Store.today()); c.put("start", now); Store.prefs(this).edit().putString("chrono", c.toString()).apply(); } catch (Exception ignored) {}
            Reminders.chronoNotif(this, now);
            render();
        });
        text("ou durée directe", 10, T2, false);
        pick = m >= 0 ? m : 30;
        pickView = stepper(v -> change(-1), v -> change(1));
        pickView.setTextSize(28);
        pickView.setText(Store.minutes(pick));
        button("Enregistrer", GREY, WHITE, v -> save(pick));
    }

    private void change(int d) { pick = Math.max(0, Math.min(180, pick + d * 5)); if (pickView != null) pickView.setText(Store.minutes(pick)); }

    @Override
    protected void onRotary(int s) { if (pickView != null) change(s); }

    private void showElapsed() {
        long st = start();
        if (elapsed != null && st > 0) elapsed.setText(Store.minutes((int) ((System.currentTimeMillis() - st) / 60000)));
    }

    private void save(int min) {
        try { Store.saveMorning(this, Store.today(), min); } catch (Exception ignored) {}
        Reminders.chronoNotif(this, 0);
        page(false);
        text("Enregistré ✓", 12, MINT, true);
        text(Store.minutes(min), 36, WHITE, true);
        text("Reprise ce soir dans la Q6 du BASDAI.", 11, T2, false);
        button("Fermer", GREY, WHITE, v -> finish());
    }
}

package io.github.kaddraxler24.healthk.wear;

import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import org.json.JSONObject;

/** Raideur du matin : chrono (garde l'heure de départ même si l'appli est fermée) ou durée choisie. */
public class ChronoActivity extends HkActivity {

    private final Handler h = new Handler(Looper.getMainLooper());
    private Widgets.Ring elapsed, pickRing;
    private int pick = 30;
    private final Runnable tick = new Runnable() { public void run() { showElapsed(); h.postDelayed(this, 5000); } };

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
        elapsed = null; pickRing = null;
        page(false);
        long st = start();
        if (st > 0) {
            text("Raideur du matin", 11, ORANGE, true);
            text("depuis " + Store.hhmm(st), 10, T2, false);
            elapsed = new Widgets.Ring(this, 7);
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(dp(96), dp(96));
            rl.gravity = Gravity.CENTER_HORIZONTAL; rl.topMargin = dp(4);
            col.addView(elapsed, rl);
            showElapsed();
            button("Raideur partie", TEAL, WHITE, v -> save((int) Math.max(0, Math.round((System.currentTimeMillis() - st) / 60000.0))));
            button("Annuler", 0xFF000000, T2, v -> { Store.prefs(this).edit().remove("chrono").apply(); Reminders.chronoNotif(this, 0); render(); });
            h.post(tick);
            return;
        }
        int m = Store.morningMin(this, Store.today());
        text(m >= 0 ? "Ce matin : " + Store.minutes(m) : "Raideur du matin", 12, ORANGE, true);
        button("▶  Démarrer le chrono", TEAL, WHITE, v -> {
            long now = System.currentTimeMillis();
            try { JSONObject c = new JSONObject(); c.put("date", Store.today()); c.put("start", now); Store.prefs(this).edit().putString("chrono", c.toString()).apply(); } catch (Exception ignored) {}
            Reminders.chronoNotif(this, now);
            render();
        });
        text("ou durée directe", 9, T2, false).setPadding(0, dp(6), 0, 0);
        pick = m >= 0 ? m : 30;
        pickRing = dial(v -> change(-1), v -> change(1), 64);
        showPick();
        button("Enregistrer", GREY, WHITE, v -> save(pick));
    }

    private void change(int d) { pick = Math.max(0, Math.min(180, pick + d * 5)); showPick(); }
    private void showPick() { if (pickRing == null) return; pickRing.valueSp = 24; pickRing.set(Math.min(1f, pick / 120f), ORANGE, Store.minutes(pick), ""); }

    @Override
    protected void onRotary(int s) { if (pickRing != null) change(s); }

    private void showElapsed() {
        long st = start();
        if (elapsed == null || st <= 0) return;
        int min = (int) ((System.currentTimeMillis() - st) / 60000);
        elapsed.valueSp = 28;
        elapsed.set(((System.currentTimeMillis() - st) % 3600000) / 3600000f, ORANGE, Store.minutes(min), "minutes");
    }

    private void save(int min) {
        try { Store.saveMorning(this, Store.today(), min); } catch (Exception ignored) {}
        Reminders.chronoNotif(this, 0);
        elapsed = null; pickRing = null;
        page(false);
        text("Enregistré ✓", 11, MINT, true);
        text(Store.minutes(min), 34, WHITE, true).setPadding(0, dp(4), 0, dp(4));
        text("Reprise ce soir dans la Q6 du BASDAI.", 10, T2, false);
        button("Fermer", GREY, WHITE, v -> finish());
    }
}

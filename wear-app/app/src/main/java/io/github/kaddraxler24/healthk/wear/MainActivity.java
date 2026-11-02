package io.github.kaddraxler24.healthk.wear;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

/** Menu Health K' : BASDAI du jour, injection, et accès rapide aux saisies. */
public class MainActivity extends HkActivity {

    private boolean injConfirm = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, 1);
        Store.hello(this);
        Store.prune(this);
        Reminders.schedule(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        injConfirm = false;
        render();
    }

    private void render() {
        page(true);
        text("Health K'", 13, MINT, true);
        space(4);
        String day = Store.basdaiDay();
        double s = Store.doneScore(this, day);
        if (s >= 0) {
            text("BASDAI du jour", 12, T2, false);
            text(Store.fr1(s), 34, Store.color(s), true);
        } else {
            text("BASDAI du jour", 12, T2, false);
            text("à faire", 22, WHITE, true);
        }
        String inj = Store.injLine(this);
        if (inj != null) text(inj, 13, 0xFFB7A8FF, false);
        space(6);
        button(s >= 0 ? "Voir mon BASDAI" : "Faire le BASDAI", s >= 0 ? GREY : TEAL, WHITE, v -> startActivity(new Intent(this, BasdaiActivity.class)));
        int m = Store.morningMin(this, Store.today());
        boolean running = Store.prefs(this).getString("chrono", null) != null;
        button(running ? "Chrono raideur en cours" : m >= 0 ? "Raideur : " + Store.minutes(m) : "Raideur du matin", running ? 0xFF8A5A10 : GREY, WHITE,
            v -> startActivity(new Intent(this, ChronoActivity.class)));
        button("Température", GREY, WHITE, v -> startActivity(new Intent(this, TempActivity.class)));
        int days = Store.injDays(this);
        if (days != Integer.MIN_VALUE && days <= 0 && !Store.injDoneToday(this)) {
            TextView ib = button("Injection faite", 0xFF5B4BB7, WHITE, null);
            ib.setOnClickListener(v -> {
                if (!injConfirm) { injConfirm = true; ib.setText("Confirmer l'injection ?"); return; }
                try { Store.saveInj(this); } catch (Exception ignored) {}
                render();
            });
        }
        space(8);
        int w = Store.waiting(this);
        long at = Store.stateTime(this);
        String sync = w > 0 ? w + " envoi" + (w > 1 ? "s" : "") + " en attente du téléphone"
            : at == 0 ? "Ouvre Health K' sur le téléphone pour relier la montre"
            : "Synchronisé à " + Store.hhmm(at);
        text(sync, 11, T2, false);
    }
}

package io.github.kaddraxler24.healthk.wear;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Accueil Health K' : BASDAI du jour, actions rapides, 10 derniers jours, dernière saisie, IMRALDI, RDV. */
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

    private void open(Class<?> c) { startActivity(new Intent(this, c)); }

    private void render() {
        page(true);
        clock();
        TextView brand = text("Health K'", 11, MINT, true);
        brand.setLetterSpacing(0.04f);
        JSONObject h = Store.hist(this);

        // ── Aujourd'hui ──
        String day = Store.basdaiDay();
        double s = Store.doneScore(this, day);
        Widgets.Ring ring = new Widgets.Ring(this, 7);
        ring.valueSp = 30;
        if (s >= 0) ring.set((float) (s / 10), Store.color(s), Store.fr1(s), "BASDAI");
        else ring.set(0, MINT, "—", "BASDAI");
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(dp(96), dp(96));
        rl.gravity = Gravity.CENTER_HORIZONTAL; rl.topMargin = dp(6);
        col.addView(ring, rl);
        ring.setOnClickListener(v -> open(BasdaiActivity.class));
        if (s >= 0) text(Store.level(s), 11, WHITE, false).setPadding(0, dp(4), 0, 0);
        else button("Faire le BASDAI", TEAL, WHITE, v -> open(BasdaiActivity.class));

        // ── Actions rapides ──
        LinearLayout acts = statRow(col);
        int m = Store.morningMin(this, Store.today());
        boolean running = Store.prefs(this).getString("chrono", null) != null;
        action(acts, R.drawable.ic_timer, ORANGE, running ? "En cours" : m >= 0 ? Store.minutes(m) : "Raideur", v -> open(ChronoActivity.class));
        action(acts, R.drawable.ic_thermo, CORAL, "Temp.", v -> open(TempActivity.class));
        action(acts, R.drawable.ic_chart, MINT, "Historique", v -> open(HistoryActivity.class));

        // ── 10 derniers jours ──
        JSONObject avg = h.optJSONObject("avg");
        LinearLayout c1 = card(v -> open(HistoryActivity.class));
        label(c1, "10 derniers jours", T2);
        if (avg != null && avg.has("basdai") && !avg.isNull("basdai")) {
            double ab = avg.optDouble("basdai");
            TextView big = text(c1, Store.fr1(ab), 30, Store.color(ab), true);
            big.setPadding(0, dp(4), 0, 0);
            text(c1, "BASDAI moyen · " + (ab >= 4 ? "au-dessus du seuil" : "sous le seuil (4)"), 10, T2, false);
        }
        List<double[]> ser = Store.series(this);
        if (!ser.isEmpty()) {
            double[] vals = new double[ser.size()];
            for (int i = 0; i < vals.length; i++) vals[i] = ser.get(i)[1];
            Widgets.Bars bars = new Widgets.Bars(this);
            bars.set(vals);
            LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, dp(38));
            bl.topMargin = dp(8);
            c1.addView(bars, bl);
        }
        if (avg != null) {
            LinearLayout r1 = statRow(c1);
            stat(r1, Store.num1(avg, "fatigue"), "Fatigue", CORAL);
            stat(r1, avg.has("raideur") && !avg.isNull("raideur") ? avg.optInt("raideur") + " min" : "—", "Raideur", ORANGE);
            stat(r1, String.valueOf(avg.optInt("days", 0)), "Jours", GREEN);
        } else if (ser.isEmpty()) text(c1, "Ouvre Health K' sur le téléphone pour voir ton historique.", 11, T2, false);

        // ── Dernière saisie ──
        JSONObject last = h.optJSONObject("last");
        if (last != null) {
            LinearLayout c2 = card(v -> open(HistoryActivity.class));
            label(c2, "Dernière saisie", T2);
            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            Widgets.Ring mini = new Widgets.Ring(this, 4);
            double lb = last.optDouble("basdai");
            mini.valueSp = 34;
            mini.set((float) (lb / 10), Store.color(lb), Store.fr1(lb), "");
            top.addView(mini, new LinearLayout.LayoutParams(dp(46), dp(46)));
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(dp(8), 0, 0, 0);
            TextView d = text(info, Store.dayLabel(last.optString("date")), 13, WHITE, true); d.setGravity(Gravity.START);
            TextView cx = text(info, last.optString("ctx", "RAS"), 10, T2, false); cx.setGravity(Gravity.START); cx.setMaxLines(2);
            top.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-1, -2); tl.topMargin = dp(8);
            c2.addView(top, tl);
            LinearLayout r2 = statRow(c2);
            stat(r2, last.optInt("q1") + "/10", "Fatigue", WHITE);
            stat(r2, last.optInt("q2") + "/10", "Dos", WHITE);
            LinearLayout r3 = statRow(c2);
            stat(r3, last.optInt("q6m") + " min", "Raideur", WHITE);
            stat(r3, last.has("abd") && !last.isNull("abd") ? last.optInt("abd") + "/10" : "—", "Abdomen", WHITE);
        }

        // ── IMRALDI ──
        JSONObject inj = h.optJSONObject("inj");
        int days = Store.injDays(this);
        if (inj != null || days != Integer.MIN_VALUE) {
            LinearLayout c3 = card(null);
            label(c3, Store.injName(this), VIOLET);
            String badge = Store.injDoneToday(this) ? "Faite ✓" : days == Integer.MIN_VALUE ? "—" : days > 0 ? "J-" + days : days == 0 ? "Aujourd'hui" : "Retard " + (-days) + " j";
            TextView p = pill(c3, badge, VIOLET);
            ((LinearLayout.LayoutParams) p.getLayoutParams()).topMargin = dp(5);
            String lastD = inj != null ? inj.optString("last", "") : "", nextD = Store.injDate(this);
            if (lastD.length() == 10 && nextD.length() == 10) {
                double a = Store.dayNum(lastD), z = Store.dayNum(nextD), now = Store.dayNum(Store.today());
                Widgets.Progress pr = new Widgets.Progress(this);
                pr.set(z > a ? (float) ((now - a) / (z - a)) : 1, VIOLET);
                LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(-1, dp(6)); pl.topMargin = dp(8);
                c3.addView(pr, pl);
                LinearLayout ends = new LinearLayout(this);
                TextView l1 = text(ends, Store.short2(lastD), 10, T2, false); l1.setGravity(Gravity.START);
                l1.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
                TextView l2 = text(ends, Store.short2(nextD), 10, VIOLET, true); l2.setGravity(Gravity.END);
                l2.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
                LinearLayout.LayoutParams el = new LinearLayout.LayoutParams(-1, -2); el.topMargin = dp(3);
                c3.addView(ends, el);
            }
            if (inj != null) {
                LinearLayout r4 = statRow(c3);
                stat(r4, String.valueOf(inj.optInt("total")), "Injections", MINT);
                stat(r4, Store.num1(inj, "abd"), "Abdo moy.", ORANGE);
                stat(r4, Store.num1(inj, "reflux"), "Reflux moy.", BLUE);
            }
            if (days != Integer.MIN_VALUE && days <= 0 && !Store.injDoneToday(this)) {
                TextView ib = button(c3, "Injection faite", 0xFF4A3DA0, WHITE, null);
                ib.setOnClickListener(v -> {
                    if (!injConfirm) { injConfirm = true; ib.setText("Confirmer l'injection ?"); return; }
                    try { Store.saveInj(this); } catch (Exception ignored) {}
                    render();
                });
            }
        }

        // ── Prochains RDV ──
        JSONArray rdv = h.optJSONArray("rdv");
        if (rdv != null && rdv.length() > 0) {
            LinearLayout c4 = card(null);
            label(c4, "Prochains RDV", T2);
            for (int i = 0; i < rdv.length(); i++) {
                JSONObject r = rdv.optJSONObject(i); if (r == null) continue;
                String tag = r.optString("tag", "RDV");
                space(c4, 8);
                pill(c4, tag, tag.toLowerCase(java.util.Locale.FRENCH).contains("gastro") ? BLUE : GREEN);
                TextView w = text(c4, cap(r.optString("when")), 13, WHITE, true);
                w.setPadding(0, dp(4), 0, 0);
                String note = r.optString("note", "");
                if (!note.isEmpty()) { TextView n = text(c4, note, 10, T2, false); n.setMaxLines(3); n.setEllipsize(android.text.TextUtils.TruncateAt.END); }
            }
        }

        space(10);
        int w = Store.waiting(this);
        long at = Store.stateTime(this);
        text(w > 0 ? "⏳ " + w + " envoi" + (w > 1 ? "s" : "") + " en attente du téléphone"
            : at == 0 ? "Ouvre Health K' sur le téléphone pour relier la montre"
            : "✓ Synchronisé à " + Store.hhmm(at), 10, T2, false);
    }

    static String cap(String s) { return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
}

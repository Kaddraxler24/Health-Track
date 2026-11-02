package io.github.kaddraxler24.healthk;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Health K' sur la montre, sans appli à installer sur la montre :
 * des notifications interactives (reprises automatiquement par la Galaxy Watch) pour
 * le BASDAI complet (Q1 à Q6), le chrono de raideur matinale, la température et l'injection.
 * Tout est traité ici, sans ouvrir l'appli. Les réponses sont gardées en attente et
 * récupérées par l'appli à sa prochaine ouverture (WatchPlugin.takePending).
 */
public class WatchFlow extends BroadcastReceiver {

    static final String PREFS = "healthk_watch";
    static final String CH_Q = "hk_watch_q", CH_HUB = "hk_watch_hub";
    static final int ID_HUB = 7100, ID_Q = 7101, ID_MORNING = 7102, ID_CHRONO = 7103, ID_RESULT = 7104;
    static final String KEY_INPUT = "hk_value";

    static final String A_BASDAI = "hk.BASDAI", A_ANSWER = "hk.ANSWER", A_SAME = "hk.SAME", A_CANCEL = "hk.CANCEL",
        A_CHRONO_START = "hk.CHRONO_START", A_CHRONO_STOP = "hk.CHRONO_STOP", A_MORNING = "hk.MORNING",
        A_TEMP = "hk.TEMP", A_INJ = "hk.INJ", A_HUB = "hk.HUB",
        A_ALARM_BASDAI = "hk.ALARM_BASDAI", A_ALARM_MORNING = "hk.ALARM_MORNING", A_ALARM_HUB = "hk.ALARM_HUB";

    static final String[] Q_TITLE = { "",
        "Q1 · Fatigue", "Q2 · Douleur cou, dos, hanches", "Q3 · Douleur ou gonflement des articulations",
        "Q4 · Gêne aux points sensibles", "Q5 · Intensité de la raideur du matin", "Q6 · Durée de la raideur du matin" };
    static final String[] Q_TEXT = { "",
        "Ta fatigue cette semaine, de 0 (aucune) à 10 (extrême).",
        "Douleur au cou, au dos ou aux hanches, de 0 à 10.",
        "Douleur ou gonflement des autres articulations, de 0 à 10.",
        "Gêne au toucher ou à la pression (talons, bassin…), de 0 à 10.",
        "Intensité de la raideur au réveil, de 0 à 10.",
        "Combien de temps dure ta raideur le matin ?" };
    static final int[] Q6_CHOICES = { 0, 15, 30, 45, 60, 90, 120 };
    static final String[] TEMP_CHOICES = { "36,5", "37", "37,5", "37,8", "38", "38,5", "39", "39,5" };

    // ─────────────────────────── Réception des actions ───────────────────────────
    @Override
    public void onReceive(Context ctx, Intent in) {
        String a = in.getAction();
        if (a == null) return;
        try {
            switch (a) {
                case A_ALARM_BASDAI:
                    scheduleAll(ctx);
                    if (!isDone(ctx, today())) startBasdai(ctx, false);
                    break;
                case A_ALARM_MORNING:
                    scheduleAll(ctx);
                    if (morningMin(ctx, today()) < 0) postMorning(ctx);
                    break;
                case A_ALARM_HUB:
                    scheduleAll(ctx); postHub(ctx, false); break;
                case A_HUB: postHub(ctx, false); break;
                case A_BASDAI: startBasdai(ctx, true); break;
                case A_ANSWER: answer(ctx, in.getIntExtra("step", 0), readValue(in, in.getIntExtra("v", -1))); break;
                case A_SAME: same(ctx); break;
                case A_CANCEL:
                    prefs(ctx).edit().remove("flow").apply();
                    NotificationManagerCompat.from(ctx).cancel(ID_Q);
                    break;
                case A_CHRONO_START: chronoStart(ctx); break;
                case A_CHRONO_STOP: chronoStop(ctx); break;
                case A_MORNING: {
                    int v = readValue(in, in.getIntExtra("v", -1));
                    if (v >= 0) { saveMorning(ctx, today(), v); NotificationManagerCompat.from(ctx).cancel(ID_MORNING);
                        notifyInfo(ctx, ID_MORNING, "Raideur matinale : " + v + " min", "Reprise ce soir dans la Q6 du BASDAI."); refreshHubIfShown(ctx); }
                    break;
                }
                case A_TEMP: temp(ctx, in); break;
                case A_INJ: injDone(ctx); break;
            }
        } catch (Exception e) { android.util.Log.w("HealthK", "montre", e); }
    }

    // ─────────────────────────── BASDAI ───────────────────────────
    static void startBasdai(Context ctx, boolean showIfDone) throws Exception {
        String d = basdaiDate();
        JSONObject done = doneEntry(ctx, d);
        if (done != null) {
            if (showIfDone) notifyInfo(ctx, ID_RESULT, "BASDAI déjà fait : " + fr1(done.optDouble("basdai")),
                "Une évaluation par jour. " + level(done.optDouble("basdai")));
            return;
        }
        JSONObject flow = new JSONObject();
        flow.put("date", d);
        prefs(ctx).edit().putString("flow", flow.toString()).apply();
        postQuestion(ctx, 1, flow);
    }

    static void answer(Context ctx, int step, int v) throws Exception {
        JSONObject flow = flow(ctx);
        if (flow == null || step < 1 || step > 6) return;
        if (v < 0) { postQuestion(ctx, step, flow); return; }       // réponse illisible : on repose la question
        if (step <= 5) flow.put("q" + step, Math.min(10, v)); else flow.put("q6m", Math.min(600, v));
        prefs(ctx).edit().putString("flow", flow.toString()).apply();
        if (step < 6) postQuestion(ctx, step + 1, flow); else finish(ctx, flow);
    }

    /** « Comme la dernière fois » : reprend Q1 à Q5 de la dernière évaluation. */
    static void same(Context ctx) throws Exception {
        JSONObject flow = flow(ctx), last = cfg(ctx).optJSONObject("last");
        if (flow == null || last == null) return;
        for (int i = 1; i <= 5; i++) flow.put("q" + i, last.optInt("q" + i, 5));
        prefs(ctx).edit().putString("flow", flow.toString()).apply();
        postQuestion(ctx, 6, flow);
    }

    static void finish(Context ctx, JSONObject f) throws Exception {
        int q6m = f.optInt("q6m", 0);
        double q6 = q6Score(q6m);
        double b = (f.optInt("q1") + f.optInt("q2") + f.optInt("q3") + f.optInt("q4") + (f.optInt("q5") + q6) / 2.0) / 5.0;
        b = Math.round(b * 10) / 10.0;
        JSONObject e = new JSONObject();
        e.put("date", f.getString("date"));
        for (int i = 1; i <= 5; i++) e.put("q" + i, f.optInt("q" + i));
        e.put("q6m", q6m); e.put("q6", q6); e.put("basdai", b); e.put("at", System.currentTimeMillis());
        JSONObject p = pending(ctx);
        JSONArray entries = p.optJSONArray("entries"); if (entries == null) entries = new JSONArray();
        entries.put(e); p.put("entries", entries);
        prefs(ctx).edit().putString("pending", p.toString()).remove("flow").apply();
        NotificationManagerCompat.from(ctx).cancel(ID_Q);
        notifyInfo(ctx, ID_RESULT, "BASDAI " + fr1(b) + " enregistré",
            level(b) + "\nFatigue " + f.optInt("q1") + " · Dos " + f.optInt("q2") + " · Articulations " + f.optInt("q3")
            + " · Points sensibles " + f.optInt("q4") + " · Raideur " + f.optInt("q5") + " · " + q6m + " min.\nIl apparaîtra dans Health K' à sa prochaine ouverture.");
        // Le widget de l'écran d'accueil affiche tout de suite le nouveau score
        SharedPreferences.Editor w = ctx.getSharedPreferences(HealthKWidget.PREFS, Context.MODE_PRIVATE).edit();
        String d = f.getString("date");
        w.putString("basdai", fr1(b)).putString("basdaiDate", d.substring(8, 10) + "/" + d.substring(5, 7)).apply();
        HealthKWidget.refreshAll(ctx);
        refreshHubIfShown(ctx);
    }

    static void postQuestion(Context ctx, int step, JSONObject flow) throws Exception {
        NotificationCompat.Builder nb = base(ctx, CH_Q)
            .setContentTitle(Q_TITLE[step] + "  (" + step + "/6)")
            .setContentText(Q_TEXT[step])
            .setStyle(new NotificationCompat.BigTextStyle().bigText(Q_TEXT[step]))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(false);
        NotificationCompat.WearableExtender we = new NotificationCompat.WearableExtender();
        List<String> labels = new ArrayList<>();
        if (step <= 5) {
            if (step == 1 && cfg(ctx).optJSONObject("last") != null)
                we.addAction(action(ctx, A_SAME, "↺ Comme la dernière fois", new Bundle(), 900));
            for (int v = 0; v <= 10; v++) {
                Bundle x = new Bundle(); x.putInt("step", step); x.putInt("v", v);
                String t = v == 0 ? "0 · aucune" : v == 10 ? "10 · extrême" : String.valueOf(v);
                we.addAction(action(ctx, A_ANSWER, t, x, step * 100 + v));
                labels.add(String.valueOf(v));
            }
        } else {
            int m = morningMin(ctx, flow.optString("date", today()));
            if (m >= 0) {
                Bundle x = new Bundle(); x.putInt("step", 6); x.putInt("v", m);
                we.addAction(action(ctx, A_ANSWER, "✓ " + m + " min (mesure du matin)", x, 699));
                labels.add(m + " min");
                nb.setContentText("Ce matin, tu as noté " + m + " min. Garder cette durée ?");
            }
            for (int c : Q6_CHOICES) {
                Bundle x = new Bundle(); x.putInt("step", 6); x.putInt("v", c);
                String t = c == 0 ? "Aucune" : c >= 120 ? "2 h ou plus" : c + " min";
                we.addAction(action(ctx, A_ANSWER, t, x, 600 + c));
                if (!labels.contains(c + " min")) labels.add(c + " min");
            }
        }
        we.addAction(action(ctx, A_CANCEL, "✕ Arrêter", new Bundle(), 999));
        // Sur le téléphone : une réponse avec choix (et saisie libre) + « Arrêter »
        Bundle x = new Bundle(); x.putInt("step", step);
        nb.addAction(inputAction(ctx, A_ANSWER, "Répondre", labels.toArray(new String[0]), x, 800 + step));
        nb.addAction(action(ctx, A_CANCEL, "Arrêter", new Bundle(), 998));
        nb.extend(we);
        NotificationManagerCompat nm = NotificationManagerCompat.from(ctx);
        nm.cancel(ID_Q);                  // nouvelle notification : la montre la présente et vibre à chaque question
        notify(ctx, ID_Q, nb);
    }

    // ─────────────────────────── Raideur matinale ───────────────────────────
    static void postMorning(Context ctx) {
        NotificationCompat.Builder nb = base(ctx, CH_Q)
            .setContentTitle("Raideur matinale")
            .setContentText("Lance le chrono maintenant, ou note directement la durée.")
            .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationCompat.WearableExtender we = new NotificationCompat.WearableExtender();
        we.addAction(action(ctx, A_CHRONO_START, "▶ Démarrer le chrono", new Bundle(), 300));
        String[] labels = { "Aucune", "15 min", "30 min", "45 min", "1 h", "1 h 30", "2 h" };
        int[] vals = { 0, 15, 30, 45, 60, 90, 120 };
        for (int i = 0; i < vals.length; i++) { Bundle x = new Bundle(); x.putInt("v", vals[i]); we.addAction(action(ctx, A_MORNING, labels[i], x, 310 + i)); }
        nb.addAction(action(ctx, A_CHRONO_START, "Chrono", new Bundle(), 301));
        nb.addAction(inputAction(ctx, A_MORNING, "Durée", new String[] { "0", "15", "30", "45", "60", "90", "120" }, new Bundle(), 302));
        nb.extend(we);
        notify(ctx, ID_MORNING, nb);
    }

    static void chronoStart(Context ctx) throws Exception {
        long now = System.currentTimeMillis();
        JSONObject c = new JSONObject(); c.put("date", today()); c.put("start", now);
        prefs(ctx).edit().putString("chrono", c.toString()).apply();
        NotificationManagerCompat.from(ctx).cancel(ID_MORNING);
        NotificationCompat.Builder nb = base(ctx, CH_Q)
            .setContentTitle("Chrono raideur lancé à " + hhmm(now))
            .setContentText("Touche « Raideur partie » quand tu te sens délié.")
            .setWhen(now).setUsesChronometer(true).setShowWhen(true)
            .setAutoCancel(false).setOnlyAlertOnce(true);
        NotificationCompat.Action stop = action(ctx, A_CHRONO_STOP, "■ Raideur partie", new Bundle(), 320);
        nb.addAction(stop);
        nb.extend(new NotificationCompat.WearableExtender().addAction(stop));
        notify(ctx, ID_CHRONO, nb);
        refreshHubIfShown(ctx);
    }

    static void chronoStop(Context ctx) throws Exception {
        String s = prefs(ctx).getString("chrono", null);
        if (s == null) return;
        JSONObject c = new JSONObject(s);
        int min = (int) Math.max(0, Math.round((System.currentTimeMillis() - c.getLong("start")) / 60000.0));
        saveMorning(ctx, c.optString("date", today()), min);
        prefs(ctx).edit().remove("chrono").apply();
        NotificationManagerCompat.from(ctx).cancel(ID_CHRONO);
        notifyInfo(ctx, ID_MORNING, "Raideur matinale : " + min + " min", "Reprise ce soir dans la Q6 du BASDAI.");
        refreshHubIfShown(ctx);
    }

    static void saveMorning(Context ctx, String date, int min) throws Exception {
        JSONObject p = pending(ctx);
        JSONObject m = p.optJSONObject("morning"); if (m == null) m = new JSONObject();
        m.put(date, min); p.put("morning", m);
        prefs(ctx).edit().putString("pending", p.toString()).apply();
    }

    // ─────────────────────────── Température et injection ───────────────────────────
    static void temp(Context ctx, Intent in) throws Exception {
        Double v = null;
        String txt = inputText(in);
        if (txt != null) v = parseTemp(txt);
        if (v == null && in.hasExtra("t")) v = parseTemp(in.getStringExtra("t"));
        if (v == null) { notifyInfo(ctx, ID_RESULT, "Température non comprise", "Réessaie depuis le menu Health K', par exemple « 37,8 »."); return; }
        JSONObject p = pending(ctx);
        JSONArray t = p.optJSONArray("temps"); if (t == null) t = new JSONArray();
        JSONObject o = new JSONObject(); o.put("value", v); o.put("at", System.currentTimeMillis()); o.put("date", today());
        t.put(o); p.put("temps", t);
        prefs(ctx).edit().putString("pending", p.toString()).apply();
        String msg = v >= 38 ? "Fièvre : pas d'injection IMRALDI sans avis médical." : v >= 37.5 ? "Légère élévation : à surveiller." : "Température normale.";
        notifyInfo(ctx, ID_RESULT, "Température " + fr1(v) + " °C notée", msg);
        refreshHubIfShown(ctx);
    }

    static void injDone(Context ctx) throws Exception {
        String d = today();
        JSONObject p = pending(ctx);
        JSONArray a = p.optJSONArray("inj"); if (a == null) a = new JSONArray();
        for (int i = 0; i < a.length(); i++) if (d.equals(a.optString(i))) return;
        a.put(d); p.put("inj", a);
        prefs(ctx).edit().putString("pending", p.toString()).apply();
        notifyInfo(ctx, ID_RESULT, "Injection du jour notée ✓", "La prochaine sera recalculée à l'ouverture de Health K'.");
        refreshHubIfShown(ctx);
    }

    // ─────────────────────────── Menu « Health K' » (remplace la tuile) ───────────────────────────
    static void postHub(Context ctx, boolean silent) throws Exception {
        JSONObject cfg = cfg(ctx);
        String d = basdaiDate();
        JSONObject done = doneEntry(ctx, d);
        String inj = injLine(ctx, cfg);
        String line = (done != null ? "BASDAI du jour : " + fr1(done.optDouble("basdai")) : "BASDAI du jour : à faire")
            + (inj != null ? " · " + inj : "");
        boolean chrono = prefs(ctx).getString("chrono", null) != null;
        NotificationCompat.Builder nb = base(ctx, CH_HUB)
            .setContentTitle("Health K'")
            .setContentText(line)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(line))
            .setOnlyAlertOnce(true).setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        if (silent) nb.setSilent(true);
        NotificationCompat.WearableExtender we = new NotificationCompat.WearableExtender();
        NotificationCompat.Action b = action(ctx, A_BASDAI, done != null ? "BASDAI ✓ " + fr1(done.optDouble("basdai")) : "Faire le BASDAI", new Bundle(), 400);
        NotificationCompat.Action c = chrono ? action(ctx, A_CHRONO_STOP, "■ Raideur partie", new Bundle(), 401) : action(ctx, A_CHRONO_START, "▶ Chrono raideur", new Bundle(), 402);
        NotificationCompat.Action t = inputAction(ctx, A_TEMP, "Température", TEMP_CHOICES, new Bundle(), 403);
        we.addAction(b); we.addAction(c); we.addAction(t);
        nb.addAction(b); nb.addAction(c); nb.addAction(t);
        String injDate = cfg.optString("injDate", "");
        if (!injDate.isEmpty() && injDate.compareTo(today()) <= 0 && !injDoneToday(ctx, cfg))
            we.addAction(action(ctx, A_INJ, "✓ Injection faite", new Bundle(), 404));
        nb.extend(we);
        notify(ctx, ID_HUB, nb);
    }

    static void refreshHubIfShown(Context ctx) {
        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            for (StatusBarNotification s : nm.getActiveNotifications()) if (s.getId() == ID_HUB) { postHub(ctx, true); return; }
        } catch (Exception ignored) {}
    }

    static String injLine(Context ctx, JSONObject cfg) {
        String d = cfg.optString("injDate", "");
        if (d.isEmpty()) return null;
        if (injDoneToday(ctx, cfg)) return "injection faite ✓";
        long days = Math.round((parse(d).getTimeInMillis() - parse(today()).getTimeInMillis()) / 86400000.0);
        String name = cfg.optString("injName", "Injection");
        if (days > 0) return name + " J-" + days;
        if (days == 0) return name + " aujourd'hui";
        return name + " en retard de " + (-days) + " j";
    }

    static boolean injDoneToday(Context ctx, JSONObject cfg) {
        String d = today();
        JSONArray a = pending(ctx).optJSONArray("inj");
        if (a != null) for (int i = 0; i < a.length(); i++) if (d.equals(a.optString(i))) return true;
        return d.equals(cfg.optString("lastInj", ""));
    }

    // ─────────────────────────── Alarmes ───────────────────────────
    static void scheduleAll(Context ctx) {
        JSONObject c = cfg(ctx);
        boolean on = c.optBoolean("on", false);
        alarm(ctx, A_ALARM_BASDAI, 1, on && c.optBoolean("basdaiOn", true), c.optString("basdaiTime", "21:00"));
        alarm(ctx, A_ALARM_MORNING, 2, on && c.optBoolean("morningOn", true), c.optString("morningTime", "08:00"));
        alarm(ctx, A_ALARM_HUB, 3, on && c.optBoolean("hubOn", true), c.optString("hubTime", "07:30"));
    }

    static void alarm(Context ctx, String action, int code, boolean on, String hhmm) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(ctx, WatchFlow.class).setAction(action);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
        if (!on) return;
        Calendar t = Calendar.getInstance();
        String[] hm = hhmm.split(":");
        t.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hm[0])); t.set(Calendar.MINUTE, Integer.parseInt(hm[1]));
        t.set(Calendar.SECOND, 0); t.set(Calendar.MILLISECOND, 0);
        if (t.getTimeInMillis() <= System.currentTimeMillis() + 1000) t.add(Calendar.DAY_OF_YEAR, 1);
        try {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), pi);
        } catch (SecurityException e) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), pi); }
    }

    // ─────────────────────────── Notifications : outils ───────────────────────────
    static void channels(Context ctx) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel q = new NotificationChannel(CH_Q, "Questions sur la montre", NotificationManager.IMPORTANCE_HIGH);
        q.setDescription("BASDAI, raideur matinale : questions auxquelles on répond depuis la montre");
        q.enableVibration(true); q.setVibrationPattern(new long[] { 0, 120 });
        nm.createNotificationChannel(q);
        NotificationChannel h = new NotificationChannel(CH_HUB, "Menu Health K' sur la montre", NotificationManager.IMPORTANCE_DEFAULT);
        h.setDescription("Menu du jour (BASDAI, chrono, température, injection)");
        h.setSound(null, null); h.enableVibration(true); h.setVibrationPattern(new long[] { 0, 60 });
        nm.createNotificationChannel(h);
    }

    static NotificationCompat.Builder base(Context ctx, String ch) {
        channels(ctx);
        Intent open = new Intent(ctx, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return new NotificationCompat.Builder(ctx, ch)
            .setSmallIcon(R.drawable.ic_stat_hk)
            .setColor(0xFF0F9F9A)
            .setContentIntent(PendingIntent.getActivity(ctx, 7000, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    }

    static void notify(Context ctx, int id, NotificationCompat.Builder nb) {
        try { NotificationManagerCompat.from(ctx).notify(id, nb.build()); } catch (SecurityException e) { /* notifications refusées */ }
    }

    static void notifyInfo(Context ctx, int id, String title, String text) {
        NotificationCompat.Builder nb = base(ctx, CH_Q).setContentTitle(title).setContentText(text)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true).setTimeoutAfter(6 * 3600 * 1000L);
        notify(ctx, id, nb);
    }

    static NotificationCompat.Action action(Context ctx, String a, String title, Bundle extras, int code) {
        Intent i = new Intent(ctx, WatchFlow.class).setAction(a).putExtras(extras);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 7000 + code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Action.Builder(R.drawable.ic_stat_hk, title, pi).build();
    }

    static NotificationCompat.Action inputAction(Context ctx, String a, String title, String[] choices, Bundle extras, int code) {
        Intent i = new Intent(ctx, WatchFlow.class).setAction(a).putExtras(extras);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 7000 + code, i, flags);
        RemoteInput ri = new RemoteInput.Builder(KEY_INPUT).setLabel(title).setChoices(choices).setAllowFreeFormInput(true).build();
        return new NotificationCompat.Action.Builder(R.drawable.ic_stat_hk, title, pi).addRemoteInput(ri).setAllowGeneratedReplies(false).build();
    }

    static String inputText(Intent in) {
        Bundle r = RemoteInput.getResultsFromIntent(in);
        if (r == null) return null;
        CharSequence s = r.getCharSequence(KEY_INPUT);
        return s == null ? null : s.toString();
    }

    /** Valeur choisie : bouton (extra « v ») ou réponse tapée / dictée (« 7 », « sept », « 30 min », « 1 h »). */
    static int readValue(Intent in, int fallback) {
        String s = inputText(in);
        if (s == null) return fallback;
        return parseValue(s);
    }

    static final String[] WORDS = { "zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf", "dix" };

    static int parseValue(String raw) {
        String s = raw.toLowerCase(Locale.FRENCH).trim();
        if (s.startsWith("aucun")) return 0;
        java.util.regex.Matcher h = java.util.regex.Pattern.compile("(\\d+)\\s*h\\s*(\\d+)?").matcher(s);
        if (h.find()) return Integer.parseInt(h.group(1)) * 60 + (h.group(2) != null ? Integer.parseInt(h.group(2)) : 0);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(s);
        if (m.find()) return Integer.parseInt(m.group());
        for (int i = WORDS.length - 1; i >= 0; i--) if (s.equals(WORDS[i]) || s.startsWith(WORDS[i] + " ")) return i;
        if (s.equals("une")) return 1;
        return -1;
    }

    static Double parseTemp(String raw) {
        if (raw == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{2})(?:[.,](\\d))?").matcher(raw);
        if (!m.find()) return null;
        double v = Integer.parseInt(m.group(1)) + (m.group(2) != null ? Integer.parseInt(m.group(2)) / 10.0 : 0);
        return v >= 34 && v <= 43 ? v : null;
    }

    // ─────────────────────────── Données ───────────────────────────
    static SharedPreferences prefs(Context ctx) { return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    static JSONObject cfg(Context ctx) { return obj(prefs(ctx).getString("cfg", null)); }
    static JSONObject pending(Context ctx) { return obj(prefs(ctx).getString("pending", null)); }
    static JSONObject flow(Context ctx) { String s = prefs(ctx).getString("flow", null); return s == null ? null : obj(s); }
    static JSONObject obj(String s) { try { return s == null ? new JSONObject() : new JSONObject(s); } catch (Exception e) { return new JSONObject(); } }

    /** Évaluation déjà faite ce jour-là (dans l'appli ou depuis la montre) ? */
    static JSONObject doneEntry(Context ctx, String d) {
        JSONArray e = pending(ctx).optJSONArray("entries");
        if (e != null) for (int i = 0; i < e.length(); i++) { JSONObject o = e.optJSONObject(i); if (o != null && d.equals(o.optString("date"))) return o; }
        JSONObject done = cfg(ctx).optJSONObject("done");
        if (done != null && done.has(d)) { JSONObject o = new JSONObject(); try { o.put("basdai", done.optDouble(d)); } catch (Exception ignored) {} return o; }
        return null;
    }
    static boolean isDone(Context ctx, String d) { return doneEntry(ctx, d) != null; }

    static int morningMin(Context ctx, String d) {
        JSONObject m = pending(ctx).optJSONObject("morning");
        if (m != null && m.has(d)) return m.optInt(d, -1);
        JSONObject c = cfg(ctx).optJSONObject("morning");
        if (c != null && c.has(d)) return c.optInt(d, -1);
        return -1;
    }

    static double q6Score(int min) {
        double s;
        if (min <= 30) s = min / 12.0; else if (min <= 60) s = 2.5 + (min - 30) / 12.0;
        else if (min <= 90) s = 5 + (min - 60) / 12.0; else if (min <= 120) s = 7.5 + (min - 90) / 12.0; else s = 10;
        return Math.min(10, Math.round(s * 10) / 10.0);
    }

    static String level(double b) { return b < 4 ? "Sous le seuil d'activité (4)." : b < 6 ? "Maladie active (4 à 6)." : "Maladie très active (plus de 6)."; }
    static String fr1(double v) { return String.format(Locale.FRENCH, "%.1f", v).replace(".", ","); }
    static String hhmm(long t) { Calendar c = Calendar.getInstance(); c.setTimeInMillis(t); return String.format(Locale.FRENCH, "%d h %02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)); }

    static String day(Calendar c) { return String.format(Locale.ROOT, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)); }
    static String today() { return day(Calendar.getInstance()); }
    /** Comme dans l'appli : avant 4 h du matin, l'évaluation compte pour la veille. */
    static String basdaiDate() { Calendar c = Calendar.getInstance(); if (c.get(Calendar.HOUR_OF_DAY) < 4) c.add(Calendar.DAY_OF_YEAR, -1); return day(c); }
    static Calendar parse(String d) {
        Calendar c = Calendar.getInstance(); c.clear();
        c.set(Integer.parseInt(d.substring(0, 4)), Integer.parseInt(d.substring(5, 7)) - 1, Integer.parseInt(d.substring(8, 10)));
        return c;
    }
}

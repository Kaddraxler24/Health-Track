package io.github.kaddraxler24.healthk.wear;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.wear.tiles.TileService;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.json.JSONObject;

/**
 * Données de la montre.
 * - « state » : état reçu du téléphone (horaires, BASDAI faits, raideur, injection).
 * - « local » : ce qui a été noté sur la montre et pas encore confirmé par le téléphone.
 * - envois : un élément de données par réponse ; le système le garde et le transmet dès que le téléphone est joignable,
 *   et le téléphone l'efface une fois enregistré (c'est l'accusé de réception).
 */
public final class Store {
    private Store() {}

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("hk_wear", Context.MODE_PRIVATE); }
    static JSONObject obj(String s) { try { return s == null ? new JSONObject() : new JSONObject(s); } catch (Exception e) { return new JSONObject(); } }

    static JSONObject state(Context c) { return obj(prefs(c).getString("state", null)); }
    static JSONObject local(Context c) { return obj(prefs(c).getString("local", null)); }
    static long stateTime(Context c) { return prefs(c).getLong("stateAt", 0); }

    static void setState(Context c, String json) {
        prefs(c).edit().putString("state", json).putLong("stateAt", System.currentTimeMillis()).apply();
        refreshSurfaces(c);
        Reminders.schedule(c);
    }

    // ─────────────── Lecture ───────────────
    /** Score BASDAI déjà noté ce jour-là (montre ou téléphone), ou -1. */
    static double doneScore(Context c, String day) {
        JSONObject l = local(c).optJSONObject("done");
        if (l != null && l.has(day)) return l.optDouble(day, -1);
        JSONObject d = state(c).optJSONObject("done");
        if (d != null && d.has(day)) return d.optDouble(day, -1);
        return -1;
    }

    static int morningMin(Context c, String day) {
        JSONObject l = local(c).optJSONObject("morning");
        if (l != null && l.has(day)) return l.optInt(day, -1);
        JSONObject m = state(c).optJSONObject("morning");
        if (m != null && m.has(day)) return m.optInt(day, -1);
        return -1;
    }

    static JSONObject lastAnswers(Context c) {
        JSONObject l = local(c).optJSONObject("last");
        return l != null ? l : state(c).optJSONObject("last");
    }

    static String injDate(Context c) { return state(c).optString("injDate", ""); }
    static String injName(Context c) { return state(c).optString("injName", "Injection"); }
    static boolean injDoneToday(Context c) {
        String t = today();
        return t.equals(local(c).optString("inj", "")) || t.equals(state(c).optString("lastInj", ""));
    }
    /** Nombre de jours avant l'injection (négatif = en retard), ou Integer.MIN_VALUE si inconnue. */
    static int injDays(Context c) {
        String d = injDate(c);
        if (d.length() != 10) return Integer.MIN_VALUE;
        return (int) Math.round((parse(d).getTimeInMillis() - parse(today()).getTimeInMillis()) / 86400000.0);
    }
    static String injLine(Context c) {
        if (injDoneToday(c)) return injName(c) + " faite ✓";
        int n = injDays(c);
        if (n == Integer.MIN_VALUE) return null;
        if (n > 0) return injName(c) + " J-" + n;
        if (n == 0) return injName(c) + " aujourd'hui";
        return injName(c) + " en retard de " + (-n) + " j";
    }

    // ─────────────── Historique (envoyé par le téléphone) ───────────────
    static JSONObject hist(Context c) { JSONObject h = state(c).optJSONObject("hist"); return h != null ? h : new JSONObject(); }

    /** Derniers BASDAI (du plus ancien au plus récent), y compris ceux notés sur la montre et pas encore synchronisés. */
    static java.util.List<double[]> series(Context c) {
        java.util.TreeMap<String, Double> m = new java.util.TreeMap<>();
        org.json.JSONArray a = hist(c).optJSONArray("series");
        if (a != null) for (int i = 0; i < a.length(); i++) { JSONObject o = a.optJSONObject(i); if (o != null) m.put(o.optString("d"), o.optDouble("b")); }
        JSONObject l = local(c).optJSONObject("done");
        if (l != null) for (Iterator<String> it = l.keys(); it.hasNext(); ) { String k = it.next(); m.put(k, l.optDouble(k)); }
        java.util.List<double[]> out = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Double> e : m.entrySet()) out.add(new double[] { dayNum(e.getKey()), e.getValue() });
        while (out.size() > 14) out.remove(0);
        return out;
    }
    static java.util.List<String> seriesDays(Context c) {
        java.util.TreeSet<String> s = new java.util.TreeSet<>();
        org.json.JSONArray a = hist(c).optJSONArray("series");
        if (a != null) for (int i = 0; i < a.length(); i++) { JSONObject o = a.optJSONObject(i); if (o != null) s.add(o.optString("d")); }
        JSONObject l = local(c).optJSONObject("done");
        if (l != null) for (Iterator<String> it = l.keys(); it.hasNext(); ) s.add(it.next());
        java.util.List<String> out = new java.util.ArrayList<>(s);
        while (out.size() > 14) out.remove(0);
        return out;
    }
    static boolean isLocal(Context c, String day) {
        JSONObject l = local(c).optJSONObject("done");
        if (l == null || !l.has(day)) return false;
        org.json.JSONArray a = hist(c).optJSONArray("series");
        if (a != null) for (int i = 0; i < a.length(); i++) { JSONObject o = a.optJSONObject(i); if (o != null && day.equals(o.optString("d"))) return false; }
        return true;
    }
    static double dayNum(String d) { try { return parse(d).getTimeInMillis() / 86400000.0; } catch (Exception e) { return 0; } }

    static final String[] DOW = { "dim.", "lun.", "mar.", "mer.", "jeu.", "ven.", "sam." };
    static final String[] MON = { "janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc." };
    /** « ven. 30 oct. » */
    static String dayLabel(String d) {
        try { Calendar c = parse(d); return DOW[c.get(Calendar.DAY_OF_WEEK) - 1] + " " + c.get(Calendar.DAY_OF_MONTH) + " " + MON[c.get(Calendar.MONTH)]; }
        catch (Exception e) { return d; }
    }
    /** « 05/11 » */
    static String short2(String d) { return d != null && d.length() == 10 ? d.substring(8, 10) + "/" + d.substring(5, 7) : "—"; }
    static String num1(JSONObject o, String k) { return o != null && o.has(k) && !o.isNull(k) ? fr1(o.optDouble(k)) : "—"; }

    static int waiting(Context c) { return prefs(c).getStringSet("out", new HashSet<>()).size(); }

    // ─────────────── Écriture ───────────────
    static void saveBasdai(Context c, String day, int[] q, int q6m, double score, int abd, int reflux) throws Exception {
        JSONObject l = local(c);
        JSONObject d = l.optJSONObject("done"); if (d == null) d = new JSONObject();
        d.put(day, score); l.put("done", d);
        JSONObject last = new JSONObject(); for (int i = 1; i <= 5; i++) last.put("q" + i, q[i]); l.put("last", last);
        prefs(c).edit().putString("local", l.toString()).apply();
        JSONObject o = new JSONObject(); o.put("type", "basdai"); o.put("date", day);
        for (int i = 1; i <= 5; i++) o.put("q" + i, q[i]);
        o.put("q6m", q6m);
        if (abd >= 0) o.put("abd", abd);
        if (reflux >= 0) o.put("reflux", reflux);
        send(c, o);
    }

    static void saveMorning(Context c, String day, int min) throws Exception {
        JSONObject l = local(c);
        JSONObject m = l.optJSONObject("morning"); if (m == null) m = new JSONObject();
        m.put(day, min); l.put("morning", m);
        prefs(c).edit().putString("local", l.toString()).remove("chrono").apply();
        JSONObject o = new JSONObject(); o.put("type", "morning"); o.put("date", day); o.put("min", min);
        send(c, o);
    }

    static void saveTemp(Context c, double v) throws Exception {
        JSONObject o = new JSONObject(); o.put("type", "temp"); o.put("date", today()); o.put("value", v); o.put("at", System.currentTimeMillis());
        prefs(c).edit().putString("lastTemp", fr1(v) + " °C à " + hhmm(System.currentTimeMillis())).apply();
        send(c, o);
    }

    static void saveInj(Context c) throws Exception {
        JSONObject l = local(c); l.put("inj", today());
        prefs(c).edit().putString("local", l.toString()).apply();
        JSONObject o = new JSONObject(); o.put("type", "inj"); o.put("date", today());
        send(c, o);
    }

    static void send(Context c, JSONObject o) {
        String id = UUID.randomUUID().toString();
        Set<String> out = new HashSet<>(prefs(c).getStringSet("out", new HashSet<>()));
        out.add(id);
        prefs(c).edit().putStringSet("out", out).apply();
        try {
            PutDataMapRequest r = PutDataMapRequest.create("/hk/out/" + id);
            r.getDataMap().putString("json", o.toString());
            r.getDataMap().putLong("t", System.currentTimeMillis());
            Wearable.getDataClient(c).putDataItem(r.asPutDataRequest().setUrgent());
        } catch (Throwable ignored) {}
        refreshSurfaces(c);
    }

    /** Le téléphone a enregistré la réponse (il a effacé l'élément). */
    static void acked(Context c, String id) {
        Set<String> out = new HashSet<>(prefs(c).getStringSet("out", new HashSet<>()));
        if (out.remove(id)) prefs(c).edit().putStringSet("out", out).apply();
    }

    /** Nettoie les notes locales déjà confirmées par le téléphone (plus vieilles que 3 jours). */
    static void prune(Context c) {
        try {
            JSONObject l = local(c);
            String lim = day(-3);
            for (String k : new String[] { "done", "morning" }) {
                JSONObject o = l.optJSONObject(k); if (o == null) continue;
                for (Iterator<String> it = o.keys(); it.hasNext(); ) if (it.next().compareTo(lim) < 0) it.remove();
            }
            prefs(c).edit().putString("local", l.toString()).apply();
        } catch (Exception ignored) {}
    }

    static void hello(Context c) {
        try {
            Wearable.getNodeClient(c).getConnectedNodes().addOnSuccessListener(nodes -> {
                for (Node n : nodes) Wearable.getMessageClient(c).sendMessage(n.getId(), "/hk/hello", new byte[0]);
            });
        } catch (Throwable ignored) {}
    }

    static void refreshSurfaces(Context c) {
        try { TileService.getUpdater(c).requestUpdate(HkTile.class); } catch (Throwable ignored) {}
    }

    // ─────────────── Calculs ───────────────
    static double q6Score(int min) {
        double s;
        if (min <= 30) s = min / 12.0; else if (min <= 60) s = 2.5 + (min - 30) / 12.0;
        else if (min <= 90) s = 5 + (min - 60) / 12.0; else if (min <= 120) s = 7.5 + (min - 90) / 12.0; else s = 10;
        return Math.min(10, Math.round(s * 10) / 10.0);
    }
    static double basdai(int[] q, int q6m) {
        return Math.round(((q[1] + q[2] + q[3] + q[4] + (q[5] + q6Score(q6m)) / 2.0) / 5.0) * 10) / 10.0;
    }
    static int color(double b) { return b < 4 ? 0xFF22C17A : b < 6 ? 0xFFF0A030 : 0xFFF2685E; }
    static String level(double b) { return b < 4 ? "Sous le seuil d'activité" : b < 6 ? "Maladie active (4 à 6)" : "Maladie très active (> 6)"; }

    static String fr1(double v) { return String.format(Locale.FRENCH, "%.1f", v).replace(".", ","); }
    static String hhmm(long t) { Calendar c = Calendar.getInstance(); c.setTimeInMillis(t); return String.format(Locale.FRENCH, "%d h %02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)); }
    static String minutes(int m) { if (m < 60) return m + " min"; return (m / 60) + " h" + (m % 60 > 0 ? String.format(Locale.FRENCH, " %02d", m % 60) : ""); }

    static String fmt(Calendar c) { return String.format(Locale.ROOT, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)); }
    static String today() { return fmt(Calendar.getInstance()); }
    static String day(int offset) { Calendar c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, offset); return fmt(c); }
    /** Comme dans l'appli : avant 4 h du matin, l'évaluation compte pour la veille. */
    static String basdaiDay() { Calendar c = Calendar.getInstance(); if (c.get(Calendar.HOUR_OF_DAY) < 4) c.add(Calendar.DAY_OF_YEAR, -1); return fmt(c); }
    static Calendar parse(String d) {
        Calendar c = Calendar.getInstance(); c.clear();
        c.set(Integer.parseInt(d.substring(0, 4)), Integer.parseInt(d.substring(5, 7)) - 1, Integer.parseInt(d.substring(8, 10)));
        return c;
    }
}

package io.github.kaddraxler24.healthk.wear;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.Calendar;
import org.json.JSONObject;

/** Rappels sur la montre (horaires réglés dans Health K' → Rappels) et notification du chrono. */
public class Reminders extends BroadcastReceiver {

    static final String CH = "hk_rappels", CH_CHRONO = "hk_chrono";
    static final int N_BASDAI = 1, N_MORNING = 2, N_CHRONO = 3, N_OPEN = 4;

    @Override
    public void onReceive(Context c, Intent in) {
        String a = in.getAction();
        if (a == null) return;
        if (Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) { schedule(c); return; }
        schedule(c);
        if ("hk.BASDAI".equals(a) && Store.doneScore(c, Store.basdaiDay()) < 0)
            post(c, N_BASDAI, "Évaluation BASDAI du soir", "6 questions, environ 1 minute.", BasdaiActivity.class);
        if ("hk.MORNING".equals(a) && Store.morningMin(c, Store.today()) < 0 && Store.prefs(c).getString("chrono", null) == null)
            post(c, N_MORNING, "Raideur du matin", "Lance le chrono ou note la durée.", ChronoActivity.class);
    }

    static void schedule(Context c) {
        JSONObject s = Store.state(c);
        boolean on = s.optBoolean("on", true);
        alarm(c, "hk.BASDAI", 1, on && s.optBoolean("basdaiOn", true), s.optString("basdaiTime", "21:00"));
        alarm(c, "hk.MORNING", 2, on && s.optBoolean("morningOn", true), s.optString("morningTime", "08:00"));
    }

    static void alarm(Context c, String action, int code, boolean on, String hhmm) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(c, code, new Intent(c, Reminders.class).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
        if (!on) return;
        Calendar t = Calendar.getInstance();
        try {
            String[] hm = hhmm.split(":");
            t.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hm[0])); t.set(Calendar.MINUTE, Integer.parseInt(hm[1]));
        } catch (Exception e) { return; }
        t.set(Calendar.SECOND, 0); t.set(Calendar.MILLISECOND, 0);
        if (t.getTimeInMillis() <= System.currentTimeMillis() + 1000) t.add(Calendar.DAY_OF_YEAR, 1);
        try {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), pi);
        } catch (SecurityException e) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), pi); }
    }

    static void channels(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CH, "Rappels Health K'", NotificationManager.IMPORTANCE_HIGH);
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);
        NotificationChannel cr = new NotificationChannel(CH_CHRONO, "Chrono de raideur", NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(cr);
    }

    static PendingIntent open(Context c, Class<?> cls, int code) {
        Intent i = new Intent(c, cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, 100 + code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void post(Context c, int id, String title, String text, Class<?> cls) {
        channels(c);
        PendingIntent pi = open(c, cls, id);
        NotificationCompat.Builder nb = new NotificationCompat.Builder(c, CH)
            .setSmallIcon(R.drawable.ic_stat_hk).setColor(0xFF0F9F9A)
            .setContentTitle(title).setContentText(text)
            .setContentIntent(pi).setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.drawable.ic_stat_hk, "Commencer", pi);
        try { NotificationManagerCompat.from(c).notify(id, nb.build()); } catch (SecurityException ignored) {}
    }

    /** Notification permanente pendant le chrono (start = 0 pour l'enlever). */
    static void chronoNotif(Context c, long start) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(c);
        nm.cancel(N_MORNING);
        if (start <= 0) { nm.cancel(N_CHRONO); return; }
        channels(c);
        NotificationCompat.Builder nb = new NotificationCompat.Builder(c, CH_CHRONO)
            .setSmallIcon(R.drawable.ic_stat_hk).setColor(0xFF0F9F9A)
            .setContentTitle("Chrono raideur").setContentText("Touche quand la raideur est partie")
            .setWhen(start).setUsesChronometer(true).setShowWhen(true)
            .setOngoing(true).setContentIntent(open(c, ChronoActivity.class, N_CHRONO));
        try { nm.notify(N_CHRONO, nb.build()); } catch (SecurityException ignored) {}
    }
}

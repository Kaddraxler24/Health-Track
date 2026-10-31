package io.github.kaddraxler24.healthk;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Widget de l'écran d'accueil : prochaine injection, dernier BASDAI, nuit, actions rapides. */
public class HealthKWidget extends AppWidgetProvider {

    static final String PREFS = "healthk_widget";

    static void refreshAll(Context ctx) {
        AppWidgetManager m = AppWidgetManager.getInstance(ctx);
        int[] ids = m.getAppWidgetIds(new ComponentName(ctx, HealthKWidget.class));
        for (int id : ids) render(ctx, m, id);
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager m, int[] ids) {
        for (int id : ids) render(ctx, m, id);
    }

    private static PendingIntent open(Context ctx, String path, int code) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("healthk://" + path), ctx, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void render(Context ctx, AppWidgetManager m, int id) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.healthk_widget);

        String inj = "Injection : —";
        String injDate = p.getString("injDate", null);
        if (injDate != null) {
            try {
                Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).parse(injDate);
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0);
                long days = Math.round((d.getTime() - today.getTimeInMillis()) / (double) TimeUnit.DAYS.toMillis(1));
                String when = new SimpleDateFormat("EEE d MMM", Locale.FRANCE).format(d);
                inj = days < 0 ? "Injection en retard (" + when + ")" : days == 0 ? "Injection aujourd'hui" : days == 1 ? "Injection demain" : "Injection dans " + days + " j · " + when;
            } catch (Exception ignored) { }
        }
        v.setTextViewText(R.id.w_inj, inj);
        String b = p.getString("basdai", null);
        v.setTextViewText(R.id.w_basdai, b != null ? "BASDAI " + b + " (" + p.getString("basdaiDate", "") + ")" : "BASDAI : —");
        String s = p.getString("sleep", null), st = p.getString("stock", null), fl = p.getString("flare", null);
        StringBuilder line = new StringBuilder();
        if (fl != null) line.append(fl);
        if (s != null) { if (line.length() > 0) line.append(" · "); line.append("Nuit ").append(s); }
        if (st != null) { if (line.length() > 0) line.append(" · "); line.append(st); }
        v.setTextViewText(R.id.w_line, line.length() > 0 ? line.toString() : "Touche pour ouvrir Health K'");

        v.setOnClickPendingIntent(R.id.w_root, open(ctx, "home", 1));
        v.setOnClickPendingIntent(R.id.w_btn_basdai, open(ctx, "express", 2));
        v.setOnClickPendingIntent(R.id.w_btn_morning, open(ctx, "morning", 3));
        v.setOnClickPendingIntent(R.id.w_btn_temp, open(ctx, "temp", 4));
        m.updateAppWidget(id, v);
    }
}

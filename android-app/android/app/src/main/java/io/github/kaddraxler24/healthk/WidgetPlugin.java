package io.github.kaddraxler24.healthk;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Reçoit de l'appli les infos à afficher dans le widget de l'écran d'accueil. */
@CapacitorPlugin(name = "HealthKWidget")
public class WidgetPlugin extends Plugin {

    @PluginMethod
    public void update(PluginCall call) {
        Context ctx = getContext();
        SharedPreferences.Editor e = ctx.getSharedPreferences(HealthKWidget.PREFS, Context.MODE_PRIVATE).edit();
        String[] keys = { "injDate", "basdai", "basdaiDate", "sleep", "stock", "flare" };
        for (String k : keys) {
            String v = call.getString(k);
            if (v == null) e.remove(k); else e.putString(k, v);
        }
        e.apply();
        HealthKWidget.refreshAll(ctx);
        call.resolve();
    }
}

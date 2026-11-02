package io.github.kaddraxler24.healthk;

import android.content.Context;
import android.content.SharedPreferences;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONObject;

/** Lien entre l'appli et la montre (notifications interactives gérées par WatchFlow). */
@CapacitorPlugin(name = "HealthKWatch")
public class WatchPlugin extends Plugin {

    /** Réglages et données utiles à la montre (horaires, évaluations déjà faites, injection…). */
    @PluginMethod
    public void configure(PluginCall call) {
        Context ctx = getContext();
        JSObject cfg = call.getData();
        WatchFlow.prefs(ctx).edit().putString("cfg", cfg.toString()).apply();
        WatchFlow.scheduleAll(ctx);
        if (!cfg.optBoolean("on", false)) {
            androidx.core.app.NotificationManagerCompat nm = androidx.core.app.NotificationManagerCompat.from(ctx);
            nm.cancel(WatchFlow.ID_HUB); nm.cancel(WatchFlow.ID_Q); nm.cancel(WatchFlow.ID_MORNING);
        } else WatchFlow.refreshHubIfShown(ctx);
        JSObject r = new JSObject(); r.put("ok", true); r.put("version", 1);
        call.resolve(r);
    }

    /** Réponses données sur la montre depuis la dernière ouverture ; elles sont effacées une fois reprises. */
    @PluginMethod
    public void takePending(PluginCall call) {
        SharedPreferences p = WatchFlow.prefs(getContext());
        String s = p.getString("pending", null);
        p.edit().remove("pending").apply();
        JSObject r = new JSObject();
        try { r = JSObject.fromJSONObject(new JSONObject(s == null ? "{}" : s)); } catch (Exception ignored) {}
        call.resolve(r);
    }

    /** Envoie tout de suite le menu Health K' sur la montre. */
    @PluginMethod
    public void showHub(PluginCall call) {
        try { WatchFlow.postHub(getContext(), false); call.resolve(); } catch (Exception e) { call.reject(e.getMessage()); }
    }

    /** Lance l'évaluation BASDAI sur la montre (question 1). */
    @PluginMethod
    public void startBasdai(PluginCall call) {
        try { WatchFlow.startBasdai(getContext(), true); call.resolve(); } catch (Exception e) { call.reject(e.getMessage()); }
    }
}

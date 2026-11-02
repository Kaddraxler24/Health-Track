package io.github.kaddraxler24.healthk;

import android.net.Uri;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

/** Reçoit les réponses de l'appli Health K' de la montre (BASDAI, raideur, température, injection). */
public class WearListener extends WearableListenerService {

    @Override
    public void onDataChanged(DataEventBuffer events) {
        List<String> got = new ArrayList<>();
        List<Uri> done = new ArrayList<>();
        for (DataEvent ev : events) {
            if (ev.getType() != DataEvent.TYPE_CHANGED) continue;
            DataItem item = ev.getDataItem();
            Uri uri = item.getUri();
            String path = uri.getPath();
            if (path == null || !path.startsWith("/hk/out/")) continue;
            try {
                String json = DataMapItem.fromDataItem(item).getDataMap().getString("json");
                if (json != null) { String r = WatchFlow.applyFromWatch(this, new JSONObject(json)); if (r != null) got.add(r); }
            } catch (Exception e) { android.util.Log.w("HealthK", "montre", e); }
            done.add(uri);
        }
        for (Uri u : done) Wearable.getDataClient(this).deleteDataItems(u);   // la montre sait ainsi que c'est bien arrivé
        if (!done.isEmpty()) {
            WatchFlow.prefs(this).edit().putLong("wearSeen", System.currentTimeMillis()).apply();
            WearSync.pushState(this);
            WatchPlugin.firePending();
        }
    }

    @Override
    public void onMessageReceived(MessageEvent ev) {
        if ("/hk/hello".equals(ev.getPath())) {          // la montre vient d'être installée ou rouverte
            WatchFlow.prefs(this).edit().putLong("wearSeen", System.currentTimeMillis()).apply();
            WearSync.pushState(this);
        }
    }
}

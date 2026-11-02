package io.github.kaddraxler24.healthk.wear;

import android.net.Uri;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/** Reçoit l'état envoyé par le téléphone, les accusés de réception, et les demandes d'ouverture. */
public class WearListener extends WearableListenerService {

    @Override
    public void onDataChanged(DataEventBuffer events) {
        for (DataEvent ev : events) {
            Uri u = ev.getDataItem().getUri();
            String path = u.getPath();
            if (path == null) continue;
            if (ev.getType() == DataEvent.TYPE_CHANGED && path.equals("/hk/state")) {
                String json = DataMapItem.fromDataItem(ev.getDataItem()).getDataMap().getString("json");
                if (json != null) Store.setState(this, json);
            } else if (ev.getType() == DataEvent.TYPE_DELETED && path.startsWith("/hk/out/")) {
                Store.acked(this, path.substring("/hk/out/".length()));
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent ev) {
        String p = ev.getPath();
        if ("/hk/open/basdai".equals(p)) {
            if (Store.doneScore(this, Store.basdaiDay()) >= 0) Reminders.post(this, Reminders.N_OPEN, "BASDAI déjà fait aujourd'hui", "Touche pour voir le résultat.", BasdaiActivity.class);
            else Reminders.post(this, Reminders.N_OPEN, "Évaluation BASDAI", "Touche pour commencer (6 questions).", BasdaiActivity.class);
        } else if ("/hk/open/hub".equals(p)) {
            Reminders.post(this, Reminders.N_OPEN, "Health K'", "Touche pour ouvrir le menu.", MainActivity.class);
        }
    }
}

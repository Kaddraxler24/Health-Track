package io.github.kaddraxler24.healthk;

import android.content.Context;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

/** Envoie à l'appli Health K' de la montre (Wear OS) l'état du jour : horaires, BASDAI faits, raideur, injection. */
public class WearSync {

    static void pushState(Context ctx) {
        try {
            PutDataMapRequest r = PutDataMapRequest.create("/hk/state");
            r.getDataMap().putString("json", WatchFlow.stateForWatch(ctx).toString());
            r.getDataMap().putLong("t", System.currentTimeMillis());
            Wearable.getDataClient(ctx).putDataItem(r.asPutDataRequest().setUrgent());
        } catch (Throwable e) { /* pas de montre ou pas de services Google : rien à faire */ }
    }

    /** Demande à la montre d'afficher un écran (« basdai » ou « hub »). */
    static void open(Context ctx, String what) {
        try {
            Wearable.getNodeClient(ctx).getConnectedNodes().addOnSuccessListener(nodes -> {
                for (Node n : nodes) Wearable.getMessageClient(ctx).sendMessage(n.getId(), "/hk/open/" + what, new byte[0]);
            });
        } catch (Throwable ignored) {}
    }
}

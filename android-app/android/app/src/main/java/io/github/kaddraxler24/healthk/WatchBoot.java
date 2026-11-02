package io.github.kaddraxler24.healthk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Après un redémarrage du téléphone ou une mise à jour de l'appli : reprogramme les questions de la montre. */
public class WatchBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent in) {
        String a = in.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) WatchFlow.scheduleAll(ctx);
    }
}

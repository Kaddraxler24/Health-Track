package io.github.kaddraxler24.healthk;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(WidgetPlugin.class);   // widget de l'écran d'accueil
        registerPlugin(WatchPlugin.class);    // questions BASDAI sur la montre
        super.onCreate(savedInstanceState);
    }
}

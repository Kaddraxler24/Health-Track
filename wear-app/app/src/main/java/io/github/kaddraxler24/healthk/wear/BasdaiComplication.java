package io.github.kaddraxler24.healthk.wear;

import android.app.PendingIntent;
import android.content.Intent;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService;
import androidx.wear.watchface.complications.datasource.ComplicationRequest;

/** Complication du cadran : BASDAI du jour (ou « à faire »). Toucher ouvre l'évaluation. */
public class BasdaiComplication extends ComplicationDataSourceService {

    static ShortTextComplicationData data(android.content.Context c, String value, String title, String desc, Class<?> cls) {
        PendingIntent pi = PendingIntent.getActivity(c, 200 + cls.getSimpleName().length(),
            new Intent(c, cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new ShortTextComplicationData.Builder(
                new PlainComplicationText.Builder(value).build(),
                new PlainComplicationText.Builder(desc).build())
            .setTitle(new PlainComplicationText.Builder(title).build())
            .setTapAction(pi)
            .build();
    }

    @Override
    public void onComplicationRequest(ComplicationRequest request, ComplicationRequestListener listener) {
        double s = Store.doneScore(this, Store.basdaiDay());
        ComplicationData d = s >= 0 ? data(this, Store.fr1(s), "BASDAI", "BASDAI du jour " + Store.fr1(s), BasdaiActivity.class)
                                    : data(this, "à faire", "BASDAI", "BASDAI du jour à faire", BasdaiActivity.class);
        try { listener.onComplicationData(d); } catch (Exception ignored) {}
    }

    @Override
    public ComplicationData getPreviewData(ComplicationType type) {
        return type == ComplicationType.SHORT_TEXT ? data(this, "3,2", "BASDAI", "BASDAI du jour", BasdaiActivity.class) : null;
    }
}

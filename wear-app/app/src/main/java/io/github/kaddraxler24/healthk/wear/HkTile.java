package io.github.kaddraxler24.healthk.wear;

import androidx.wear.protolayout.ActionBuilders;
import androidx.wear.protolayout.ColorBuilders;
import androidx.wear.protolayout.DimensionBuilders;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.ModifiersBuilders;
import androidx.wear.protolayout.ResourceBuilders;
import androidx.wear.protolayout.TimelineBuilders;
import androidx.wear.tiles.RequestBuilders;
import androidx.wear.tiles.TileBuilders;
import androidx.wear.tiles.TileService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/** Tuile Health K' : BASDAI du jour, prochaine injection, bouton pour faire le BASDAI. */
public class HkTile extends TileService {

    static final String RES = "1";

    private LayoutElementBuilders.LayoutElement text(String s, float sp, int color, boolean bold) {
        return new LayoutElementBuilders.Text.Builder()
            .setText(s)
            .setMaxLines(2)
            .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                .setSize(DimensionBuilders.sp(sp))
                .setColor(ColorBuilders.argb(color))
                .setWeight(bold ? LayoutElementBuilders.FONT_WEIGHT_BOLD : LayoutElementBuilders.FONT_WEIGHT_NORMAL)
                .build())
            .build();
    }

    private ModifiersBuilders.Clickable launch(String id, Class<?> cls) {
        return new ModifiersBuilders.Clickable.Builder()
            .setId(id)
            .setOnClick(new ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(new ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(getPackageName())
                    .setClassName(cls.getName())
                    .build())
                .build())
            .build();
    }

    private LayoutElementBuilders.LayoutElement chip(String label, int bg, ModifiersBuilders.Clickable click) {
        return new LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.dp(130))
            .setHeight(DimensionBuilders.dp(40))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setModifiers(new ModifiersBuilders.Modifiers.Builder()
                .setBackground(new ModifiersBuilders.Background.Builder()
                    .setColor(ColorBuilders.argb(bg))
                    .setCorner(new ModifiersBuilders.Corner.Builder().setRadius(DimensionBuilders.dp(20)).build())
                    .build())
                .setClickable(click)
                .build())
            .addContent(text(label, 14, 0xFFFFFFFF, true))
            .build();
    }

    private LayoutElementBuilders.LayoutElement gap(float h) {
        return new LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(h)).build();
    }

    @Override
    protected ListenableFuture<TileBuilders.Tile> onTileRequest(RequestBuilders.TileRequest req) {
        double s = Store.doneScore(this, Store.basdaiDay());
        String inj = Store.injLine(this);
        LayoutElementBuilders.Column.Builder col = new LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(new ModifiersBuilders.Modifiers.Builder().setClickable(launch("menu", MainActivity.class)).build())
            .addContent(text("Health K'", 13, 0xFF34D3C3, true))
            .addContent(gap(4))
            .addContent(text("BASDAI du jour", 12, 0xFFA9BDBA, false))
            .addContent(s >= 0 ? text(Store.fr1(s), 34, Store.color(s), true) : text("à faire", 22, 0xFFFFFFFF, true));
        if (inj != null) col.addContent(text(inj, 12, 0xFFB7A8FF, false));
        org.json.JSONObject avg = Store.hist(this).optJSONObject("avg");
        if (avg != null && avg.has("basdai") && !avg.isNull("basdai"))
            col.addContent(text("Moyenne 10 j : " + Store.fr1(avg.optDouble("basdai")), 11, 0xFF8FA5A2, false));
        col.addContent(gap(8));
        col.addContent(s >= 0 ? chip("Raideur · Temp.", 0xFF22302F, launch("menu2", MainActivity.class))
                              : chip("Faire le BASDAI", 0xFF0F9F9A, launch("basdai", BasdaiActivity.class)));
        LayoutElementBuilders.Box root = new LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand()).setHeight(DimensionBuilders.expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .addContent(col.build())
            .build();
        TileBuilders.Tile tile = new TileBuilders.Tile.Builder()
            .setResourcesVersion(RES)
            .setFreshnessIntervalMillis(30 * 60 * 1000L)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(root))
            .build();
        return Futures.immediateFuture(tile);
    }

    @Override
    protected ListenableFuture<ResourceBuilders.Resources> onTileResourcesRequest(RequestBuilders.ResourcesRequest req) {
        return Futures.immediateFuture(new ResourceBuilders.Resources.Builder().setVersion(RES).build());
    }
}

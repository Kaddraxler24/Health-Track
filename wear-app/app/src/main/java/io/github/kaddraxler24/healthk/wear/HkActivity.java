package io.github.kaddraxler24.healthk.wear;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

/** Base des écrans : fond noir, cartes arrondies, boutons « pilule », molette / lunette rotative. */
public abstract class HkActivity extends Activity {

    static final int MINT = 0xFF34D3C3, TEAL = 0xFF0F9F9A, CARD = 0xFF111A1A, GREY = 0xFF1E2A29, T2 = 0xFF8FA5A2, WHITE = 0xFFFFFFFF,
        VIOLET = 0xFF9D8CFF, ORANGE = 0xFFF0A030, CORAL = 0xFFF2685E, GREEN = 0xFF22C17A, BLUE = 0xFF5AB0F0;
    protected LinearLayout col;
    protected View root;
    private float rotAcc = 0;

    protected int dp(float v) { return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics())); }
    protected int side() { return Math.round(getResources().getDisplayMetrics().widthPixels * 0.11f); }

    /** Colonne centrée ; scroll = true pour les écrans plus longs que l'écran. */
    protected void page(boolean scroll) {
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        if (scroll) {
            col.setPadding(side(), dp(14), side(), dp(46));
            ScrollView sv = new ScrollView(this);
            sv.setFillViewport(true);
            sv.setVerticalScrollBarEnabled(true);
            sv.addView(col, new ViewGroup.LayoutParams(-1, -2));
            root = sv;
        } else {
            col.setPadding(side(), dp(6), side(), dp(6));
            col.setGravity(Gravity.CENTER);
            root = col;
        }
        root.setBackgroundColor(0xFF000000);
        root.setFocusable(true);
        root.setFocusableInTouchMode(true);
        setContentView(root);
        root.requestFocus();
    }

    /** Heure en haut, comme dans les applis Wear OS. */
    protected void clock() {
        TextClock tc = new TextClock(this);
        tc.setFormat24Hour("HH:mm"); tc.setFormat12Hour("HH:mm");
        tc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tc.setTextColor(WHITE);
        tc.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tc.setGravity(Gravity.CENTER);
        col.addView(tc, new LinearLayout.LayoutParams(-1, -2));
    }

    // ─────────────── Textes ───────────────
    protected TextView text(ViewGroup parent, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        t.setLineSpacing(0, 1.1f);
        t.setTypeface(Typeface.create(bold ? "sans-serif" : "sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        parent.addView(t, new LinearLayout.LayoutParams(-1, -2));
        return t;
    }
    protected TextView text(String s, float sp, int color, boolean bold) { return text(col, s, sp, color, bold); }

    /** Petit titre en capitales espacées. */
    protected TextView label(ViewGroup parent, String s, int color) {
        TextView t = text(parent, s.toUpperCase(java.util.Locale.FRENCH), 10, color, true);
        t.setLetterSpacing(0.08f);
        return t;
    }

    protected void space(ViewGroup parent, int h) { parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(h))); }
    protected void space(int h) { space(col, h); }

    protected GradientDrawable shape(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(radiusDp));
        return g;
    }

    // ─────────────── Cartes ───────────────
    protected LinearLayout card(ViewGroup parent, View.OnClickListener tap) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(dp(12), dp(11), dp(12), dp(12));
        c.setBackground(shape(CARD, 22));
        if (tap != null) { c.setClickable(true); c.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); tap.onClick(v); }); c.setForeground(getDrawable(android.R.drawable.list_selector_background)); }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(8);
        parent.addView(c, lp);
        return c;
    }
    protected LinearLayout card(View.OnClickListener tap) { return card(col, tap); }

    /** Ligne de petites statistiques (valeur + légende), réparties à parts égales. */
    protected LinearLayout statRow(ViewGroup parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(8);
        parent.addView(row, lp);
        return row;
    }
    protected void stat(LinearLayout row, String value, String caption, int color) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        TextView v = text(cell, value, 14, color, true);
        v.setMaxLines(1); v.setEllipsize(TextUtils.TruncateAt.END);
        TextView c = text(cell, caption, 9, T2, false);
        c.setMaxLines(1);
        row.addView(cell, new LinearLayout.LayoutParams(0, -2, 1));
    }

    /** Pastille colorée (J-2, Rhumato…). */
    protected TextView pill(ViewGroup parent, String s, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        t.setTextColor(color);
        t.setPadding(dp(8), dp(2), dp(8), dp(3));
        t.setBackground(shape((color & 0x00FFFFFF) | 0x2E000000, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        parent.addView(t, lp);
        return t;
    }

    // ─────────────── Boutons ───────────────
    protected TextView button(ViewGroup parent, String s, int bg, int fg, View.OnClickListener l) {
        TextView b = new TextView(this);
        b.setText(s);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setTextColor(fg);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(42));
        b.setPadding(dp(12), dp(4), dp(12), dp(4));
        b.setBackground(shape(bg, 24));
        b.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); if (l != null) l.onClick(v); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(6);
        parent.addView(b, lp);
        return b;
    }
    protected TextView button(String s, int bg, int fg, View.OnClickListener l) { return button(col, s, bg, fg, l); }

    /** Bouton rond avec icône et légende (actions rapides). */
    protected void action(LinearLayout row, int icon, int color, String caption, View.OnClickListener l) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        iv.setColorFilter(color);
        iv.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable g = new GradientDrawable(); g.setShape(GradientDrawable.OVAL); g.setColor((color & 0x00FFFFFF) | 0x26000000);
        iv.setBackground(g);
        cell.addView(iv, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView t = text(cell, caption, 10, T2, false);
        ((LinearLayout.LayoutParams) t.getLayoutParams()).topMargin = dp(3);
        cell.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); l.onClick(v); });
        row.addView(cell, new LinearLayout.LayoutParams(0, -2, 1));
    }

    protected TextView round(String s, View.OnClickListener l) {
        TextView b = new TextView(this);
        b.setText(s);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        b.setTextColor(WHITE);
        b.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(GREY); g.setShape(GradientDrawable.OVAL);
        b.setBackground(g);
        b.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); l.onClick(v); });
        return b;
    }

    /** Ligne « − anneau + » : la valeur est affichée dans un anneau qui se remplit. */
    protected Widgets.Ring dial(View.OnClickListener minus, View.OnClickListener plus, int sizeDp) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        Widgets.Ring ring = new Widgets.Ring(this, 6);
        row.addView(round("−", minus), new LinearLayout.LayoutParams(dp(36), dp(36)));
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp));
        rl.leftMargin = dp(8); rl.rightMargin = dp(8);
        row.addView(ring, rl);
        row.addView(round("+", plus), new LinearLayout.LayoutParams(dp(36), dp(36)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(4);
        col.addView(row, lp);
        return ring;
    }

    /** Petits points de progression (questions). */
    protected void dots(int n, int current) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        for (int i = 1; i <= n; i++) {
            View d = new View(this);
            GradientDrawable g = new GradientDrawable(); g.setCornerRadius(dp(3));
            g.setColor(i < current ? TEAL : i == current ? MINT : GREY);
            d.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(i == current ? 14 : 6), dp(6));
            lp.leftMargin = dp(2); lp.rightMargin = dp(2);
            row.addView(d, lp);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(5);
        col.addView(row, lp);
    }

    /** Molette (Pixel Watch) ou lunette rotative (Galaxy Watch) : +1 / −1 par cran. */
    protected void onRotary(int step) {}

    @Override
    public boolean onGenericMotionEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_SCROLL && ev.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
            float px = -ev.getAxisValue(MotionEvent.AXIS_SCROLL) * ViewConfiguration.get(this).getScaledVerticalScrollFactor();
            rotAcc += px;
            float th = dp(26);
            boolean moved = false;
            while (rotAcc >= th) { rotAcc -= th; onRotary(1); moved = true; }
            while (rotAcc <= -th) { rotAcc += th; onRotary(-1); moved = true; }
            if (moved && root != null) root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            return true;
        }
        return super.onGenericMotionEvent(ev);
    }

    @Override
    protected void onResume() { super.onResume(); if (root != null) root.requestFocus(); }
}

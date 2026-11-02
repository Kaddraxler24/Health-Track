package io.github.kaddraxler24.healthk.wear;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Base des écrans : fond noir (économie d'énergie), textes et boutons arrondis, molette / lunette rotative. */
public abstract class HkActivity extends Activity {

    static final int MINT = 0xFF34D3C3, TEAL = 0xFF0F9F9A, GREY = 0xFF22302F, T2 = 0xFFA9BDBA, WHITE = 0xFFFFFFFF;
    protected LinearLayout col;
    protected View root;
    private float rotAcc = 0;

    protected int dp(float v) { return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics())); }

    /** Colonne centrée ; scroll = true pour les écrans plus longs que l'écran (menu). */
    protected void page(boolean scroll) {
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        int side = Math.round(getResources().getDisplayMetrics().widthPixels * 0.12f);   // marge pour l'écran rond
        if (scroll) {
            col.setPadding(side, dp(22), side, dp(40));
            ScrollView sv = new ScrollView(this);
            sv.setFillViewport(true);
            sv.setVerticalScrollBarEnabled(false);
            sv.addView(col, new ViewGroup.LayoutParams(-1, -2));
            root = sv;
        } else {
            col.setPadding(side, dp(8), side, dp(8));
            col.setGravity(Gravity.CENTER);
            root = col;
        }
        root.setBackgroundColor(0xFF000000);
        root.setFocusable(true);
        root.setFocusableInTouchMode(true);
        setContentView(root);
        root.requestFocus();
    }

    protected TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        col.addView(t, new LinearLayout.LayoutParams(-1, -2));
        return t;
    }

    protected TextView button(String s, int bg, int fg, View.OnClickListener l) {
        TextView b = new TextView(this);
        b.setText(s);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setTextColor(fg);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(44));
        b.setPadding(dp(12), dp(6), dp(12), dp(6));
        GradientDrawable g = new GradientDrawable();
        g.setColor(bg); g.setCornerRadius(dp(24));
        b.setBackground(g);
        b.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); l.onClick(v); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(6);
        col.addView(b, lp);
        return b;
    }

    protected TextView round(String s, View.OnClickListener l) {
        TextView b = new TextView(this);
        b.setText(s);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        b.setTextColor(WHITE);
        b.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setColor(GREY); g.setShape(GradientDrawable.OVAL);
        b.setBackground(g);
        b.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); l.onClick(v); });
        return b;
    }

    protected void space(int h) { col.addView(new View(this), new LinearLayout.LayoutParams(1, dp(h))); }

    /** Ligne « − valeur + ». */
    protected TextView stepper(View.OnClickListener minus, View.OnClickListener plus) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        TextView m = round("−", minus), p = round("+", plus);
        TextView v = new TextView(this);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        v.setTextColor(WHITE);
        v.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        v.setGravity(Gravity.CENTER);
        row.addView(m, new LinearLayout.LayoutParams(dp(42), dp(42)));
        row.addView(v, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(p, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(4);
        col.addView(row, lp);
        return v;
    }

    /** Molette (Pixel Watch) ou lunette rotative (Galaxy Watch) : +1 / −1 par cran. */
    protected void onRotary(int step) {}

    @Override
    public boolean onGenericMotionEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_SCROLL && ev.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
            float px = -ev.getAxisValue(MotionEvent.AXIS_SCROLL) * ViewConfiguration.get(this).getScaledVerticalScrollFactor();
            rotAcc += px;
            float th = dp(28);
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

    @Override
    protected void onCreate(Bundle b) { super.onCreate(b); }
}

package io.github.kaddraxler24.healthk.wear;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** Petits composants dessinés : anneau de score, barres d'historique, barre de progression. */
final class Widgets {
    private Widgets() {}

    static float dp(Context c, float v) { return v * c.getResources().getDisplayMetrics().density; }

    /** Anneau : piste grise + arc coloré (0 à 1), texte au centre et petite légende dessous. */
    static class Ring extends View {
        float frac = 0; int color = 0xFF34D3C3; String value = "—", caption = ""; float valueSp = 30, stroke;
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG), arc = new Paint(Paint.ANTI_ALIAS_FLAG), txt = new Paint(Paint.ANTI_ALIAS_FLAG), cap = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        Ring(Context c, float strokeDp) {
            super(c);
            stroke = dp(c, strokeDp);
            track.setStyle(Paint.Style.STROKE); track.setStrokeWidth(stroke); track.setColor(0xFF185159);
            arc.setStyle(Paint.Style.STROKE); arc.setStrokeWidth(stroke); arc.setStrokeCap(Paint.Cap.ROUND);
            txt.setTextAlign(Paint.Align.CENTER); txt.setTypeface(Typeface.create("sans-serif", Typeface.BOLD)); txt.setColor(0xFFFFFFFF);
            cap.setTextAlign(Paint.Align.CENTER); cap.setColor(0xFF9CC7C1); cap.setLetterSpacing(0.08f);
        }

        void set(float f, int col, String v, String c) { frac = Math.max(0, Math.min(1, f)); color = col; value = v; caption = c; invalidate(); }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight(), s = Math.min(w, h), pad = stroke / 2 + 1;
            r.set((w - s) / 2 + pad, (h - s) / 2 + pad, (w + s) / 2 - pad, (h + s) / 2 - pad);
            cv.drawArc(r, 0, 360, false, track);
            if (frac > 0) { arc.setColor(color); cv.drawArc(r, -90, Math.max(4, 360 * frac), false, arc); }
            float vs = s * (valueSp / 100f);
            txt.setTextSize(vs);
            txt.setColor(frac > 0 ? color : 0xFFFFFFFF);
            float cy = h / 2 + vs * 0.34f - (caption.isEmpty() ? 0 : s * 0.05f);
            cv.drawText(value, w / 2, cy, txt);
            if (!caption.isEmpty()) { cap.setTextSize(s * 0.095f); cv.drawText(caption, w / 2, cy + s * 0.15f, cap); }
        }
    }

    /** Barres des derniers BASDAI (vert < 4, orange 4–6, rouge > 6) avec la ligne du seuil 4. */
    static class Bars extends View {
        double[] v = new double[0]; boolean[] local = new boolean[0];
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG), line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        Bars(Context c) {
            super(c);
            line.setColor(0x66FFFFFF); line.setStrokeWidth(dp(c, 1)); line.setStyle(Paint.Style.STROKE);
            line.setPathEffect(new DashPathEffect(new float[] { dp(c, 3), dp(c, 3) }, 0));
        }

        void set(double[] values) { v = values; invalidate(); }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight();
            if (v.length == 0) return;
            float gap = dp(getContext(), 3), bw = Math.min(dp(getContext(), 12), (w - gap * (v.length - 1)) / v.length);
            float total = bw * v.length + gap * (v.length - 1), x = (w - total) / 2;
            for (double b : v) {
                float bh = Math.max(dp(getContext(), 3), (float) (h * Math.min(10, b) / 10));
                p.setColor(Store.color(b));
                r.set(x, h - bh, x + bw, h);
                cv.drawRoundRect(r, bw / 2, bw / 2, p);
                x += bw + gap;
            }
            float y = h - h * 0.4f;
            cv.drawLine(0, y, w, y, line);
        }
    }

    /** Barre de progression arrondie. */
    static class Progress extends View {
        float frac = 0; int color = 0xFFB3A4FF;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        Progress(Context c) { super(c); }
        void set(float f, int col) { frac = Math.max(0, Math.min(1, f)); color = col; invalidate(); }
        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight();
            p.setColor(0xFF185159); r.set(0, 0, w, h); cv.drawRoundRect(r, h / 2, h / 2, p);
            if (frac > 0) { p.setColor(color); r.set(0, 0, Math.max(h, w * frac), h); cv.drawRoundRect(r, h / 2, h / 2, p); }
        }
    }
}

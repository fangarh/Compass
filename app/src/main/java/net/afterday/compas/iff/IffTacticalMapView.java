package net.afterday.compas.iff;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public final class IffTacticalMapView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<MapPoint> points = new ArrayList<>();
    private String localLabel = "";

    public IffTacticalMapView(Context context) {
        super(context);
        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE,
                android.graphics.Typeface.BOLD));
        textPaint.setTextSize(sp(11));
    }

    public void setState(String nextLocalLabel, List<MapPoint> nextPoints) {
        localLabel = nextLocalLabel == null ? "" : nextLocalLabel;
        points.clear();
        if (nextPoints != null) {
            points.addAll(nextPoints);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        drawBackground(canvas, width, height);
        drawFrame(canvas, width, height);
        drawLocalNode(canvas, width, height);
        drawPoints(canvas, width, height);
        drawLegend(canvas, width, height);
    }

    private void drawBackground(Canvas canvas, int width, int height) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xff08100b);
        canvas.drawRect(0, 0, width, height, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(0xff20301f);
        int step = Math.max(dp(28), width / 12);
        for (int x = 0; x < width; x += step) {
            canvas.drawLine(x, 0, x, height, paint);
        }
        for (int y = 0; y < height; y += step) {
            canvas.drawLine(0, y, width, y, paint);
        }
    }

    private void drawFrame(Canvas canvas, int width, int height) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(0xff6f7d42);
        canvas.drawRect(dp(2), dp(2), width - dp(2), height - dp(2), paint);

        textPaint.setColor(0xffffd16a);
        canvas.drawText("IFF TACTICAL MAP MOCK", dp(12), dp(22), textPaint);
        textPaint.setColor(0xffb8c49a);
        canvas.drawText("NO GPS POSITION / NO BEARING", dp(12), dp(40), textPaint);
    }

    private void drawLocalNode(Canvas canvas, int width, int height) {
        float cx = width * 0.5f;
        float cy = height * 0.54f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(0xff405037);
        canvas.drawCircle(cx, cy, Math.min(width, height) * 0.18f, paint);
        canvas.drawCircle(cx, cy, Math.min(width, height) * 0.31f, paint);
        canvas.drawCircle(cx, cy, Math.min(width, height) * 0.43f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xffffd16a);
        canvas.drawCircle(cx, cy, dp(11), paint);
        textPaint.setColor(0xffffffff);
        canvas.drawText("THIS DEVICE: " + localLabel, cx + dp(16), cy + dp(4), textPaint);
    }

    private void drawPoints(Canvas canvas, int width, int height) {
        float cx = width * 0.5f;
        float cy = height * 0.54f;
        float[][] slots = new float[][] {
                {-0.62f, -0.30f},
                {0.56f, -0.32f},
                {-0.52f, 0.42f},
                {0.50f, 0.44f}
        };
        float radius = Math.min(width, height) * 0.42f;

        for (int i = 0; i < points.size(); i++) {
            MapPoint point = points.get(i);
            float[] slot = slots[i % slots.length];
            float x = cx + slot[0] * radius;
            float y = cy + slot[1] * radius;
            int color = colorFor(point);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawCircle(x, y, point.localDevice ? dp(10) : dp(8), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(point.selected ? 0xffffd16a : 0xff20301f);
            canvas.drawCircle(x, y, dp(14), paint);

            textPaint.setColor(0xffffffff);
            canvas.drawText(point.name, x + dp(18), y - dp(3), textPaint);
            textPaint.setColor(0xffb8c49a);
            canvas.drawText(point.radioLabel, x + dp(18), y + dp(14), textPaint);
        }
    }

    private void drawLegend(Canvas canvas, int width, int height) {
        int y = height - dp(36);
        textPaint.setColor(0xffb8c49a);
        canvas.drawText("Slots are roster order, not direction", dp(12), y, textPaint);
        canvas.drawText("green=fresh  amber=stale  gray=unknown", dp(12), y + dp(17), textPaint);
    }

    private int colorFor(MapPoint point) {
        if (point.current) {
            return 0xff7dff73;
        }
        if (point.stale) {
            return 0xffffd16a;
        }
        if (point.localDevice) {
            return 0xffd8dfca;
        }
        return 0xff6f7869;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private float sp(int value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    public static final class MapPoint {
        public final String name;
        public final String radioLabel;
        public final boolean localDevice;
        public final boolean selected;
        public final boolean current;
        public final boolean stale;

        public MapPoint(String name, String radioLabel, boolean localDevice, boolean selected,
                        boolean current, boolean stale) {
            this.name = name;
            this.radioLabel = radioLabel;
            this.localDevice = localDevice;
            this.selected = selected;
            this.current = current;
            this.stale = stale;
        }
    }
}

package net.afterday.compas.iff;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public final class IffTacticalMapView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<MapPoint> points = new ArrayList<>();
    private String localLabel = "";
    private IffFieldMapSnapshot fieldState;
    private int detailMode;

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

    public void setFieldState(IffFieldMapSnapshot nextFieldState) {
        fieldState = nextFieldState;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) {
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            detailMode = (detailMode + 1) % 3;
            invalidate();
            return true;
        }
        return true;
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
        drawFieldGeometry(canvas, width, height);
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
        canvas.drawText("IFF FIELD MAP", dp(12), dp(22), textPaint);
        textPaint.setColor(0xffb8c49a);
        canvas.drawText(fieldState == null ? "NO FIELD STATE" : fieldState.statusLine, dp(12), dp(40), textPaint);
    }

    private void drawFieldGeometry(Canvas canvas, int width, int height) {
        float cx = width * 0.5f;
        float cy = height * 0.64f;
        float scale = Math.min(width, height);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(0xff405037);
        canvas.drawCircle(cx, cy, scale * 0.16f, paint);
        canvas.drawCircle(cx, cy, scale * 0.24f, paint);
        canvas.drawCircle(cx, cy, scale * 0.32f, paint);
        canvas.drawCircle(cx, cy, scale * 0.40f, paint);

        float leftX = width * 0.22f;
        float rightX = width * 0.78f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xff5ea8ff);
        canvas.drawCircle(leftX, cy, dp(10), paint);
        canvas.drawCircle(rightX, cy, dp(10), paint);
        textPaint.setColor(0xffffffff);
        canvas.drawText("VASYA", leftX - dp(28), cy + dp(26), textPaint);
        canvas.drawText("PETYA", rightX - dp(28), cy + dp(26), textPaint);

        if (fieldState != null && fieldState.targetVisible) {
            drawTargetEstimate(canvas, width, height, cx, cy, scale);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xffd8dfca);
        canvas.drawCircle(cx, cy, dp(4), paint);
    }

    private void drawTargetEstimate(Canvas canvas, int width, int height, float cx, float cy, float scale) {
        float tx = width * fieldState.targetX;
        float ty = height * fieldState.targetY;
        float ringRadius = radiusFor(fieldState.distanceBucketM, scale);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(fieldState.directionKnown ? 0xff7dff73 : 0xffffd16a);
        canvas.drawCircle(cx, cy, ringRadius, paint);

        if (fieldState.directionKnown) {
            float sweepRadius = Math.max(dp(36), ringRadius);
            RectF arc = new RectF(cx - sweepRadius, cy - sweepRadius, cx + sweepRadius, cy + sweepRadius);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(18));
            paint.setColor(0x667dff73);
            canvas.drawArc(arc, startAngleForClock(fieldState.clockDirection), 38.0f, false, paint);
            paint.setStrokeWidth(dp(2));
            paint.setColor(0xff7dff73);
            canvas.drawLine(cx, cy, tx, ty, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fieldState.directionKnown ? 0xff7dff73 : 0xffffd16a);
        canvas.drawCircle(tx, ty, dp(12), paint);
        textPaint.setColor(0xffffffff);
        canvas.drawText("ZHENYA", tx + dp(16), ty - dp(4), textPaint);
        textPaint.setColor(0xffb8c49a);
        canvas.drawText(fieldState.distanceBucketM + "m / " + fieldState.clockDirection, tx + dp(16), ty + dp(13), textPaint);
    }

    private void drawPoints(Canvas canvas, int width, int height) {
        if (detailMode == 0) {
            return;
        }
        float cx = width * 0.5f;
        float cy = height * 0.64f;
        float[][] slots = new float[][] {
                {-0.70f, -0.52f},
                {0.48f, -0.52f},
                {-0.70f, 0.20f},
                {0.48f, 0.20f}
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
        int y = height - dp(52);
        textPaint.setColor(0xffb8c49a);
        canvas.drawText("tap: map / details / roster", dp(12), y, textPaint);
        canvas.drawText("green=two anchors  amber=one anchor/fallback", dp(12), y + dp(17), textPaint);
        canvas.drawText("local: " + localLabel, dp(12), y + dp(34), textPaint);
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

    private float radiusFor(int distanceBucketM, float scale) {
        if (distanceBucketM <= 5) {
            return scale * 0.16f;
        }
        if (distanceBucketM <= 10) {
            return scale * 0.24f;
        }
        if (distanceBucketM <= 15) {
            return scale * 0.32f;
        }
        if (distanceBucketM <= 20) {
            return scale * 0.40f;
        }
        return scale * 0.46f;
    }

    private float startAngleForClock(String clockDirection) {
        float center;
        if ("3".equals(clockDirection)) {
            center = 0.0f;
        } else if ("2".equals(clockDirection)) {
            center = -30.0f;
        } else if ("1".equals(clockDirection)) {
            center = -60.0f;
        } else if ("9".equals(clockDirection)) {
            center = 180.0f;
        } else if ("10".equals(clockDirection)) {
            center = 150.0f;
        } else if ("11".equals(clockDirection)) {
            center = 120.0f;
        } else {
            center = -90.0f;
        }
        return center - 19.0f;
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

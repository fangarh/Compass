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
    private IffParticipantMapModel.Snapshot participantState;
    private int detailMode;
    private int mapRangeMeters = IffMapScale.defaultRangeMeters();
    private boolean phoneHeadingAvailable;
    private float phoneHeadingDeg;
    private static final long CURRENT_POINT_MS = 2500L;

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

    public void setParticipantState(IffParticipantMapModel.Snapshot nextParticipantState) {
        participantState = nextParticipantState;
        invalidate();
    }

    public void setMapRangeMeters(int nextMapRangeMeters) {
        mapRangeMeters = IffMapScale.normalizeRangeMeters(nextMapRangeMeters);
        invalidate();
    }

    public void setPhoneHeading(boolean available, float headingDeg) {
        phoneHeadingAvailable = available;
        phoneHeadingDeg = normalizeDegrees(headingDeg);
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
        drawParticipantPoints(canvas, width, height);
        drawRadioRoster(canvas, width, height);
        drawMapStatus(canvas, width);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width <= 0) {
            width = dp(320);
        }
        int desiredHeight = Math.max(dp(180), Math.round(width * 9.0f / 16.0f));
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int height = desiredHeight;
        if (heightMode == MeasureSpec.AT_MOST && heightSize > 0) {
            height = Math.min(desiredHeight, heightSize);
        } else if (heightMode == MeasureSpec.EXACTLY && heightSize > 0) {
            height = heightSize;
        }
        setMeasuredDimension(width, height);
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

    }

    private void drawFieldGeometry(Canvas canvas, int width, int height) {
        float cx = width * 0.5f;
        float cy = height * 0.5f;
        float scale = Math.min(width, height);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(0xff405037);
        canvas.drawCircle(cx, cy, scale * 0.09f, paint);
        canvas.drawCircle(cx, cy, scale * 0.18f, paint);
        canvas.drawCircle(cx, cy, scale * 0.30f, paint);
        canvas.drawCircle(cx, cy, scale * 0.45f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xffd8dfca);
        canvas.drawCircle(cx, cy, dp(5), paint);
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

    private void drawParticipantPoints(Canvas canvas, int width, int height) {
        if (participantState == null) {
            return;
        }
        if (participantState.points == null || participantState.points.isEmpty()) {
            return;
        }

        int top = dp(32);
        int bottom = height - dp(18);
        for (int i = 0; i < participantState.points.size(); i++) {
            IffParticipantMapModel.Point point = participantState.points.get(i);
            float scale = Math.min(width, height);
            float[] screenOffset = screenOffsetFor(point);
            float x = clamp((width * 0.5f) + (screenOffset[0] * scale), dp(14), width - dp(14));
            float y = clamp((height * 0.5f) + (screenOffset[1] * scale), top, bottom);
            int color = distanceColor(point.distanceAccuracyMeters);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(point.approachActive ? 0xff7dff73 : 0xffffd16a);
            canvas.drawCircle(x, y, dp(7), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(color);
            canvas.drawCircle(x, y, dp(14), paint);

            String callsign = safe(point.displayName);
            String distanceLabel = point.distanceM + "m";
            float oldTextSize = textPaint.getTextSize();
            textPaint.setTextSize(sp(13));
            float callsignWidth = textPaint.measureText(callsign);
            textPaint.setTextSize(sp(24));
            float distanceWidth = textPaint.measureText(distanceLabel);
            float blockWidth = Math.max(callsignWidth, distanceWidth);
            float textX = clamp(x + dp(16), dp(8), width - blockWidth - dp(8));
            float labelOffset = (i - ((participantState.points.size() - 1) * 0.5f)) * dp(54);
            float distanceY = clamp(y + dp(13) + labelOffset, top + dp(110), bottom);
            textPaint.setTextSize(sp(13));
            textPaint.setColor(0xffffffff);
            canvas.drawText(callsign, textX, distanceY - dp(25), textPaint);
            textPaint.setTextSize(sp(24));
            textPaint.setColor(color);
            canvas.drawText(distanceLabel, textX, distanceY, textPaint);
            textPaint.setTextSize(oldTextSize);
        }
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
    }

    private void drawRadioRoster(Canvas canvas, int width, int height) {
        int radioCount = radioOnlyVisibleCount();
        if (radioCount == 0) {
            return;
        }

        boolean hasSpatialPoints = hasParticipantPoints();
        int maxContacts = hasSpatialPoints ? 3 : 4;
        int lines = Math.min(radioCount, maxContacts) + (hasSpatialPoints ? 0 : 1);
        int rowHeight = dp(17);
        int left = dp(10);
        int top = Math.max(dp(48), height - dp(12) - (lines * rowHeight) - dp(7));
        int right = Math.min(width - dp(10), left + dp(230));
        int bottom = height - dp(8);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xaa08100b);
        canvas.drawRect(left - dp(4), top - dp(5), right + dp(4), bottom, paint);

        float oldTextSize = textPaint.getTextSize();
        textPaint.setTextSize(sp(12));
        int y = top + dp(10);
        if (!hasSpatialPoints) {
            textPaint.setColor(0xffffd16a);
            canvas.drawText(fitText("RADIO ONLY / NO GPS POINT", right - left), left, y, textPaint);
            y += rowHeight;
        }

        int drawn = 0;
        for (int i = 0; i < points.size() && drawn < maxContacts; i++) {
            MapPoint point = points.get(i);
            if (point.localDevice || (!point.current && !point.stale) || hasParticipantPoint(point.playerId)) {
                continue;
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colorFor(point));
            canvas.drawCircle(left + dp(5), y - dp(4), dp(4), paint);
            textPaint.setColor(point.current ? 0xffd8dfca : 0xffb8c49a);
            canvas.drawText(fitText(point.name + " RADIO ONLY " + point.radioLabel, right - left - dp(16)),
                    left + dp(16), y, textPaint);
            y += rowHeight;
            drawn++;
        }
        textPaint.setTextSize(oldTextSize);
    }

    private void drawMapStatus(Canvas canvas, int width) {
        float oldTextSize = textPaint.getTextSize();
        textPaint.setTextSize(sp(12));
        int y = dp(20);
        textPaint.setColor(0xffb8c49a);
        drawRight(canvas, safe(participantState == null ? "NO_MAP" : participantState.mode), width - dp(10), y);
        drawRight(canvas, IffMapScale.label(mapRangeMeters) + " / "
                        + (phoneHeadingAvailable ? "PHONE-UP " + Math.round(phoneHeadingDeg) : "N-UP"),
                width - dp(10), y + dp(17));
        if (participantState == null || participantState.points == null || participantState.points.isEmpty()) {
            drawRight(canvas, "acc -- hidden="
                            + (participantState == null ? 0 : participantState.hiddenCount),
                    width - dp(10), y + dp(34));
            textPaint.setTextSize(oldTextSize);
            return;
        }
        for (int i = 0; i < participantState.points.size() && i < 3; i++) {
            IffParticipantMapModel.Point point = participantState.points.get(i);
            textPaint.setColor(distanceColor(point.distanceAccuracyMeters));
            drawRight(canvas,
                    safe(point.displayName)
                            + " +/-" + Math.round(point.distanceAccuracyMeters) + "m "
                            + freshnessLabel(point),
                    width - dp(10),
                    y + dp(17 * (i + 2)));
        }
        textPaint.setTextSize(oldTextSize);
    }

    private boolean hasParticipantPoints() {
        return participantState != null
                && participantState.points != null
                && !participantState.points.isEmpty();
    }

    private boolean hasParticipantPoint(String playerId) {
        if (participantState == null || participantState.points == null) {
            return false;
        }
        for (int i = 0; i < participantState.points.size(); i++) {
            IffParticipantMapModel.Point point = participantState.points.get(i);
            if (point != null && safe(playerId).equals(point.playerId)) {
                return true;
            }
        }
        return false;
    }

    private int radioOnlyVisibleCount() {
        int count = 0;
        for (int i = 0; i < points.size(); i++) {
            MapPoint point = points.get(i);
            if (!point.localDevice && (point.current || point.stale) && !hasParticipantPoint(point.playerId)) {
                count++;
            }
        }
        return count;
    }

    private String fitText(String text, float maxWidth) {
        String value = safe(text);
        if (textPaint.measureText(value) <= maxWidth) {
            return value;
        }
        String suffix = "...";
        float suffixWidth = textPaint.measureText(suffix);
        int end = value.length();
        while (end > 0 && textPaint.measureText(value.substring(0, end)) + suffixWidth > maxWidth) {
            end--;
        }
        return end <= 0 ? suffix : value.substring(0, end) + suffix;
    }

    private float[] screenOffsetFor(IffParticipantMapModel.Point point) {
        if (point == null) {
            return new float[] {0.0f, 0.0f};
        }
        float radius = IffMapScale.screenRadius(point.distanceM, mapRangeMeters);
        float bearing = phoneHeadingAvailable
                ? normalizeDegrees(point.bearingDeg - phoneHeadingDeg)
                : point.bearingDeg;
        double relativeBearing = Math.toRadians(bearing);
        return new float[] {
                (float) (Math.sin(relativeBearing) * radius),
                -(float) (Math.cos(relativeBearing) * radius)
        };
    }

    private void drawRight(Canvas canvas, String text, float right, float baseline) {
        canvas.drawText(text, right - textPaint.measureText(text), baseline, textPaint);
    }

    private String freshnessLabel(IffParticipantMapModel.Point point) {
        return point.ageMs <= CURRENT_POINT_MS ? "CURRENT" : "STALE";
    }

    private int distanceColor(float accuracyMeters) {
        if (accuracyMeters <= 15.0f) {
            return 0xff7dff73;
        }
        if (accuracyMeters <= 30.0f) {
            return 0xffffb84d;
        }
        return 0xffff5f5f;
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
        if (distanceBucketM <= 15) {
            return scale * 0.09f;
        }
        if (distanceBucketM <= 30) {
            return scale * 0.18f;
        }
        if (distanceBucketM <= 50) {
            return scale * 0.30f;
        }
        return scale * 0.45f;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private float normalizeDegrees(float degrees) {
        float value = degrees % 360.0f;
        return value < 0.0f ? value + 360.0f : value;
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
        public final String playerId;
        public final String name;
        public final String radioLabel;
        public final boolean localDevice;
        public final boolean selected;
        public final boolean current;
        public final boolean stale;

        public MapPoint(String playerId, String name, String radioLabel, boolean localDevice, boolean selected,
                        boolean current, boolean stale) {
            this.playerId = playerId == null ? "" : playerId;
            this.name = name;
            this.radioLabel = radioLabel;
            this.localDevice = localDevice;
            this.selected = selected;
            this.current = current;
            this.stale = stale;
        }
    }
}

package net.afterday.compas.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import net.afterday.compas.R;
import net.afterday.compas.settings.Constants;
import net.afterday.compas.settings.Settings;

/* JADX INFO: loaded from: classes.dex */
public class Compass extends View implements SensorListener, SensorEventListener {
    private static final String TAG = "Compass";
    private static final int WIDGET_HEIGHT = 1030;
    private static final int WIDGET_WIDTH = 1030;
    private final float MAX_ROATE_DEGREE;
    private Bitmap compass;
    private Drawable compassD;
    private float degrees;
    private boolean isOn;
    private Sensor mAccelerometer;
    protected Runnable mCompassViewUpdater;
    private float mCurrentDegree;
    private float mDirection;
    protected final Handler mHandler;
    private int mHeight;
    private AccelerateInterpolator mInterpolator;
    private float[] mLastAccelerometer;
    private boolean mLastAccelerometerSet;
    private float[] mLastMagnetometer;
    private boolean mLastMagnetometerSet;
    private String mLocationProvider;
    private Sensor mMagnetometer;
    private float[] mOrientation;
    private Sensor mOrientationSensor;
    private SensorEventListener mOrientationSensorEventListener;
    private float[] mR;
    private float mScaleFactorX;
    private float mScaleFactorY;
    private SensorManager mSensorManager;
    private float mTargetDirection;
    private int mWidth;
    private Matrix matrix;
    private int offset;

    static /* synthetic */ boolean access$000(Compass x0) {
        return x0.isOn;
    }

    static /* synthetic */ float access$100(Compass x0) {
        return x0.mDirection;
    }

    static /* synthetic */ float access$102(Compass x0, float x1) {
        x0.mDirection = x1;
        return x1;
    }

    static /* synthetic */ float access$200(Compass x0) {
        return x0.mTargetDirection;
    }

    static /* synthetic */ float access$202(Compass x0, float x1) {
        x0.mTargetDirection = x1;
        return x1;
    }

    static /* synthetic */ int access$300(Compass x0) {
        return x0.offset;
    }

    static /* synthetic */ AccelerateInterpolator access$400(Compass x0) {
        return x0.mInterpolator;
    }

    static /* synthetic */ float access$500(Compass x0, float x1) {
        return x0.normalizeDegree(x1);
    }

    public Compass(Context context) {
        super(context);
        this.mLastAccelerometer = new float[3];
        this.mLastMagnetometer = new float[3];
        this.mLastAccelerometerSet = false;
        this.mLastMagnetometerSet = false;
        this.mR = new float[9];
        this.mOrientation = new float[3];
        this.mCurrentDegree = 0.0f;
        this.degrees = 0.0f;
        this.isOn = false;
        this.MAX_ROATE_DEGREE = 1.0f;
        this.mHandler = new Handler();
        this.offset = 0;
        this.mCompassViewUpdater = new AnonymousClass1();
        this.mOrientationSensorEventListener = new AnonymousClass2();
        init();
    }

    public Compass(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mLastAccelerometer = new float[3];
        this.mLastMagnetometer = new float[3];
        this.mLastAccelerometerSet = false;
        this.mLastMagnetometerSet = false;
        this.mR = new float[9];
        this.mOrientation = new float[3];
        this.mCurrentDegree = 0.0f;
        this.degrees = 0.0f;
        this.isOn = false;
        this.MAX_ROATE_DEGREE = 1.0f;
        this.mHandler = new Handler();
        this.offset = 0;
        this.mCompassViewUpdater = new AnonymousClass1();
        this.mOrientationSensorEventListener = new AnonymousClass2();
        init();
    }

    public Compass(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mLastAccelerometer = new float[3];
        this.mLastMagnetometer = new float[3];
        this.mLastAccelerometerSet = false;
        this.mLastMagnetometerSet = false;
        this.mR = new float[9];
        this.mOrientation = new float[3];
        this.mCurrentDegree = 0.0f;
        this.degrees = 0.0f;
        this.isOn = false;
        this.MAX_ROATE_DEGREE = 1.0f;
        this.mHandler = new Handler();
        this.offset = 0;
        this.mCompassViewUpdater = new AnonymousClass1();
        this.mOrientationSensorEventListener = new AnonymousClass2();
        init();
    }

    /* JADX INFO: renamed from: net.afterday.compas.view.Compass$1, reason: invalid class name */
    class AnonymousClass1 implements Runnable {
        AnonymousClass1() {
        }

        @Override // java.lang.Runnable
        public void run() {
            if (Compass.access$000(Compass.this)) {
                if (Compass.access$100(Compass.this) != Compass.access$200(Compass.this)) {
                    float to = Compass.access$200(Compass.this) + Compass.access$300(Compass.this);
                    if (to - Compass.access$100(Compass.this) > 180.0f) {
                        to -= 360.0f;
                    } else if (to - Compass.access$100(Compass.this) < -180.0f) {
                        to += 360.0f;
                    }
                    float distance = to - Compass.access$100(Compass.this);
                    if (Math.abs(distance) > 1.0f) {
                        distance = distance > 0.0f ? 1.0f : -1.0f;
                    }
                    Compass compass = Compass.this;
                    Compass.access$102(compass, Compass.access$500(compass, Compass.access$100(compass) + ((to - Compass.access$100(Compass.this)) * Compass.access$400(Compass.this).getInterpolation(Math.abs(distance) > 1.0f ? 0.4f : 0.3f))));
                    Compass.this.postInvalidate();
                }
                Compass.this.mHandler.postDelayed(Compass.this.mCompassViewUpdater, 50L);
            }
        }
    }

    /* JADX INFO: renamed from: net.afterday.compas.view.Compass$2, reason: invalid class name */
    class AnonymousClass2 implements SensorEventListener {
        AnonymousClass2() {
        }

        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            float direction = event.values[0] * (-1.0f);
            Compass compass = Compass.this;
            Compass.access$202(compass, Compass.access$500(compass, direction));
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    private float normalizeDegree(float degree) {
        return (720.0f + degree) % 360.0f;
    }

    public void compassOff() {
        if (!this.isOn) {
            return;
        }
        this.mHandler.removeCallbacks(this.mCompassViewUpdater);
        try {
            this.mSensorManager.unregisterListener(this.mOrientationSensorEventListener, this.mOrientationSensor);
        } catch (Exception e) {
        }
        this.mSensorManager = null;
        this.mOrientationSensor = null;
        this.mDirection = 0.0f;
        postInvalidate();
        this.isOn = false;
    }

    public void compassOn() {
        if (this.isOn) {
            return;
        }
        initResources();
        initServices();
        Sensor sensor = this.mOrientationSensor;
        if (sensor != null) {
            this.mSensorManager.registerListener(this.mOrientationSensorEventListener, sensor, 1);
        }
        this.mHandler.postDelayed(this.mCompassViewUpdater, 50L);
        this.isOn = true;
    }

    @Override // android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int finalMeasureSpecX = View.MeasureSpec.makeMeasureSpec(widthSize, 1073741824);
        int finalMeasureSpecY = View.MeasureSpec.makeMeasureSpec(heightSize, 1073741824);
        super.onMeasure(finalMeasureSpecX, finalMeasureSpecY);
        this.mWidth = widthSize;
        this.mHeight = heightSize;
        this.mScaleFactorX = this.mWidth / 1030.0f;
        this.mScaleFactorY = this.mHeight / 1030.0f;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        convertRect(this.compass.getWidth(), this.compass.getHeight(), 0, 0, this.matrix);
        canvas.drawBitmap(this.compass, this.matrix, null);
    }

    @Override // android.view.View
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        compassOn();
    }

    private void initResources() {
        this.mDirection = 0.0f;
        this.mTargetDirection = 0.0f;
        this.mInterpolator = new AccelerateInterpolator();
    }

    private void initServices() {
        this.mSensorManager = (SensorManager) getContext().getSystemService("sensor");
        this.mOrientationSensor = this.mSensorManager.getDefaultSensor(3);
    }

    private void convertRect(int bitmapWidth, int bitmapHeight, int left, int top, Matrix matrix) {
        matrix.reset();
        matrix.postRotate(this.mDirection, bitmapWidth / 2, bitmapHeight / 2);
        matrix.postScale(this.mScaleFactorX, this.mScaleFactorY);
        matrix.postTranslate(this.mScaleFactorX * left, this.mScaleFactorY * top);
    }

    protected void init() {
        this.offset = Settings.instance().getIntSetting(Constants.ORIENTATION) == 1 ? 0 : 90;
        this.compass = BitmapFactory.decodeResource(getResources(), R.drawable.compass);
        this.matrix = new Matrix();
    }

    @Override // android.hardware.SensorListener
    public void onSensorChanged(int i, float[] floats) {
    }

    @Override // android.hardware.SensorListener
    public void onAccuracyChanged(int i, int i1) {
    }

    @Override // android.hardware.SensorEventListener
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == this.mAccelerometer) {
            System.arraycopy(event.values, 0, this.mLastAccelerometer, 0, event.values.length);
            this.mLastAccelerometerSet = true;
        } else if (event.sensor == this.mMagnetometer) {
            System.arraycopy(event.values, 0, this.mLastMagnetometer, 0, event.values.length);
            this.mLastMagnetometerSet = true;
        }
        SensorManager.getRotationMatrix(this.mR, null, this.mLastAccelerometer, this.mLastMagnetometer);
        SensorManager.getOrientation(this.mR, this.mOrientation);
        float azimuthInRadians = this.mOrientation[0];
        this.degrees = ((float) (Math.toDegrees(azimuthInRadians) + 360.0d)) % 360.0f;
        invalidate();
    }

    @Override // android.view.View
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        compassOff();
        if (!this.compass.isRecycled()) {
            this.compass.recycle();
        }
    }

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}

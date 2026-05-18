package net.afterday.compas.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import java.util.HashMap;
import java.util.Map;
import net.afterday.compas.R;
import net.afterday.compas.core.player.Player;

/* JADX INFO: loaded from: classes.dex */
public class Tube extends View {
    private static final String TAG = "Tube";
    private static final int WIDGET_HEIGHT = 335;
    private static final int WIDGET_WIDTH = 766;
    private Map<Player.STATE, Bitmap> bitmapsByState;
    private Player.STATE currentState;
    private Bitmap currentTube;
    private Player.FRACTION fraction;
    private int level;
    private double mAnomaly;
    private double mBurer;
    private long mControlled;
    private double mController;
    private boolean mEmission;
    private double mHealing;
    private int mHeight;
    private Matrix mMatrix;
    private double mMental;
    private double mMonolith;
    private Paint mPaint;
    private double mRadiation;
    private RectF mRect;
    private float mScaleFactorX;
    private float mScaleFactorY;
    private Bitmap mTubeAbducted;
    private Bitmap mTubeAnomaly;
    private Bitmap mTubeBomb;
    private Bitmap mTubeBurer;
    private Bitmap mTubeClear;
    private Bitmap mTubeControlled;
    private Bitmap mTubeController;
    private Bitmap mTubeDeadAno;
    private Bitmap mTubeDeadEmission;
    private Bitmap mTubeDeadMental;
    private Bitmap mTubeDeadMisc;
    private Bitmap mTubeDeadRad;
    private Bitmap mTubeDying;
    private Bitmap mTubeEmission;
    private Bitmap mTubeHealing;
    private Bitmap mTubeMental;
    private Bitmap mTubeMonolith;
    private Bitmap mTubeOff;
    private Bitmap mTubeRad0;
    private Bitmap mTubeRad1;
    private Bitmap mTubeRad2;
    private Bitmap mTubeTransmutation;
    private Bitmap mTubeUnknown;
    private Bitmap mTubeVodka;
    private Bitmap mTubeZombified;
    private int mWidth;
    private long mZombified;

    public Tube(Context context) {
        super(context);
        init();
    }

    public Tube(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Tube(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setParameters(double radiation, double anomaly, double mental, double monolith, double controller, double burer, double healing, Player.STATE playerState) {
        this.mRadiation = radiation;
        this.mAnomaly = anomaly;
        this.mMental = mental;
        this.mMonolith = monolith;
        this.mController = controller;
        this.mBurer = burer;
        this.mHealing = healing;
        this.currentState = playerState;
        repaintIfNeed();
    }

    public void setEmission(boolean emission) {
        this.mEmission = emission;
        repaintIfNeed();
    }

    public void setFraction(Player.FRACTION fraction) {
        this.fraction = fraction;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setState(Player.STATE playerState) {
        this.currentState = playerState;
        repaintIfNeed();
    }

    private void repaintIfNeed() {
        Bitmap t = getCurrentTube();
        Log.e(TAG, "t: " + t);
        if (this.currentTube == t) {
            return;
        }
        this.currentTube = t;
        invalidate();
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
        this.mScaleFactorX = this.mWidth / 766.0f;
        this.mScaleFactorY = this.mHeight / 335.0f;
        this.mPaint.setTextSize(this.mScaleFactorY * 50.0f);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d(TAG, "onDraw " + this.currentState);
        drawTube(canvas);
    }

    protected void init() {
        bindResources();
        bindStatesToImg();
        this.mZombified = System.currentTimeMillis();
        this.currentState = Player.STATE.ALIVE;
        this.mMatrix = new Matrix();
        this.mRect = new RectF();
        this.mPaint = new Paint();
        this.mPaint.setARGB(255, 100, 255, 100);
    }

    private void bindResources() {
        this.mTubeOff = BitmapFactory.decodeResource(getResources(), R.drawable.tube_off);
        this.mTubeDeadAno = BitmapFactory.decodeResource(getResources(), R.drawable.tube_dead_ano);
        this.mTubeDeadMental = BitmapFactory.decodeResource(getResources(), R.drawable.tube_dead_mental);
        this.mTubeDeadMisc = BitmapFactory.decodeResource(getResources(), R.drawable.tube_dead_misc);
        this.mTubeDeadRad = BitmapFactory.decodeResource(getResources(), R.drawable.tube_dead_rad);
        this.mTubeAnomaly = BitmapFactory.decodeResource(getResources(), R.drawable.tube_anomaly);
        this.mTubeBurer = BitmapFactory.decodeResource(getResources(), R.drawable.tube_burer);
        this.mTubeClear = BitmapFactory.decodeResource(getResources(), R.drawable.tube_clear);
        this.mTubeControlled = BitmapFactory.decodeResource(getResources(), R.drawable.tube_controlled);
        this.mTubeController = BitmapFactory.decodeResource(getResources(), R.drawable.tube_controller);
        this.mTubeEmission = BitmapFactory.decodeResource(getResources(), R.drawable.tube_emission);
        this.mTubeDeadEmission = BitmapFactory.decodeResource(getResources(), R.drawable.tube_dead_emi);
        this.mTubeHealing = BitmapFactory.decodeResource(getResources(), R.drawable.tube_healing);
        this.mTubeMental = BitmapFactory.decodeResource(getResources(), R.drawable.tube_mental);
        this.mTubeMonolith = BitmapFactory.decodeResource(getResources(), R.drawable.tube_monolith);
        this.mTubeRad0 = BitmapFactory.decodeResource(getResources(), R.drawable.tube_rad0);
        this.mTubeRad1 = BitmapFactory.decodeResource(getResources(), R.drawable.tube_rad1);
        this.mTubeRad2 = BitmapFactory.decodeResource(getResources(), R.drawable.tube_rad2);
        this.mTubeZombified = BitmapFactory.decodeResource(getResources(), R.drawable.tube_zombified);
        this.mTubeAbducted = BitmapFactory.decodeResource(getResources(), R.drawable.tube_abducted);
        this.mTubeDying = BitmapFactory.decodeResource(getResources(), R.drawable.tube_dying);
        this.mTubeTransmutation = BitmapFactory.decodeResource(getResources(), R.drawable.tube_transmutation);
        this.mTubeUnknown = BitmapFactory.decodeResource(getResources(), R.drawable.tube_unknown);
    }

    private void bindStatesToImg() {
        this.bitmapsByState = new HashMap();
        this.bitmapsByState.put(Player.STATE.W_CONTROLLED, this.mTubeTransmutation);
        this.bitmapsByState.put(Player.STATE.W_DEAD_ANOMALY, this.mTubeDying);
        this.bitmapsByState.put(Player.STATE.W_DEAD_BURER, this.mTubeDying);
        this.bitmapsByState.put(Player.STATE.W_MENTALLED, this.mTubeTransmutation);
        this.bitmapsByState.put(Player.STATE.W_DEAD_RADIATION, this.mTubeDying);
        this.bitmapsByState.put(Player.STATE.DEAD_RADIATION, this.mTubeDeadRad);
        this.bitmapsByState.put(Player.STATE.DEAD_EMISSION, this.mTubeDeadEmission);
        this.bitmapsByState.put(Player.STATE.CONTROLLED, this.mTubeControlled);
        this.bitmapsByState.put(Player.STATE.MENTALLED, this.mTubeZombified);
        this.bitmapsByState.put(Player.STATE.DEAD_CONTROLLER, this.mTubeDeadMental);
        this.bitmapsByState.put(Player.STATE.DEAD_ANOMALY, this.mTubeDeadAno);
        this.bitmapsByState.put(Player.STATE.DEAD_MENTAL, this.mTubeDeadMental);
        this.bitmapsByState.put(Player.STATE.ALIVE, this.mTubeClear);
        this.bitmapsByState.put(Player.STATE.DEAD_BURER, this.mTubeDeadMisc);
        this.bitmapsByState.put(Player.STATE.ABDUCTED, this.mTubeAbducted);
        this.bitmapsByState.put(Player.STATE.W_ABDUCTED, this.mTubeDying);
    }

    protected void drawTube(Canvas canvas) {
        Bitmap bitmap = this.currentTube;
        if (bitmap == null) {
            return;
        }
        convertRect(bitmap.getWidth(), this.currentTube.getHeight(), 0, 0, this.mMatrix);
        canvas.drawBitmap(this.currentTube, this.mMatrix, null);
    }

    private Bitmap getCurrentTube() {
        if (this.fraction == Player.FRACTION.MONOLITH && this.mMonolith > 0.0d) {
            return this.mTubeMonolith;
        }
        if (this.fraction == Player.FRACTION.DARKEN && this.mRadiation > 0.0d) {
            return this.mTubeRad0;
        }
        if (this.mEmission && this.currentState.getCode() == 1) {
            return this.mTubeEmission;
        }
        if (this.currentState.getCode() != 1) {
            if (this.bitmapsByState.containsKey(this.currentState)) {
                return this.bitmapsByState.get(this.currentState);
            }
            return this.mTubeOff;
        }
        if (this.currentState == Player.STATE.ABDUCTED) {
            return this.mTubeAbducted;
        }
        if (this.mHealing > 0.0d && this.fraction != Player.FRACTION.MONOLITH) {
            return this.mTubeHealing;
        }
        if (this.mRadiation < 0.01d && this.mAnomaly < 0.01d && this.mMental < 0.01d && this.mMonolith < 0.01d && this.mController < 0.01d && this.mBurer < 0.01d) {
            return this.mTubeClear;
        }
        int maxInf = maxInfluence();
        if (maxInf == 0) {
            double d = this.mRadiation;
            if (d >= 7.0d) {
                return this.mTubeRad2;
            }
            if (d >= 1.0d) {
                return this.mTubeRad1;
            }
            if (d >= 0.0d) {
                return this.mTubeRad0;
            }
        } else if (maxInf != 1) {
            if (maxInf == 2) {
                return this.level >= 3 ? this.mTubeMental : this.mTubeUnknown;
            }
            if (maxInf == 3) {
                return this.level >= 4 ? this.mTubeController : this.mTubeUnknown;
            }
            if (maxInf == 4) {
                return this.level >= 4 ? this.mTubeBurer : this.mTubeUnknown;
            }
            if (maxInf == 5) {
                return this.mTubeMonolith;
            }
            return this.mTubeOff;
        }
        return this.level >= 2 ? this.mTubeAnomaly : this.mTubeUnknown;
    }

    private int maxInfluence() {
        double max = 0.0d;
        int inf = -1;
        if (this.mRadiation > 0.0d) {
            inf = 0;
            max = this.mRadiation;
        }
        if (this.mAnomaly > max) {
            inf = 1;
            max = this.mAnomaly;
        }
        if (this.mMental > max) {
            inf = 2;
            max = this.mMental;
        }
        if (this.mController > max) {
            inf = 3;
            max = this.mController;
        }
        if (this.mBurer > max) {
            inf = 4;
            max = this.mBurer;
        }
        if (this.mMonolith > max) {
            double max2 = this.mMonolith;
            return 5;
        }
        return inf;
    }

    private void convertRect(int bitmapWidth, int bitmapHeight, int left, int top, Matrix matrix) {
        matrix.reset();
        matrix.postScale(this.mScaleFactorX, this.mScaleFactorY);
        matrix.postTranslate(this.mScaleFactorX * left, this.mScaleFactorY * top);
    }
}

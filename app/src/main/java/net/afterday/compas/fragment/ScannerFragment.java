package net.afterday.compas.fragment;

import android.app.DialogFragment;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;
import java.util.List;
import net.afterday.compas.R;
import net.afterday.compas.engine.events.CodeInputEventBus;
import net.afterday.compas.util.OnSwipeTouchListener;

/* JADX INFO: loaded from: classes.dex */
public class ScannerFragment extends DialogFragment {
    private static final String TAG = "ScannerFragment";
    private int currentCam;
    private DecoratedBarcodeView dbw;
    private boolean flashOn;
    private View v;

    static /* synthetic */ void access$000(ScannerFragment x0) {
        x0.changeCamera();
    }

    static /* synthetic */ void access$100(ScannerFragment x0) {
        x0.toggleFlashLight();
    }

    static /* synthetic */ DecoratedBarcodeView access$200(ScannerFragment x0) {
        return x0.dbw;
    }

    public static ScannerFragment newInstance() {
        ScannerFragment f = new ScannerFragment();
        return f;
    }

    @Override // android.app.DialogFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(2, R.style.DialogStyle);
    }

    /* JADX INFO: renamed from: net.afterday.compas.fragment.ScannerFragment$1, reason: invalid class name */
    class AnonymousClass1 extends OnSwipeTouchListener {
        AnonymousClass1(Context arg0) {
            super(arg0);
        }

        @Override // net.afterday.compas.util.OnSwipeTouchListener
        public void onSwipeLeft() {
            ScannerFragment.access$000(ScannerFragment.this);
        }

        @Override // net.afterday.compas.util.OnSwipeTouchListener
        public void onSwipeRight() {
            ScannerFragment.access$000(ScannerFragment.this);
        }

        @Override // net.afterday.compas.util.OnSwipeTouchListener
        public void onSwipeUp() {
            ScannerFragment.access$100(ScannerFragment.this);
        }

        @Override // net.afterday.compas.util.OnSwipeTouchListener
        public void onSwipeDown() {
            ScannerFragment.access$100(ScannerFragment.this);
        }
    }

    @Override // android.app.Fragment
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        this.v = inflater.inflate(R.layout.scanner_fragment, container, false);
        this.v.setOnTouchListener(new AnonymousClass1(getActivity()));
        this.v.findViewById(R.id.close).setOnClickListener(new AnonymousClass2());
        scanQr();
        return this.v;
    }

    /* JADX INFO: renamed from: net.afterday.compas.fragment.ScannerFragment$2, reason: invalid class name */
    class AnonymousClass2 implements View.OnClickListener {
        AnonymousClass2() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View v) {
            try {
                ScannerFragment.this.closePopup(v);
            } catch (Exception e) {
            }
        }
    }

    @Override // android.app.Fragment
    public void onDestroy() {
        super.onDestroy();
        this.v.setOnTouchListener(null);
    }

    public void closePopup(View view) {
        dismiss();
    }

    private void toggleFlashLight() {
        if (this.flashOn) {
            this.dbw.setTorchOff();
            this.flashOn = false;
        } else {
            this.dbw.setTorchOn();
            this.flashOn = true;
        }
    }

    private void changeCamera() {
        if (this.currentCam == 1) {
            this.currentCam = 0;
        } else {
            this.currentCam = 1;
        }
        this.dbw.pause();
        this.dbw.getBarcodeView().setCameraSettings(getCameraSettings(this.currentCam));
        this.dbw.resume();
    }

    private void scanQr() {
        this.dbw = (DecoratedBarcodeView) this.v.findViewById(R.id.zxing_barcode_scanner);
        this.dbw.setStatusText(null);
        this.currentCam = 1;
        this.dbw.getBarcodeView().setCameraSettings(getCameraSettings(this.currentCam));
        this.dbw.getBarcodeView().decodeSingle(new AnonymousClass3());
    }

    /* JADX INFO: renamed from: net.afterday.compas.fragment.ScannerFragment$3, reason: invalid class name */
    class AnonymousClass3 implements BarcodeCallback {
        AnonymousClass3() {
        }

        @Override // com.journeyapps.barcodescanner.BarcodeCallback
        public void barcodeResult(BarcodeResult result) {
            Log.d(ScannerFragment.TAG, "ItemScanned: " + result.getText());
            CodeInputEventBus.codeScanned(result.getText());
            ScannerFragment.access$200(ScannerFragment.this).pause();
            ScannerFragment.this.getActivity().onBackPressed();
        }

        @Override // com.journeyapps.barcodescanner.BarcodeCallback
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    }

    private CameraSettings getCameraSettings(int cameraType) {
        CameraSettings cs = new CameraSettings();
        int cameraId = 0;
        int defaultId = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        int camIdx = 0;
        while (true) {
            if (camIdx >= cameraCount) {
                break;
            }
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == 1) {
                defaultId = camIdx;
            }
            if (cameraInfo.facing != cameraType) {
                camIdx++;
            } else {
                cameraId = camIdx;
                break;
            }
        }
        cs.setRequestedCameraId(cameraId > 0 ? cameraId : defaultId);
        return cs;
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        this.dbw.getBarcodeView().resume();
    }

    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        this.dbw.getBarcodeView().pause();
    }
}

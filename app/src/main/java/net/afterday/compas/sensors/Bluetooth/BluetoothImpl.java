package net.afterday.compas.sensors.Bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/* JADX INFO: loaded from: classes.dex */
@TargetApi(18)
public class BluetoothImpl implements Bluetooth {
    private static final String TAG = "BluetoothImpl";
    private BluetoothAdapter bla;
    private BluetoothReceiver br;
    private BluetoothAdapter.LeScanCallback callback;
    private Context context;
    private IntentFilter intentFilter;
    private Disposable resetter;
    private Observable<Double> resultStream = PublishSubject.create();
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private List<String> registeredMacs = new ArrayList();

    static /* synthetic */ List access$200(BluetoothImpl x0) {
        return x0.registeredMacs;
    }

    static /* synthetic */ Disposable access$300(BluetoothImpl x0) {
        return x0.resetter;
    }

    static /* synthetic */ Disposable access$302(BluetoothImpl x0, Disposable x1) {
        x0.resetter = x1;
        return x1;
    }

    static /* synthetic */ Observable access$400(BluetoothImpl x0) {
        return x0.resultStream;
    }

    static /* synthetic */ AtomicBoolean access$500(BluetoothImpl x0) {
        return x0.isRunning;
    }

    static /* synthetic */ BluetoothAdapter.LeScanCallback access$600(BluetoothImpl x0) {
        return x0.callback;
    }

    static /* synthetic */ BluetoothAdapter access$700(BluetoothImpl x0) {
        return x0.bla;
    }

    public BluetoothImpl(Context context) {
        this.br = new BluetoothReceiver(this, null);
        this.context = context;
        setup();
        this.br = new BluetoothReceiver(this, null);
        this.bla = BluetoothAdapter.getDefaultAdapter();
        this.intentFilter = new IntentFilter("android.bluetooth.device.action.FOUND");
        this.callback = new LeScanCallback(this, null);
    }

    private void setup() {
        this.registeredMacs.add("FF:FF:3E:F4:03:09");
        this.registeredMacs.add("FF:FF:3D:F9:15:7E");
        this.registeredMacs.add("FF:FF:3C:FA:C6:B3");
        this.registeredMacs.add("FF:FF:3B:FE:9F:38");
        this.registeredMacs.add("FF:FF:3D:F9:35:58");
        this.registeredMacs.add("FF:FF:3F:F4:32:BB");
        this.registeredMacs.add("FF:FF:3E:F3:96:5A");
        this.registeredMacs.add("FF:FF:3E:F4:03:7B");
        this.registeredMacs.add("FF:FF:3E:F3:F1:0F");
        this.registeredMacs.add("FF:FF:3E:F4:0A:5E");
        this.registeredMacs.add("FF:FF:3B:FE:9C:90");
        this.registeredMacs.add("FF:FF:3E:F3:F4:EC");
        this.registeredMacs.add("FF:FF:3C:FA:D9:E8");
        this.registeredMacs.add("FF:FF:3E:F3:F5:14");
        this.registeredMacs.add("FF:FF:3F:F3:DC:BF");
        this.registeredMacs.add("FF:FF:3F:F4:27:A6");
        this.registeredMacs.add("FF:FF:3E:F4:13:90");
        this.registeredMacs.add("FF:FF:3E:F3:97:32");
        this.registeredMacs.add("FF:FF:39:F3:95:0E");
        this.registeredMacs.add("FF:FF:3E:F3:F4:EC");
        this.registeredMacs.add("00:12:40:60:02:7E");
        this.registeredMacs.add("66:4f:91:3d:87:91");
        this.registeredMacs.add("ba:f9:46:03:26:67");
    }

    @Override // net.afterday.compas.sensors.Sensor
    public void start() {
        this.isRunning.set(true);
        this.bla.startLeScan(this.callback);
    }

    @Override // net.afterday.compas.sensors.Sensor
    public void stop() {
        this.isRunning.set(false);
    }

    @Override // net.afterday.compas.sensors.Sensor
    public Observable<Double> getSensorResultsStream() {
        return this.resultStream;
    }

    /* JADX INFO: Access modifiers changed from: private */
    class LeScanCallback implements BluetoothAdapter.LeScanCallback {
        private LeScanCallback() {
        }

        /* synthetic */ LeScanCallback(BluetoothImpl x0, Object x1) {
            this();
        }

        @Override // android.bluetooth.BluetoothAdapter.LeScanCallback
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            System.currentTimeMillis();
            if (BluetoothImpl.access$200(BluetoothImpl.this).contains(bluetoothDevice.getAddress())) {
                if (BluetoothImpl.access$300(BluetoothImpl.this) != null && !BluetoothImpl.access$300(BluetoothImpl.this).isDisposed()) {
                    BluetoothImpl.access$300(BluetoothImpl.this).dispose();
                }
                ((Subject) BluetoothImpl.access$400(BluetoothImpl.this)).onNext(new Double(i + 100));
                BluetoothImpl.access$302(BluetoothImpl.this, Observable.timer(3L, TimeUnit.SECONDS).subscribe(new $$Lambda$BluetoothImpl$LeScanCallback$m1bevS7FLoSbxDjges1RSiTEXMo(this)));
            }
            if (!BluetoothImpl.access$500(BluetoothImpl.this).get()) {
                BluetoothImpl.access$700(BluetoothImpl.this).stopLeScan(BluetoothImpl.access$600(BluetoothImpl.this));
            }
        }

        public /* synthetic */ void lambda$onLeScan$0$BluetoothImpl$LeScanCallback(Long x) {
            ((Subject) BluetoothImpl.access$400(BluetoothImpl.this)).onNext(Double.valueOf(0.0d));
        }
    }

    private class BluetoothReceiver extends BroadcastReceiver {
        private BluetoothReceiver() {
        }

        /* synthetic */ BluetoothReceiver(BluetoothImpl x0, Object x1) {
            this();
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.bluetooth.device.action.FOUND".equals(intent.getAction())) {
                Log.d("BLUETOOTH RECEIVED!", "" + ((int) intent.getShortExtra("android.bluetooth.device.extra.RSSI", Short.MIN_VALUE)));
            }
            if (BluetoothImpl.access$500(BluetoothImpl.this).get()) {
                Log.e(BluetoothImpl.TAG, "START DISCOVERY");
                BluetoothImpl.access$700(BluetoothImpl.this).startDiscovery();
            }
        }
    }
}

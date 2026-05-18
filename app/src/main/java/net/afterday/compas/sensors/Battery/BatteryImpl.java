package net.afterday.compas.sensors.Battery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

/* JADX INFO: loaded from: classes.dex */
public class BatteryImpl implements Battery {
    private BatteryManager bManager;
    private Observable<BatteryStatus> batteryLevel = BehaviorSubject.create();
    private Context context;
    private IntentFilter iFilter;
    private Intent intent;
    private BatteryListener listener;

    static /* synthetic */ Observable access$100(BatteryImpl x0) {
        return x0.batteryLevel;
    }

    public BatteryImpl(Context context) {
        this.context = context;
    }

    @Override // net.afterday.compas.sensors.Sensor
    public void start() {
        if (this.listener == null) {
            this.listener = new BatteryListener(this, null);
        }
        this.context.registerReceiver(this.listener, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
    }

    @Override // net.afterday.compas.sensors.Sensor
    public void stop() {
        this.context.unregisterReceiver(this.listener);
    }

    @Override // net.afterday.compas.sensors.Sensor
    public Observable<BatteryStatus> getSensorResultsStream() {
        return this.batteryLevel;
    }

    private class BatteryListener extends BroadcastReceiver {
        private BatteryListener() {
        }

        /* synthetic */ BatteryListener(BatteryImpl x0, Object x1) {
            this();
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra("status", -1);
            boolean isCharging = status == 2 || status == 5;
            ((Subject) BatteryImpl.access$100(BatteryImpl.this)).onNext(new BatteryStatusImpl(intent.getIntExtra("level", -1), isCharging));
        }
    }

    private static class BatteryStatusImpl implements BatteryStatus {
        private boolean c;
        private int e;

        public BatteryStatusImpl(int e, boolean c) {
            this.e = e;
            this.c = c;
        }

        @Override // net.afterday.compas.sensors.Battery.BatteryStatus
        public int getEnergyLevel() {
            return this.e;
        }

        @Override // net.afterday.compas.sensors.Battery.BatteryStatus
        public boolean isCharging() {
            return this.c;
        }
    }
}

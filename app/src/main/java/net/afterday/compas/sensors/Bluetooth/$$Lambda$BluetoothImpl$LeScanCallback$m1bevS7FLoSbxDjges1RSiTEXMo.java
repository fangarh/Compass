package net.afterday.compas.sensors.Bluetooth;

import io.reactivex.functions.Consumer;
import net.afterday.compas.sensors.Bluetooth.BluetoothImpl;

/* JADX INFO: renamed from: net.afterday.compas.sensors.Bluetooth.-$$Lambda$BluetoothImpl$LeScanCallback$m1bevS7FLoSbxDjges1RSiTEXMo, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$BluetoothImpl$LeScanCallback$m1bevS7FLoSbxDjges1RSiTEXMo implements Consumer {
    private final /* synthetic */ BluetoothImpl.LeScanCallback f$0;

    public /* synthetic */ $$Lambda$BluetoothImpl$LeScanCallback$m1bevS7FLoSbxDjges1RSiTEXMo(BluetoothImpl.LeScanCallback leScanCallback) {
        this.f$0 = leScanCallback;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$onLeScan$0$BluetoothImpl$LeScanCallback((Long) obj);
    }
}

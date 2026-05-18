package net.afterday.compas.engine.influences.BluetoothInfluences;

import android.util.Log;
import net.afterday.compas.core.influences.Influences;
import net.afterday.compas.sensors.Bluetooth.BluetoothScanResult;
import net.afterday.compas.util.Convert;

/* JADX INFO: loaded from: classes.dex */
public class BluetoothInfluenceImpl implements BluetoothInfluence {
    private static final String TAG = "BluetoothInfl";
    private int strength;

    public BluetoothInfluenceImpl(BluetoothScanResult bluetoothScanResult) {
        Log.d(TAG, "***********************");
        this.strength = bluetoothScanResult.getStrength();
    }

    @Override // net.afterday.compas.core.events.Event
    public long getTimestamp() {
        return 0L;
    }

    @Override // net.afterday.compas.core.influences.Influence
    public String getName() {
        return Influences.ARTEFACT;
    }

    @Override // net.afterday.compas.core.influences.Influence
    public String getId() {
        return null;
    }

    @Override // net.afterday.compas.core.influences.Influence
    public boolean affects(int what) {
        return false;
    }

    @Override // net.afterday.compas.core.influences.Influence
    public boolean isDanger() {
        return false;
    }

    @Override // net.afterday.compas.core.influences.Influence
    public double getStrength() {
        return Convert.map(this.strength, -100.0f, 0.0f, 1.0f, 100.0f);
    }

    @Override // net.afterday.compas.core.influences.Influence
    public int getTypeId() {
        return 6;
    }

    public String toString() {
        return "BluetoothInfluence - name: " + getName() + "; strength: " + getStrength();
    }
}

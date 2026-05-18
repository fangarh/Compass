package net.afterday.compas.engine.influences.WifiInfluences;

import android.net.wifi.ScanResult;
import android.util.Log;

/* JADX INFO: loaded from: classes.dex */
public class WifiInfluenceImpl implements WifiInfluence {
    private static final String TAG = "WiFi influence";
    private double multiplier;
    private String name;
    private ScanResult scanResult;
    private int typeId;

    public WifiInfluenceImpl(ScanResult scanResult, String name, int type, double multiplier) {
        this.name = null;
        this.typeId = -1;
        this.multiplier = 1.0d;
        Log.d(TAG, "TypeId = " + type);
        this.scanResult = scanResult;
        this.typeId = type;
        this.name = name;
        this.multiplier = multiplier;
    }

    @Override // net.afterday.compas.core.influences.Influence
    public String getName() {
        return this.name;
    }

    @Override // net.afterday.compas.core.influences.Influence
    public String getId() {
        return this.scanResult.BSSID;
    }

    @Override // net.afterday.compas.core.influences.Influence
    public boolean affects(int what) {
        return false;
    }

    @Override // net.afterday.compas.core.influences.Influence
    public boolean isDanger() {
        int i = this.typeId;
        return (i == 0 || i == 3 || i == 4 || i == 1 || i == 7) ? false : true;
    }

    @Override // net.afterday.compas.core.events.Event
    public long getTimestamp() {
        return 0L;
    }

    @Override // net.afterday.compas.core.influences.Influence
    public double getStrength() {
        return WifiConverter.convert(getTypeId(), this.scanResult.level) * this.multiplier;
    }

    @Override // net.afterday.compas.core.influences.Influence
    public int getTypeId() {
        return this.typeId;
    }

    public String toString() {
        return "WiFiInfluence - name: " + getName() + "; strength: " + getStrength();
    }
}

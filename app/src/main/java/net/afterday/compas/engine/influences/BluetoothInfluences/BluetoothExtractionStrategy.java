package net.afterday.compas.engine.influences.BluetoothInfluences;

import java.util.ArrayList;
import java.util.List;
import net.afterday.compas.engine.influences.InfluenceExtractionStrategy;
import net.afterday.compas.sensors.Bluetooth.BluetoothScanResult;

/* JADX INFO: loaded from: classes.dex */
public class BluetoothExtractionStrategy implements InfluenceExtractionStrategy<List<BluetoothScanResult>, Double> {
    private static final int CONNECTION_COOLDOWN = 3000;
    private static final String TAG = "BluetoothExtractor";
    private List<BluetoothScanResult> emitNext = new ArrayList();
    private long lastReceived;
    private double lastStrength;

    @Override // net.afterday.compas.engine.influences.InfluenceExtractionStrategy
    public /* bridge */ /* synthetic */ Double makeInfluences(List<BluetoothScanResult> list) {
        return makeInfluences2(list);
    }

    private int getIndex(List<BluetoothScanResult> results, BluetoothScanResult bsr) {
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).getName().equals(bsr.getName())) {
                return i;
            }
        }
        return -1;
    }

    /* JADX INFO: renamed from: makeInfluences, reason: avoid collision after fix types in other method */
    public Double makeInfluences2(List<BluetoothScanResult> i) {
        long now = System.currentTimeMillis();
        if (i.isEmpty()) {
            if (this.lastReceived < now - 3000) {
                return Double.valueOf(-9.99999999999999E10d);
            }
            return Double.valueOf(this.lastStrength);
        }
        for (BluetoothScanResult bs : i) {
            if (bs.getStrength() > this.lastStrength) {
                this.lastStrength = bs.getStrength();
                this.lastReceived = bs.getScanTime();
            }
        }
        return Double.valueOf(this.lastStrength);
    }
}

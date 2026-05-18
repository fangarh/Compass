package net.afterday.compas.sensors.Bluetooth;

/* JADX INFO: loaded from: classes.dex */
public class BluetoothScanResultImpl implements BluetoothScanResult {
    private final String address;
    private final int strength;
    private final long time;

    public BluetoothScanResultImpl(String address, int strength, long time) {
        this.address = address;
        this.strength = strength;
        this.time = time;
    }

    @Override // net.afterday.compas.sensors.Bluetooth.BluetoothScanResult
    public String getName() {
        return this.address;
    }

    @Override // net.afterday.compas.sensors.Bluetooth.BluetoothScanResult
    public int getStrength() {
        return this.strength;
    }

    @Override // net.afterday.compas.sensors.Bluetooth.BluetoothScanResult
    public long getScanTime() {
        return this.time;
    }
}

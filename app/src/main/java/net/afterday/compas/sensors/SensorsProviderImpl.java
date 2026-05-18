package net.afterday.compas.sensors;

import android.content.Context;
import net.afterday.compas.sensors.Battery.Battery;
import net.afterday.compas.sensors.Battery.BatteryImpl;
import net.afterday.compas.sensors.Bluetooth.Bluetooth;
import net.afterday.compas.sensors.Bluetooth.BluetoothImpl;
import net.afterday.compas.sensors.Gps.Gps;
import net.afterday.compas.sensors.Gps.GpsImpl;
import net.afterday.compas.sensors.WiFi.WiFi;
import net.afterday.compas.sensors.WiFi.WifiImpl;

/* JADX INFO: loaded from: classes.dex */
public class SensorsProviderImpl implements SensorsProvider {
    private static SensorsProvider instance;
    private Context context;

    private SensorsProviderImpl(Context context) {
        this.context = context;
    }

    public static SensorsProvider initialize(Context context) {
        if (instance == null) {
            instance = new SensorsProviderImpl(context);
        }
        return instance;
    }

    public static SensorsProvider instance() {
        SensorsProvider sensorsProvider = instance;
        if (sensorsProvider == null) {
            throw new IllegalStateException("Sensors provider not initialized");
        }
        return sensorsProvider;
    }

    @Override // net.afterday.compas.sensors.SensorsProvider
    public WiFi getWifiSensor() {
        return new WifiImpl(this.context);
    }

    @Override // net.afterday.compas.sensors.SensorsProvider
    public Battery getBatterySensor() {
        return new BatteryImpl(this.context);
    }

    @Override // net.afterday.compas.sensors.SensorsProvider
    public Bluetooth getBluetoothSensor() {
        return new BluetoothImpl(this.context);
    }

    @Override // net.afterday.compas.sensors.SensorsProvider
    public Gps getGpsSensor() {
        return new GpsImpl(this.context);
    }
}

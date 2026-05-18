package net.afterday.compas.sensors;

import net.afterday.compas.sensors.Battery.Battery;
import net.afterday.compas.sensors.Bluetooth.Bluetooth;
import net.afterday.compas.sensors.Gps.Gps;
import net.afterday.compas.sensors.WiFi.WiFi;

/* JADX INFO: loaded from: classes.dex */
public interface SensorsProvider {
    Battery getBatterySensor();

    Bluetooth getBluetoothSensor();

    Gps getGpsSensor();

    WiFi getWifiSensor();
}

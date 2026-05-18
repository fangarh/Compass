package net.afterday.compas.devices;

import net.afterday.compas.devices.sound.Sound;
import net.afterday.compas.devices.vibro.Vibro;

/* JADX INFO: loaded from: classes.dex */
public interface DeviceProvider {
    Sound getSoundPlayer();

    Vibro getVibrator();
}

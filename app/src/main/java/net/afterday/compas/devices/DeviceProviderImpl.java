package net.afterday.compas.devices;

import android.content.Context;
import net.afterday.compas.devices.sound.Sound;
import net.afterday.compas.devices.vibro.Vibro;
import net.afterday.compas.devices.vibro.VibroImpl;

/* JADX INFO: loaded from: classes.dex */
public class DeviceProviderImpl implements DeviceProvider {
    private Context ctx;
    private Sound sound;
    private Vibro vibro;

    public DeviceProviderImpl(Context ctx) {
        this.ctx = ctx;
    }

    @Override // net.afterday.compas.devices.DeviceProvider
    public Sound getSoundPlayer() {
        if (this.sound == null) {
            this.sound = new Sound(this.ctx);
        }
        return this.sound;
    }

    @Override // net.afterday.compas.devices.DeviceProvider
    public Vibro getVibrator() {
        if (this.vibro == null) {
            this.vibro = new VibroImpl(this.ctx);
        }
        return this.vibro;
    }
}

package net.afterday.compas.devices.vibro;

import net.afterday.compas.core.player.PlayerProps;

/* JADX INFO: loaded from: classes.dex */
public interface Vibro {
    void vibrateAlarm();

    void vibrateDamage(PlayerProps playerProps);

    void vibrateDeath();

    void vibrateHit();

    void vibrateMessage();

    void vibrateTouch();

    void vibrateW();
}

package net.afterday.compas.core;

import net.afterday.compas.core.player.Player;

/* JADX INFO: loaded from: classes.dex */
public interface CountDown {
    void startCountDown(Player.STATE state);

    void stopCountDown();
}

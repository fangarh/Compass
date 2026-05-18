package net.afterday.compas.persistency.player;

import net.afterday.compas.core.player.Player;

/* JADX INFO: loaded from: classes.dex */
public interface PlayerPersistency {
    Player.COMMAND getCommandByCode(String str);

    Player.FRACTION getFractionByCode(String str);
}

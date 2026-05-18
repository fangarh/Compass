package net.afterday.compas.core.events;

import net.afterday.compas.core.inventory.items.Events.ItemAdded;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.core.player.Impacts;
import net.afterday.compas.core.player.Player;

/* JADX INFO: loaded from: classes.dex */
public interface PlayerEventsListener {
    void onFractionChanged(Player.FRACTION fraction, Player.FRACTION fraction2);

    void onImpactsStateChanged(Impacts.STATE state, Impacts.STATE state2);

    void onItemAdded(ItemAdded itemAdded);

    void onItemDropped(Item item);

    void onItemUsed(Item item);

    void onPlayerLevelChanged(int i);

    void onPlayerStateChanged(Player.STATE state, Player.STATE state2);
}

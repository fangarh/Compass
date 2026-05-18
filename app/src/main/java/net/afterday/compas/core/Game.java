package net.afterday.compas.core;

import net.afterday.compas.core.gameState.Frame;
import net.afterday.compas.core.influences.InfluencesPack;
import net.afterday.compas.core.inventory.Inventory;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.core.player.Player;

/* JADX INFO: loaded from: classes.dex */
public interface Game {
    boolean acceptCode(String str);

    Frame acceptInfluences(InfluencesPack influencesPack);

    Inventory getInventory();

    Player getPlayer();

    Frame start();

    Frame useItem(Item item);
}

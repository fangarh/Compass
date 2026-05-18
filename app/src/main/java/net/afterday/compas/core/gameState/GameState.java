package net.afterday.compas.core.gameState;

import java.util.List;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.core.player.Player;

/* JADX INFO: loaded from: classes.dex */
public interface GameState {
    List<Item> getItems();

    Player getPlayer();

    State getState();
}

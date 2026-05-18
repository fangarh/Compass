package net.afterday.compas.core.inventory.items.Events;

import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.core.player.XpChanged;

/* JADX INFO: loaded from: classes.dex */
public interface ItemAdded extends XpChanged {
    Item getItem();
}

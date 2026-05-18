package net.afterday.compas.fragment;

import net.afterday.compas.core.inventory.items.Item;

/* JADX INFO: loaded from: classes.dex */
public interface ItemInfoCallback {
    void onItemDropped(Item item);

    void onItemInfoClosed(Item item);

    void onItemUsed(Item item);
}

package net.afterday.compas.core.userActions;

import net.afterday.compas.core.inventory.items.Item;

/* JADX INFO: loaded from: classes.dex */
public class AddItemAction implements UserAction {
    private Item item;
    private long timestamp = System.currentTimeMillis();

    public AddItemAction(Item item) {
        this.item = item;
    }

    @Override // net.afterday.compas.core.userActions.UserAction
    public String getActionType() {
        return null;
    }

    @Override // net.afterday.compas.core.events.Event
    public long getTimestamp() {
        return 0L;
    }

    public Item getItem() {
        return this.item;
    }
}

package net.afterday.compas.core.userActions;

import java.util.List;
import net.afterday.compas.core.events.EventsPack;
import net.afterday.compas.core.inventory.items.Events.AddItem;
import net.afterday.compas.core.inventory.items.Events.DropItem;
import net.afterday.compas.core.inventory.items.Events.UseItem;

/* JADX INFO: loaded from: classes.dex */
public interface UserActionsPack extends EventsPack {
    List<AddItem> getAddItemEvents();

    List<DropItem> getDropItemEvents();

    List<UseItem> getUseItemEvents();

    boolean hasAddItemEvents();

    boolean hasDropItemEvents();

    boolean hasUseItemEvents();
}

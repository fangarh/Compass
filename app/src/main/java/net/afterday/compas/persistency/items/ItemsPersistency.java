package net.afterday.compas.persistency.items;

import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public interface ItemsPersistency {
    ItemDescriptor getItemForCode(String str);

    Map<Integer, List<ItemDescriptor>> getItemsAddeWithLevel();

    Map<String, ItemDescriptor> getItemsByCode();
}

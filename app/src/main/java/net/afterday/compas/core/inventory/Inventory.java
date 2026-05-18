package net.afterday.compas.core.inventory;

import java.util.List;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.core.player.PlayerProps;
import net.afterday.compas.core.serialization.Jsonable;
import net.afterday.compas.persistency.items.ItemDescriptor;

/* JADX INFO: loaded from: classes.dex */
public interface Inventory extends Jsonable {
    public static final int MAX_ARTIFACTS_COUNT = 5;

    Item addItem(ItemDescriptor itemDescriptor, String str);

    Item consumeArmor(long j);

    Item consumeBooster(long j);

    Item consumeDevice(long j);

    boolean dropItem(Item item);

    Item getActiveArmor();

    Item getActiveBooster();

    Item getActiveDevice();

    List<Item> getArmors();

    double[] getArtifacts();

    List<Item> getBoosters();

    List<Item> getDevices();

    List<Item> getItems();

    boolean hasActiveArmor();

    boolean hasActiveBooster();

    boolean hasActiveDevice();

    boolean hasHealthInstant();

    boolean hasRadiationInstant();

    void setPlayerLevel(int i);

    PlayerProps useItem(Item item, PlayerProps playerProps);
}

package net.afterday.compas.core.inventory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.afterday.compas.R;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.core.inventory.items.ItemImpl;
import net.afterday.compas.core.player.PlayerProps;
import net.afterday.compas.core.serialization.Jsonable;
import net.afterday.compas.core.serialization.Serializer;
import net.afterday.compas.logging.Logger;
import net.afterday.compas.persistency.items.ItemDescriptor;
import net.afterday.compas.persistency.items.ItemsPersistency;

/* JADX INFO: loaded from: classes.dex */
public class InventoryImpl implements Inventory {
    private static final String INVENTORY = "INVENTORY";
    private static final String INVENTORY_ITEMS = "INV_ITEMS";
    private static final String TAG = "InventoryImpl";
    private Item activeArmor;
    private Item activeBooster;
    private Item activeDevice;
    private double[] artifacts = new double[10];
    private JsonArray artifactsO;
    private boolean hasHealthInstant;
    private boolean hasRadInstant;
    private List<Item> inventoryItems;
    private JsonArray inventoryItemsO;
    private Map<Integer, List<Item>> itemsByLevel;
    private ItemsPersistency itemsPersistency;
    private int level;
    private JsonObject o;
    private Serializer serializer;

    public InventoryImpl(ItemsPersistency persistency, Serializer serializer) {
        JsonObject o;
        this.level = 1;
        this.itemsPersistency = persistency;
        this.serializer = serializer;
        this.itemsByLevel = makeItems(persistency.getItemsAddeWithLevel());
        this.artifacts[5] = 0.0d;
        this.inventoryItems = deserializeItems();
        Jsonable io2 = deserializeInventory();
        if (io2 != null) {
            o = io2.toJson();
            if (o.has("level")) {
                this.level = o.get("level").getAsInt();
            }
            if (o.has("activeArmor")) {
                JsonElement e = o.get("activeArmor");
                if (e.isJsonObject()) {
                    this.activeArmor = deserializeItem(e.getAsJsonObject());
                }
            }
            if (o.has("activeBooster")) {
                JsonElement e2 = o.get("activeBooster");
                if (e2.isJsonObject()) {
                    this.activeBooster = deserializeItem(e2.getAsJsonObject());
                }
            }
            if (o.has("activeDevice")) {
                JsonElement e3 = o.get("activeDevice");
                if (e3.isJsonObject()) {
                    this.activeDevice = deserializeItem(e3.getAsJsonObject());
                }
            }
            if (o.has("hasHealthInstant")) {
                this.hasHealthInstant = o.get("hasHealthInstant").getAsBoolean();
            }
            if (o.has("hasRadInstant")) {
                this.hasRadInstant = o.get("hasRadInstant").getAsBoolean();
            }
            if (o.has("artifacts")) {
                deserializeArtifacts(o.get("artifacts").getAsJsonArray());
            } else {
                initArtifacts();
            }
        } else {
            o = new JsonObject();
            o.addProperty("level", Integer.valueOf(this.level));
            o.add("activeArmor", null);
            o.add("activeBooster", null);
            o.add("activeDevice", null);
            o.addProperty("hasHealthInstant", (Boolean) false);
            o.addProperty("hasRadInstant", (Boolean) false);
            initArtifacts();
            o.add("artifacts", this.artifactsO);
        }
        this.o = o;
    }

    private void deserializeArtifacts(JsonArray ja) {
        int jaSize = ja.size();
        if (jaSize != 10) {
            initArtifacts();
            return;
        }
        this.artifactsO = ja;
        for (int i = 0; i < 10; i++) {
            this.artifacts[i] = ja.get(i).getAsDouble();
        }
    }

    private void initArtifacts() {
        this.artifactsO = new JsonArray(10);
        int i = 0;
        while (true) {
            double d = 0.0d;
            if (i < 10) {
                double[] dArr = this.artifacts;
                if (i != 5) {
                    d = 1.0d;
                }
                dArr[i] = d;
                this.artifactsO.add(Double.valueOf(this.artifacts[i]));
                i++;
            } else {
                this.artifacts[5] = 0.0d;
                return;
            }
        }
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public Item addItem(ItemDescriptor itemD, String code) {
        Item item = new ItemImpl(itemD, code);
        if (item.getItemDescriptor().isArtefact()) {
            int artifactsCount = getArtifactsCount();
            if (artifactsCount >= 5) {
                Logger.e(R.string.message_artifact_full);
                return null;
            }
            applyArtifactModifier(item, 1);
            applyArtifactModifier(item, 5);
            applyArtifactModifier(item, 2);
            applyArtifactModifier(item, 0);
            applyArtifactModifier(item, 4);
            applyArtifactModifier(item, 6);
            applyArtifactModifier(item, 3);
            applyArtifactModifier(item, 9);
        }
        if (item.hasModifier(8) && item.getModifier(8) < 0.0d) {
            this.hasRadInstant = true;
            this.o.addProperty("hasRadInstant", (Boolean) true);
        }
        if (item.hasModifier(7) && item.getModifier(7) > 0.0d) {
            this.hasHealthInstant = true;
            this.o.addProperty("hasHealthInstant", (Boolean) true);
        }
        this.inventoryItems.add(item);
        this.serializer.serialize(INVENTORY_ITEMS, item.getId(), item);
        return item;
    }

    private int getArtifactsCount() {
        int count = 0;
        for (Item i : this.inventoryItems) {
            if (i.getItemDescriptor().isArtefact()) {
                count++;
            }
        }
        return count;
    }

    private void applyArtifactModifier(Item i, int modifierId) {
        if (i.hasModifier(modifierId)) {
            if (modifierId == 5) {
                double[] dArr = this.artifacts;
                dArr[modifierId] = dArr[modifierId] + i.getModifier(modifierId);
                this.artifactsO.set(modifierId, new JsonPrimitive((Number) Double.valueOf(this.artifacts[modifierId])));
            } else {
                double[] dArr2 = this.artifacts;
                dArr2[modifierId] = dArr2[modifierId] * i.getModifier(modifierId);
                this.artifactsO.set(modifierId, new JsonPrimitive((Number) Double.valueOf(this.artifacts[modifierId])));
            }
        }
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public boolean hasActiveArmor() {
        Item item = this.activeArmor;
        return (item == null || item.isConsumed()) ? false : true;
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public boolean hasHealthInstant() {
        return this.hasHealthInstant;
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public boolean hasRadiationInstant() {
        return this.hasRadInstant;
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public Item consumeArmor(long delta) {
        Item item = this.activeArmor;
        if (item != null) {
            item.consume(delta);
            this.serializer.serialize(INVENTORY, this);
        }
        return this.activeArmor;
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public Item getActiveArmor() {
        return this.activeArmor;
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public Item getActiveBooster() {
        return this.activeBooster;
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public Item getActiveDevice() {
        return this.activeDevice;
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public void setPlayerLevel(int level) {
        this.level = level;
        this.o.addProperty("level", Integer.valueOf(level));
        this.serializer.serialize(INVENTORY, this);
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public boolean hasActiveBooster() {
        Item item = this.activeBooster;
        return (item == null || item.isConsumed()) ? false : true;
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public Item consumeBooster(long delta) {
        Item item = this.activeBooster;
        if (item != null) {
            item.consume(delta);
            this.serializer.serialize(INVENTORY, this);
        }
        return this.activeBooster;
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public boolean hasActiveDevice() {
        Item item = this.activeDevice;
        return (item == null || item.isConsumed()) ? false : true;
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public Item consumeDevice(long delta) {
        Item item = this.activeDevice;
        if (item != null) {
            item.consume(delta);
            this.serializer.serialize(INVENTORY, this);
        }
        return this.activeDevice;
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public PlayerProps useItem(Item item, PlayerProps playerProps) {
        if (item.getItemDescriptor().isSingleUse()) {
            playerProps = item.modifyProps(playerProps);
            if (item.isConsumed()) {
                removeItem(item);
            }
        } else if (!item.isConsumed() && item.getItemDescriptor().isArmor()) {
            this.activeArmor = item;
            this.o.add("activeArmor", item.toJson());
            item.setActive(true);
            removeItem(item);
            playerProps.setArmorPercents(item.getPercentsLeft());
        } else if (!item.isConsumed() && item.getItemDescriptor().isBooster()) {
            this.activeBooster = item;
            this.o.add("activeBooster", item.toJson());
            item.setActive(true);
            removeItem(item);
            playerProps.setBoosterPercents(item.getPercentsLeft());
        } else if (!item.isConsumed() && item.getItemDescriptor().isDevice()) {
            this.activeDevice = item;
            this.o.add("activeDevice", item.toJson());
            item.setActive(true);
            removeItem(item);
            playerProps.setDevicePercents(item.getPercentsLeft());
        }
        validateInstants();
        this.serializer.serialize(INVENTORY, this);
        return playerProps;
    }

    private List<Item> getItemsByLevel() {
        List<Item> items = new ArrayList<>();
        Iterator<Integer> it = this.itemsByLevel.keySet().iterator();
        while (it.hasNext()) {
            int i = it.next().intValue();
            if (i <= this.level) {
                items.addAll(this.itemsByLevel.get(Integer.valueOf(i)));
            }
        }
        return items;
    }

    private boolean removeItem(Item item) {
        if (this.inventoryItems.contains(item)) {
            boolean removed = this.inventoryItems.remove(item);
            if (removed) {
                this.serializer.remove(INVENTORY_ITEMS, item.getId());
            }
            return removed;
        }
        return false;
    }

    private void validateInstants() {
        boolean hInstant = false;
        boolean rInstant = false;
        for (Item i : this.inventoryItems) {
            if (i.hasModifier(7) && i.getModifier(7) > 0.0d) {
                hInstant = true;
            }
            if (i.hasModifier(8) && i.getModifier(8) < 0.0d) {
                rInstant = true;
            }
        }
        this.hasHealthInstant = hInstant;
        this.o.addProperty("hasHealthInstant", Boolean.valueOf(hInstant));
        this.hasRadInstant = rInstant;
        this.o.addProperty("hasRadInstant", Boolean.valueOf(hInstant));
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public boolean dropItem(Item item) {
        boolean removed = removeItem(item);
        if (item.getItemDescriptor().isArtefact()) {
            if (item.hasModifier(5)) {
                double[] dArr = this.artifacts;
                dArr[5] = dArr[5] - item.getModifier(5);
                this.artifactsO.set(5, new JsonPrimitive((Number) Double.valueOf(this.artifacts[5])));
            }
            if (item.hasModifier(1)) {
                double[] dArr2 = this.artifacts;
                dArr2[1] = dArr2[1] / item.getModifier(1);
                this.artifactsO.set(1, new JsonPrimitive((Number) Double.valueOf(this.artifacts[1])));
            }
            if (item.hasModifier(3)) {
                double[] dArr3 = this.artifacts;
                dArr3[3] = dArr3[3] / item.getModifier(3);
                this.artifactsO.set(3, new JsonPrimitive((Number) Double.valueOf(this.artifacts[3])));
            }
            if (item.hasModifier(9)) {
                double[] dArr4 = this.artifacts;
                dArr4[9] = dArr4[9] / item.getModifier(9);
                this.artifactsO.set(9, new JsonPrimitive((Number) Double.valueOf(this.artifacts[9])));
            }
            if (item.hasModifier(2)) {
                double[] dArr5 = this.artifacts;
                dArr5[2] = dArr5[2] / item.getModifier(2);
                this.artifactsO.set(2, new JsonPrimitive((Number) Double.valueOf(this.artifacts[2])));
            }
            if (item.hasModifier(4)) {
                double[] dArr6 = this.artifacts;
                dArr6[4] = dArr6[4] / item.getModifier(4);
                this.artifactsO.set(4, new JsonPrimitive((Number) Double.valueOf(this.artifacts[4])));
            }
            if (item.hasModifier(6)) {
                double[] dArr7 = this.artifacts;
                dArr7[6] = dArr7[6] / item.getModifier(6);
                this.artifactsO.set(6, new JsonPrimitive((Number) Double.valueOf(this.artifacts[6])));
            }
            if (item.hasModifier(0)) {
                double[] dArr8 = this.artifacts;
                dArr8[0] = dArr8[0] / item.getModifier(0);
                this.artifactsO.set(0, new JsonPrimitive((Number) Double.valueOf(this.artifacts[0])));
            }
        }
        validateInstants();
        this.serializer.serialize(INVENTORY, this);
        return removed;
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public List<Item> getItems() {
        List<Item> allItems = new ArrayList<>();
        List<Item> items = new ArrayList<>();
        List<Item> artifacts = new ArrayList<>();
        for (Item i : this.inventoryItems) {
            if (i.getItemDescriptor().isArtefact()) {
                artifacts.add(i);
            } else {
                items.add(i);
            }
        }
        allItems.addAll(artifacts);
        allItems.addAll(getItemsByLevel());
        allItems.addAll(items);
        return allItems;
    }

    private Map<Integer, List<Item>> makeItems(Map<Integer, List<ItemDescriptor>> descriptors) {
        Map<Integer, List<Item>> itemsByLevel = new HashMap<>();
        Iterator<Integer> it = descriptors.keySet().iterator();
        while (it.hasNext()) {
            int level = it.next().intValue();
            List<Item> l = new ArrayList<>();
            for (ItemDescriptor d : descriptors.get(Integer.valueOf(level))) {
                l.add(new ItemImpl(d));
            }
            itemsByLevel.put(Integer.valueOf(level), l);
        }
        return itemsByLevel;
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public List<Item> getBoosters() {
        List<Item> boosters = new ArrayList<>();
        for (Item i : this.inventoryItems) {
            if (i.getItemDescriptor().isBooster()) {
                boosters.add(i);
            }
        }
        return boosters;
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public List<Item> getDevices() {
        List<Item> devices = new ArrayList<>();
        for (Item i : this.inventoryItems) {
            if (i.getItemDescriptor().isDevice()) {
                devices.add(i);
            }
        }
        return devices;
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public List<Item> getArmors() {
        List<Item> armors = new ArrayList<>();
        for (Item i : this.inventoryItems) {
            if (i.getItemDescriptor().isArmor()) {
                armors.add(i);
            }
        }
        return armors;
    }

    @Override // net.afterday.compas.core.inventory.Inventory
    public double[] getArtifacts() {
        return this.artifacts;
    }

    @Override // net.afterday.compas.core.serialization.Jsonable
    public JsonObject toJson() {
        return this.o;
    }

    private List<Item> deserializeItems() {
        List<Item> items = new ArrayList<>();
        List<Jsonable> jsonables = this.serializer.deserializeList(INVENTORY_ITEMS);
        for (Jsonable j : jsonables) {
            Item i = deserializeItem(j.toJson());
            if (i != null) {
                items.add(i);
            }
        }
        return items;
    }

    private Item deserializeItem(JsonObject jo) {
        if (jo == null || !jo.has("code")) {
            return null;
        }
        String code = jo.get("code").getAsString();
        ItemDescriptor iDesc = this.itemsPersistency.getItemForCode(code);
        if (iDesc == null) {
            return null;
        }
        return new ItemImpl(jo, iDesc);
    }

    private Jsonable deserializeInventory() {
        return this.serializer.deserialize(INVENTORY);
    }
}

package net.afterday.compas.core.inventory.items;

import com.google.gson.JsonObject;
import java.util.UUID;
import net.afterday.compas.core.player.PlayerProps;
import net.afterday.compas.db.SQLiteHelper;
import net.afterday.compas.persistency.items.ItemDescriptor;

/* JADX INFO: loaded from: classes.dex */
public class ItemImpl implements Item {
    private static final String TAG = "ItemImpl";
    private String code;
    private ItemDescriptor descriptor;
    private String id;
    private boolean isActive;
    private boolean isConsumed;
    private long left;
    private double[] modifiers;
    private JsonObject o;

    public ItemImpl(ItemDescriptor descriptor) {
        this.o = new JsonObject();
        this.isConsumed = false;
        this.left = 0L;
        this.isActive = false;
        this.descriptor = descriptor;
        this.modifiers = descriptor.getModifiers();
        this.left = descriptor.getDuration();
        this.id = UUID.randomUUID().toString();
    }

    public ItemImpl(ItemDescriptor descriptor, String code) {
        this.o = new JsonObject();
        this.isConsumed = false;
        this.left = 0L;
        this.isActive = false;
        this.descriptor = descriptor;
        this.modifiers = descriptor.getModifiers();
        this.left = descriptor.getDuration();
        this.id = UUID.randomUUID().toString();
        this.code = code;
        this.o.addProperty(SQLiteHelper.COLUMN_ID, this.id);
        this.o.addProperty("left", Long.valueOf(this.left));
        this.o.addProperty("isActive", Boolean.valueOf(this.isActive));
        this.o.addProperty("isConsumed", Boolean.valueOf(this.isConsumed));
        this.o.addProperty("code", code);
    }

    public ItemImpl(JsonObject o, ItemDescriptor descriptor) {
        this.o = new JsonObject();
        this.isConsumed = false;
        this.left = 0L;
        this.isActive = false;
        this.descriptor = descriptor;
        this.modifiers = descriptor.getModifiers();
        if (o.has(SQLiteHelper.COLUMN_ID)) {
            this.id = o.get(SQLiteHelper.COLUMN_ID).getAsString();
        }
        if (o.has("left")) {
            this.left = o.get("left").getAsLong();
        }
        if (o.has("isActive")) {
            this.isActive = o.get("isActive").getAsBoolean();
        }
        if (o.has("isConsumed")) {
            this.isConsumed = o.get("isConsumed").getAsBoolean();
        }
        if (o.has("code")) {
            this.code = o.get("code").getAsString();
        }
        this.o = o;
    }

    @Override // net.afterday.compas.core.inventory.items.Item
    public boolean hasModifier(int modifierType) {
        double[] dArr = this.modifiers;
        if (dArr.length > modifierType) {
            double m = dArr[modifierType];
            if (m != -9.9999999E7d) {
                return true;
            }
            return false;
        }
        return false;
    }

    @Override // net.afterday.compas.core.inventory.items.Item
    public double getModifier(int modifierType) {
        double[] dArr = this.modifiers;
        if (dArr.length > modifierType) {
            return dArr[modifierType];
        }
        return -9.9999999E7d;
    }

    @Override // net.afterday.compas.core.inventory.items.Item
    public boolean isActive() {
        return this.isActive;
    }

    @Override // net.afterday.compas.core.inventory.items.Item
    public void setActive(boolean isActive) {
        this.isActive = isActive;
        this.o.addProperty("isActive", Boolean.valueOf(isActive));
    }

    @Override // net.afterday.compas.core.inventory.items.Item
    public boolean isConsumed() {
        if (this.descriptor.isSingleUse()) {
            return this.isConsumed;
        }
        return this.descriptor.getDuration() >= 0 && this.left <= 0;
    }

    @Override // net.afterday.compas.core.inventory.items.Item
    public void consume(long time) {
        if (this.descriptor.getDuration() < 0) {
            return;
        }
        this.left -= time;
        this.o.addProperty("left", Long.valueOf(this.left));
    }

    @Override // net.afterday.compas.core.inventory.items.Item
    public int getPercentsLeft() {
        if (this.descriptor.getDuration() < 0 || this.left == this.descriptor.getDuration()) {
            return 100;
        }
        return (int) ((this.left * 100) / Math.max(this.descriptor.getDuration(), 1L));
    }

    @Override // net.afterday.compas.core.inventory.items.Item
    public PlayerProps modifyProps(PlayerProps playerProps) {
        if (!this.isConsumed && this.descriptor.isSingleUse()) {
            if (hasModifier(7)) {
                playerProps.addHealth(getModifier(7));
            }
            if (hasModifier(8)) {
                playerProps.addRadiation(getModifier(8));
            }
            this.isConsumed = true;
            this.o.addProperty("isConsumed", Boolean.valueOf(this.isConsumed));
        }
        return playerProps;
    }

    @Override // net.afterday.compas.core.inventory.items.Item
    public String getCode() {
        return this.code;
    }

    @Override // net.afterday.compas.core.inventory.items.Item
    public PlayerProps modifyProps(PlayerProps playerProps, long delta) {
        this.left -= delta;
        this.o.addProperty("left", Long.valueOf(this.left));
        return playerProps;
    }

    @Override // net.afterday.compas.core.inventory.items.Item
    public String getId() {
        return this.id;
    }

    @Override // net.afterday.compas.core.inventory.items.Item
    public ItemDescriptor getItemDescriptor() {
        return this.descriptor;
    }

    public String toString() {
        new JsonObject();
        return "";
    }

    @Override // net.afterday.compas.core.serialization.Jsonable
    public JsonObject toJson() {
        return this.o;
    }
}

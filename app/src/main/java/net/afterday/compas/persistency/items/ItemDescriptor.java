package net.afterday.compas.persistency.items;

import net.afterday.compas.core.inventory.items.Item;

/* JADX INFO: loaded from: classes.dex */
public interface ItemDescriptor {
    public static final double NULL_MODIFIER = -9.9999999E7d;

    Item.CATEGORY getCategory();

    String getDescription();

    int getDescriptionId();

    long getDuration();

    int getImage();

    double[] getModifiers();

    String getName();

    int getNameId();

    int getXpPoints();

    boolean isArmor();

    boolean isArtefact();

    boolean isBooster();

    boolean isConsumable();

    boolean isDevice();

    boolean isDropable();

    boolean isSingleUse();

    boolean isUsable();
}

package net.afterday.compas.core.player;

import net.afterday.compas.core.inventory.items.Item;

/* JADX INFO: loaded from: classes.dex */
public interface Impacts {

    public enum STATE {
        HEALING,
        CLEAR,
        DAMAGE,
        DEAD
    }

    void armorImpact(Item item);

    void artifactsImpact(double[] dArr);

    void boosterImpact(Item item);

    STATE getState();

    void itemImpact(Item item);
}

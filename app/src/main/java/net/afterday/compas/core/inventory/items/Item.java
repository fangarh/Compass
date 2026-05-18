package net.afterday.compas.core.inventory.items;

import net.afterday.compas.core.player.PlayerProps;
import net.afterday.compas.core.serialization.Jsonable;
import net.afterday.compas.persistency.items.ItemDescriptor;

/* JADX INFO: loaded from: classes.dex */
public interface Item extends Jsonable {
    public static final int ALL = 99;
    public static final int ANOMALY_MODIFIER = 2;
    public static final int ANTIRADS = 7;
    public static final int ARMOR = 3;
    public static final int ARTIFACTS = 1;
    public static final int ARTIFACT_MODIFIERS_COUNT = 10;
    public static final int BOOSTER = 2;
    public static final int BURER_MODIFIER = 4;
    public static final int CONTROLLER_MODIFIER = 6;
    public static final int DEVICES = 9;
    public static final int FOOD = 8;
    public static final int HABAR = 5;
    public static final int HEALTH_INSTANT = 7;
    public static final int HEALTH_MODIFIER = 0;
    public static final int MEDKITS = 4;
    public static final int MENTAL_MODIFIER = 3;
    public static final int MODIFIERS_COUNT = 10;
    public static final int MONOLITH_MODIFIER = 9;
    public static final int RADIATION_EMMITER = 5;
    public static final int RADIATION_INSTANT = 8;
    public static final int RADIATION_MODIFIER = 1;
    public static final int UPGRADES = 6;
    public static final int WEAPONS = 0;

    void consume(long j);

    String getCode();

    String getId();

    ItemDescriptor getItemDescriptor();

    double getModifier(int i);

    int getPercentsLeft();

    boolean hasModifier(int i);

    boolean isActive();

    boolean isConsumed();

    PlayerProps modifyProps(PlayerProps playerProps);

    PlayerProps modifyProps(PlayerProps playerProps, long j);

    void setActive(boolean z);

    public enum CATEGORY {
        WEAPONS(0),
        ARTIFACTS(1),
        BOOSTERS(2),
        ARMORS(3),
        MEDKITS(4),
        HABAR(5),
        UPGRADES(6),
        ANTIRADS(7),
        FOOD(8),
        DEVICES(9);

        private int id;

        CATEGORY(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }
    }
}

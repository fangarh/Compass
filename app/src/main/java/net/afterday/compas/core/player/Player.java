package net.afterday.compas.core.player;

import net.afterday.compas.core.events.PlayerEventsListener;
import net.afterday.compas.core.gameState.Frame;
import net.afterday.compas.core.inventory.Inventory;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.core.serialization.Jsonable;
import net.afterday.compas.persistency.items.ItemDescriptor;

/* JADX INFO: loaded from: classes.dex */
public interface Player extends Jsonable {
    public static final int ABDUCTED = 2;
    public static final int ALIVE = 1;
    public static final int CONTROLLED = 3;
    public static final int DEAD = 4;
    public static final int INSTANT_DEATH = 1;
    public static final long MIN1 = 60;
    public static final long MIN15 = 900;
    public static final long MIN2 = 120;
    public static final long MIN3 = 180;
    public static final long MIN30 = 1800;
    public static final long MIN5 = 300;
    public static final long MIN60 = 3600;
    public static final long MIN7 = 420;
    public static final int NON_HUMAN = 2;
    public static final long SEC30 = 30;
    public static final int SUICIDE_NOT_ALLOWED = 4;
    public static final int W_ABDUCTED = 3;
    public static final int ZOMBIFIED = 2;

    public enum COMMAND {
        REVIVE,
        KILL
    }

    boolean addItem(ItemDescriptor itemDescriptor, String str);

    void addPlayerEventsListener(PlayerEventsListener playerEventsListener);

    boolean dropItem(Item item);

    Inventory getInventory();

    PlayerProps getPlayerProps();

    boolean reborn();

    boolean setFraction(FRACTION fraction);

    Frame setState(STATE state);

    Frame useItem(Item item);

    public enum STATE {
        ALIVE(1, 3),
        DEAD_CONTROLLER(4),
        DEAD_ANOMALY(4),
        DEAD_RADIATION(4),
        DEAD_EMISSION(4),
        DEAD_BURER(4),
        DEAD_MENTAL(4),
        CONTROLLED(4, 1),
        MENTALLED(4, 1),
        W_CONTROLLED(4),
        W_MENTALLED(4),
        W_DEAD_BURER(4),
        W_DEAD_RADIATION(4),
        W_DEAD_ANOMALY(4),
        W_ABDUCTED(4, 2),
        ABDUCTED(1, 1);

        private final int code;
        private final int suicideType;

        STATE(int code, int suicideType) {
            this.code = code;
            this.suicideType = suicideType;
        }

        STATE(int code) {
            this.code = code;
            this.suicideType = 4;
        }

        public int getCode() {
            return this.code;
        }

        public int getSuicideType() {
            return this.suicideType;
        }

        public long getWaitTime() {
            switch (this) {
                case ALIVE:
                    return 0L;
                case DEAD_CONTROLLER:
                    return 1L;
                case DEAD_ANOMALY:
                    return 60L;
                case DEAD_RADIATION:
                    return 0L;
                case DEAD_BURER:
                    return 0L;
                case DEAD_MENTAL:
                    return 60L;
                case CONTROLLED:
                    return Player.MIN60;
                case MENTALLED:
                    return Player.MIN30;
                case W_MENTALLED:
                    return 60L;
                case W_CONTROLLED:
                    return 60L;
                case W_DEAD_BURER:
                    return 300L;
                case W_DEAD_RADIATION:
                    return 300L;
                case W_DEAD_ANOMALY:
                    return 300L;
                case W_ABDUCTED:
                    return 300L;
                case ABDUCTED:
                    return Player.MIN60;
                case DEAD_EMISSION:
                    return 60L;
                default:
                    return 300L;
            }
        }

        @Override // java.lang.Enum
        public String toString() {
            switch (this) {
                case ALIVE:
                    return "ALIVE";
                case DEAD_CONTROLLER:
                    return "DEAD_CONTROLLER";
                case DEAD_ANOMALY:
                    return "DEAD_ANOMALY";
                case DEAD_RADIATION:
                    return "DEAD_RADIATION";
                case DEAD_BURER:
                    return "DEAD_BURER";
                case DEAD_MENTAL:
                    return "DEAD_MENTAL";
                case CONTROLLED:
                    return "CONTROLLED";
                case MENTALLED:
                    return "MENTALLED";
                case W_MENTALLED:
                    return "W_MENTALLED";
                case W_CONTROLLED:
                    return "W_CONTROLLED";
                case W_DEAD_BURER:
                    return "W_DEAD_BURER";
                case W_DEAD_RADIATION:
                    return "W_DEAD_RADIATION";
                case W_DEAD_ANOMALY:
                    return "W_DEAD_ANOMALY";
                case W_ABDUCTED:
                    return "W_ABDUCTED";
                case ABDUCTED:
                    return "ABDUCTED";
                case DEAD_EMISSION:
                    return "DEAD_EMISSION";
                default:
                    return "Unknown player state!";
            }
        }

        public static STATE fromString(String value) {
            if (value == null) {
                return null;
            }
            try {
                return STATE.valueOf(value);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public enum FRACTION {
        STALKER(0),
        MONOLITH(1),
        GAMEMASTER(2),
        DARKEN(3);

        FRACTION(int id) {
        }

        @Override // java.lang.Enum
        public String toString() {
            int i = AnonymousClass1.$SwitchMap$net$afterday$compas$core$player$Player$FRACTION[ordinal()];
            if (i == 1) {
                return "STALKER";
            }
            if (i == 2) {
                return "MONOLITH";
            }
            if (i == 3) {
                return "GAMEMASTER";
            }
            if (i == 4) {
                return "DARKEN";
            }
            return "UnknownFraction";
        }

        public static FRACTION fromString(String value) {
            if (value == null) {
                return null;
            }
            try {
                return FRACTION.valueOf(value);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /* JADX INFO: renamed from: net.afterday.compas.core.player.Player$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$net$afterday$compas$core$player$Player$FRACTION = new int[FRACTION.values().length];
        static final /* synthetic */ int[] $SwitchMap$net$afterday$compas$core$player$Player$STATE;

        static {
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$FRACTION[FRACTION.STALKER.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$FRACTION[FRACTION.MONOLITH.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$FRACTION[FRACTION.GAMEMASTER.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$FRACTION[FRACTION.DARKEN.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            $SwitchMap$net$afterday$compas$core$player$Player$STATE = new int[STATE.values().length];
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[STATE.ALIVE.ordinal()] = 1;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[STATE.DEAD_CONTROLLER.ordinal()] = 2;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[STATE.DEAD_ANOMALY.ordinal()] = 3;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[STATE.DEAD_RADIATION.ordinal()] = 4;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[STATE.DEAD_BURER.ordinal()] = 5;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[STATE.DEAD_MENTAL.ordinal()] = 6;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[STATE.CONTROLLED.ordinal()] = 7;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[STATE.MENTALLED.ordinal()] = 8;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[STATE.W_MENTALLED.ordinal()] = 9;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[STATE.W_CONTROLLED.ordinal()] = 10;
            } catch (NoSuchFieldError e14) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[STATE.W_DEAD_BURER.ordinal()] = 11;
            } catch (NoSuchFieldError e15) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[STATE.W_DEAD_RADIATION.ordinal()] = 12;
            } catch (NoSuchFieldError e16) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[STATE.W_DEAD_ANOMALY.ordinal()] = 13;
            } catch (NoSuchFieldError e17) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[STATE.W_ABDUCTED.ordinal()] = 14;
            } catch (NoSuchFieldError e18) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[STATE.ABDUCTED.ordinal()] = 15;
            } catch (NoSuchFieldError e19) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$STATE[STATE.DEAD_EMISSION.ordinal()] = 16;
            } catch (NoSuchFieldError e20) {
            }
        }
    }
}

package net.afterday.compas.core.influences;

import net.afterday.compas.core.events.Event;

/* JADX INFO: loaded from: classes.dex */
public interface Influence extends Event {
    public static final int ANOMALY = 1;
    public static final double ANOMALY_PEAK = 16.0d;
    public static final int ARTEFACT = 6;
    public static final int BURER = 3;
    public static final double BURER_PEAK = 16.0d;
    public static final int CONTROLLER = 4;
    public static final double CONTROLLER_PEAK = 16.0d;
    public static final int EMISSION = 8;
    public static final int HEALTH = 5;
    public static final int INFLUENCE_COUNT = 9;
    public static final double MAX = 7.0d;
    public static final int MAX_SATELLITES = 8;
    public static final double MED = 1.0d;
    public static final int MENTAL = 2;
    public static final double MENTAL_PEAK = 16.0d;
    public static final double MIN = 0.1d;
    public static final int MONOLITH = 7;
    public static final double MONOLITH_PEAK = 16.0d;
    public static final double NULL = -9.99999999999999E10d;
    public static final double PEAK = 16.0d;
    public static final int RADIATION = 0;
    public static final double RADIATION_PEAK = 16.0d;

    public enum SOURCE {
        WIFI,
        BLUETOOTH,
        ALL
    }

    boolean affects(int i);

    String getId();

    String getName();

    double getStrength();

    int getTypeId();

    boolean isDanger();
}

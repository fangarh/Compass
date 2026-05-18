package net.afterday.compas.core.influences;

import net.afterday.compas.core.events.EventsPack;

/* JADX INFO: loaded from: classes.dex */
public interface InfluencesPack extends EventsPack {
    public static final int BLUETOOTH = 1;
    public static final int WIFI = 0;

    void addInfluence(int i, double d);

    long creationTime();

    double getInfluence(int i);

    double[] getInfluences();

    int getSource();

    boolean inDanger();

    boolean influencedBy(int i);

    boolean isClear();

    boolean isEmission();

    void setEmission(boolean z);
}

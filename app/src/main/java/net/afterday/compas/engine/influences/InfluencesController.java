package net.afterday.compas.engine.influences;

import net.afterday.compas.core.influences.InfluencesPack;

/* JADX INFO: loaded from: classes.dex */
public interface InfluencesController extends InfluenceProvider<InfluencesPack> {
    void start(int i);

    void startEmission();

    void stop(int i);

    void stopEmission();
}

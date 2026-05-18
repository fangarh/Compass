package net.afterday.compas.core.influences;

import java.util.Calendar;

/* JADX INFO: loaded from: classes.dex */
public interface Emission {
    int duration();

    Calendar getStartTime();

    boolean isFake();

    int notifyBefore();
}

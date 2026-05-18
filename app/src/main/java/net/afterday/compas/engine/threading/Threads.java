package net.afterday.compas.engine.threading;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import java.util.concurrent.Executors;

/* JADX INFO: loaded from: classes.dex */
public class Threads {
    private static Scheduler computation;

    public static Scheduler computation() {
        if (computation == null) {
            computation = Schedulers.from(Executors.newSingleThreadExecutor());
        }
        return computation;
    }
}

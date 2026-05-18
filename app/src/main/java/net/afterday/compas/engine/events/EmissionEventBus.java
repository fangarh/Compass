package net.afterday.compas.engine.events;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/* JADX INFO: loaded from: classes.dex */
public class EmissionEventBus {
    public static final int WARN_BEFORE = 240;
    private static final Subject<Boolean> emissionActive = BehaviorSubject.createDefault(false);
    private static final Subject<Integer> emissionWarning = PublishSubject.create();
    private static final Subject<Integer> fakeEmissions = PublishSubject.create();
    private static EmissionEventBus instance;
    private Disposable waitingForEmission;

    public static EmissionEventBus instance() {
        if (instance == null) {
            instance = new EmissionEventBus();
        }
        return instance;
    }

    public void setEmissionActive(boolean emissionActive2) {
        emissionActive.onNext(Boolean.valueOf(emissionActive2));
    }

    public void emissionWillStart(int startsAfter) {
        emissionWarning.onNext(Integer.valueOf(startsAfter));
    }

    public void fakeEmission() {
        fakeEmissions.onNext(1);
    }

    public Observable<Boolean> getEmissionStateStream() {
        return emissionActive;
    }

    public Observable<Integer> getEmissionWarnings() {
        return emissionWarning;
    }

    public Observable<Integer> getFakeEmissions() {
        return fakeEmissions;
    }
}

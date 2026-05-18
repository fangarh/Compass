package net.afterday.compas.engine;

import io.reactivex.functions.Predicate;

/* JADX INFO: renamed from: net.afterday.compas.engine.-$$Lambda$Engine$T9F15mCkGbSHf7BlA1zcOU65sCM, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Engine$T9F15mCkGbSHf7BlA1zcOU65sCM implements Predicate {
    private final /* synthetic */ long f$0;

    public /* synthetic */ $$Lambda$Engine$T9F15mCkGbSHf7BlA1zcOU65sCM(long j) {
        this.f$0 = j;
    }

    @Override // io.reactivex.functions.Predicate
    public final boolean test(Object obj) {
        return Engine.lambda$countUntil$25(this.f$0, (Long) obj);
    }
}

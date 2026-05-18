package net.afterday.compas.engine;

import io.reactivex.functions.Predicate;

/* JADX INFO: renamed from: net.afterday.compas.engine.-$$Lambda$Engine$lmWpPli2Kk7WwnBRsMMGi-OXX6g, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Engine$lmWpPli2Kk7WwnBRsMMGiOXX6g implements Predicate {
    private final /* synthetic */ long f$0;

    public /* synthetic */ $$Lambda$Engine$lmWpPli2Kk7WwnBRsMMGiOXX6g(long j) {
        this.f$0 = j;
    }

    @Override // io.reactivex.functions.Predicate
    public final boolean test(Object obj) {
        return Engine.lambda$countUntil$26(this.f$0, (Long) obj);
    }
}

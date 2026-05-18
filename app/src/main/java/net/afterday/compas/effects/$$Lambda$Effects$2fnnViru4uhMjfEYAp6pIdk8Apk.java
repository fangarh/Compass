package net.afterday.compas.effects;

import io.reactivex.functions.Consumer;

/* JADX INFO: renamed from: net.afterday.compas.effects.-$$Lambda$Effects$2fnnViru4uhMjfEYAp6pIdk8Apk, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Effects$2fnnViru4uhMjfEYAp6pIdk8Apk implements Consumer {
    private final /* synthetic */ Effects f$0;

    public /* synthetic */ $$Lambda$Effects$2fnnViru4uhMjfEYAp6pIdk8Apk(Effects effects) {
        this.f$0 = effects;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$setPlayerLevelStream$11$Effects((Integer) obj);
    }
}

package net.afterday.compas.engine;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.player.Player;

/* JADX INFO: renamed from: net.afterday.compas.engine.-$$Lambda$Engine$IzKCYl5QkAz87UpbQaW8REz8bSg, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Engine$IzKCYl5QkAz87UpbQaW8REz8bSg implements Consumer {
    private final /* synthetic */ Engine f$0;
    private final /* synthetic */ long f$1;
    private final /* synthetic */ Player.STATE f$2;

    public /* synthetic */ $$Lambda$Engine$IzKCYl5QkAz87UpbQaW8REz8bSg(Engine engine, long j, Player.STATE state) {
        this.f$0 = engine;
        this.f$1 = j;
        this.f$2 = state;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$makeCountDownForStates$23$Engine(this.f$1, this.f$2, (Long) obj);
    }
}

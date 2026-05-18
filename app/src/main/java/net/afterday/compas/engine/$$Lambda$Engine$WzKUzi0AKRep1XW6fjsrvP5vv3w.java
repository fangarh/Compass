package net.afterday.compas.engine;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.player.Player;

/* JADX INFO: renamed from: net.afterday.compas.engine.-$$Lambda$Engine$WzKUzi0AKRep1XW6fjsrvP5vv3w, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Engine$WzKUzi0AKRep1XW6fjsrvP5vv3w implements Consumer {
    private final /* synthetic */ Engine f$0;
    private final /* synthetic */ long f$1;
    private final /* synthetic */ Player.STATE f$2;
    private final /* synthetic */ long f$3;

    public /* synthetic */ $$Lambda$Engine$WzKUzi0AKRep1XW6fjsrvP5vv3w(Engine engine, long j, Player.STATE state, long j2) {
        this.f$0 = engine;
        this.f$1 = j;
        this.f$2 = state;
        this.f$3 = j2;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$makeCountDownForStates$24$Engine(this.f$1, this.f$2, this.f$3, (Long) obj);
    }
}

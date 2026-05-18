package net.afterday.compas.engine;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.Game;

/* JADX INFO: renamed from: net.afterday.compas.engine.-$$Lambda$Engine$d8dqkwfFTlvkVfP_nzgZdFKPjHI, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Engine$d8dqkwfFTlvkVfP_nzgZdFKPjHI implements Consumer {
    private final /* synthetic */ Game f$0;

    public /* synthetic */ $$Lambda$Engine$d8dqkwfFTlvkVfP_nzgZdFKPjHI(Game game) {
        this.f$0 = game;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        Engine.lambda$startGame$8(this.f$0, (String) obj);
    }
}

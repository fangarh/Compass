package net.afterday.compas.engine;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.player.Player;

/* JADX INFO: renamed from: net.afterday.compas.engine.-$$Lambda$Engine$a9BgvSDiJxtPc5XwxpIlC9QsfgE, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Engine$a9BgvSDiJxtPc5XwxpIlC9QsfgE implements Consumer {
    private final /* synthetic */ Engine f$0;

    public /* synthetic */ $$Lambda$Engine$a9BgvSDiJxtPc5XwxpIlC9QsfgE(Engine engine) {
        this.f$0 = engine;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$setupSuicides$22$Engine((Player.STATE) obj);
    }
}

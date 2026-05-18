package net.afterday.compas.engine;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.Game;
import net.afterday.compas.core.inventory.items.Item;

/* JADX INFO: renamed from: net.afterday.compas.engine.-$$Lambda$Engine$saDmpyWoyMUtJFkJL9p82hVrL1Q, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Engine$saDmpyWoyMUtJFkJL9p82hVrL1Q implements Consumer {
    private final /* synthetic */ Engine f$0;
    private final /* synthetic */ Game f$1;

    public /* synthetic */ $$Lambda$Engine$saDmpyWoyMUtJFkJL9p82hVrL1Q(Engine engine, Game game) {
        this.f$0 = engine;
        this.f$1 = game;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$startGame$11$Engine(this.f$1, (Item) obj);
    }
}

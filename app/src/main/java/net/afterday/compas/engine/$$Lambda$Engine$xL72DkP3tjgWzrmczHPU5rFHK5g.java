package net.afterday.compas.engine;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.Game;
import net.afterday.compas.core.inventory.items.Item;

/* JADX INFO: renamed from: net.afterday.compas.engine.-$$Lambda$Engine$xL72DkP3tjgWzrmczHPU5rFHK5g, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Engine$xL72DkP3tjgWzrmczHPU5rFHK5g implements Consumer {
    private final /* synthetic */ Game f$0;

    public /* synthetic */ $$Lambda$Engine$xL72DkP3tjgWzrmczHPU5rFHK5g(Game game) {
        this.f$0 = game;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        Engine.lambda$startGame$9(this.f$0, (Item) obj);
    }
}

package net.afterday.compas.effects;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.inventory.items.Item;

/* JADX INFO: renamed from: net.afterday.compas.effects.-$$Lambda$Effects$Z4rx_K76D6UK5eyh2JaG6MvkNo4, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Effects$Z4rx_K76D6UK5eyh2JaG6MvkNo4 implements Consumer {
    private final /* synthetic */ Effects f$0;

    public /* synthetic */ $$Lambda$Effects$Z4rx_K76D6UK5eyh2JaG6MvkNo4(Effects effects) {
        this.f$0 = effects;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$new$2$Effects((Item) obj);
    }
}

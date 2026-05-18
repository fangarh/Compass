package net.afterday.compas.effects;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.inventory.items.Item;

/* JADX INFO: renamed from: net.afterday.compas.effects.-$$Lambda$Effects$jrSFN-E8UEAWxElyolB6f4N4b6A, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Effects$jrSFNE8UEAWxElyolB6f4N4b6A implements Consumer {
    private final /* synthetic */ Effects f$0;

    public /* synthetic */ $$Lambda$Effects$jrSFNE8UEAWxElyolB6f4N4b6A(Effects effects) {
        this.f$0 = effects;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$new$3$Effects((Item) obj);
    }
}

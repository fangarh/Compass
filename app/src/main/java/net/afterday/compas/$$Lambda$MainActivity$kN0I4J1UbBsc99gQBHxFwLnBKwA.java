package net.afterday.compas;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.inventory.items.Item;

/* JADX INFO: renamed from: net.afterday.compas.-$$Lambda$MainActivity$kN0I4J1UbBsc99gQBHxFwLnBKwA, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$MainActivity$kN0I4J1UbBsc99gQBHxFwLnBKwA implements Consumer {
    private final /* synthetic */ MainActivity f$0;

    public /* synthetic */ $$Lambda$MainActivity$kN0I4J1UbBsc99gQBHxFwLnBKwA(MainActivity mainActivity) {
        this.f$0 = mainActivity;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$setupListeners$9$MainActivity((Item) obj);
    }
}

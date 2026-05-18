package net.afterday.compas.fragment;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.inventory.items.Item;

/* JADX INFO: renamed from: net.afterday.compas.fragment.-$$Lambda$InventoryFragment$rVng5uWEvbEI9a7L4N5mNZBg3Lc, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$InventoryFragment$rVng5uWEvbEI9a7L4N5mNZBg3Lc implements Consumer {
    private final /* synthetic */ InventoryFragment f$0;

    public /* synthetic */ $$Lambda$InventoryFragment$rVng5uWEvbEI9a7L4N5mNZBg3Lc(InventoryFragment inventoryFragment) {
        this.f$0 = inventoryFragment;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$onCreate$0$InventoryFragment((Item) obj);
    }
}

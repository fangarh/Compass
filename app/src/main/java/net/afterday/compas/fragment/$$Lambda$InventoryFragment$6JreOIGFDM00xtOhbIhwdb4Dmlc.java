package net.afterday.compas.fragment;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.inventory.Inventory;

/* JADX INFO: renamed from: net.afterday.compas.fragment.-$$Lambda$InventoryFragment$6JreOIGFDM00xtOhbIhwdb4Dmlc, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$InventoryFragment$6JreOIGFDM00xtOhbIhwdb4Dmlc implements Consumer {
    private final /* synthetic */ InventoryFragment f$0;

    public /* synthetic */ $$Lambda$InventoryFragment$6JreOIGFDM00xtOhbIhwdb4Dmlc(InventoryFragment inventoryFragment) {
        this.f$0 = inventoryFragment;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$loadInventory$3$InventoryFragment((Inventory) obj);
    }
}

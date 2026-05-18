package net.afterday.compas.fragment;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.player.Player;

/* JADX INFO: renamed from: net.afterday.compas.fragment.-$$Lambda$InventoryFragment$JOkctnXL9-jyG3eS23ByAuKzB0Y, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$InventoryFragment$JOkctnXL9jyG3eS23ByAuKzB0Y implements Consumer {
    private final /* synthetic */ InventoryFragment f$0;

    public /* synthetic */ $$Lambda$InventoryFragment$JOkctnXL9jyG3eS23ByAuKzB0Y(InventoryFragment inventoryFragment) {
        this.f$0 = inventoryFragment;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$onCreate$1$InventoryFragment((Player.STATE) obj);
    }
}

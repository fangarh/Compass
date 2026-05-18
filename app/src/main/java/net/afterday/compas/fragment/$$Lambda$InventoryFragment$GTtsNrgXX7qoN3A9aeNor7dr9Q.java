package net.afterday.compas.fragment;

import android.view.View;
import android.widget.AdapterView;

/* JADX INFO: renamed from: net.afterday.compas.fragment.-$$Lambda$InventoryFragment$GTtsNrgXX7qoN3A9aeNor-7dr9Q, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$InventoryFragment$GTtsNrgXX7qoN3A9aeNor7dr9Q implements AdapterView.OnItemClickListener {
    private final /* synthetic */ InventoryFragment f$0;

    public /* synthetic */ $$Lambda$InventoryFragment$GTtsNrgXX7qoN3A9aeNor7dr9Q(InventoryFragment inventoryFragment) {
        this.f$0 = inventoryFragment;
    }

    @Override // android.widget.AdapterView.OnItemClickListener
    public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
        this.f$0.lambda$showCategories$4$InventoryFragment(adapterView, view, i, j);
    }
}

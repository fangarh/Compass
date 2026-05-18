package net.afterday.compas.fragment;

import android.view.View;
import android.widget.AdapterView;

/* JADX INFO: renamed from: net.afterday.compas.fragment.-$$Lambda$InventoryFragment$_fNe5jLbo7gA40PTXGjHtoWDmIA, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$InventoryFragment$_fNe5jLbo7gA40PTXGjHtoWDmIA implements AdapterView.OnItemClickListener {
    private final /* synthetic */ InventoryFragment f$0;

    public /* synthetic */ $$Lambda$InventoryFragment$_fNe5jLbo7gA40PTXGjHtoWDmIA(InventoryFragment inventoryFragment) {
        this.f$0 = inventoryFragment;
    }

    @Override // android.widget.AdapterView.OnItemClickListener
    public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
        this.f$0.lambda$openCategoryItems$5$InventoryFragment(adapterView, view, i, j);
    }
}

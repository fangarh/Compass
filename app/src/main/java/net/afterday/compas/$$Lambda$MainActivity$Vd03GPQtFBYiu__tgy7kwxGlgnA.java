package net.afterday.compas;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.inventory.Inventory;

/* JADX INFO: renamed from: net.afterday.compas.-$$Lambda$MainActivity$Vd03GPQtFBYiu__tgy7kwxGlgnA, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$MainActivity$Vd03GPQtFBYiu__tgy7kwxGlgnA implements Consumer {
    private final /* synthetic */ MainActivity f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ $$Lambda$MainActivity$Vd03GPQtFBYiu__tgy7kwxGlgnA(MainActivity mainActivity, int i) {
        this.f$0 = mainActivity;
        this.f$1 = i;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$openInventory$8$MainActivity(this.f$1, (Inventory) obj);
    }
}

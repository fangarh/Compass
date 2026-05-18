package net.afterday.compas.fragment;

import android.widget.Button;
import io.reactivex.functions.Consumer;

/* JADX INFO: renamed from: net.afterday.compas.fragment.-$$Lambda$ItemInfoFragment$2gYzakf95LWlnmLEe0gch-57j6I, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$ItemInfoFragment$2gYzakf95LWlnmLEe0gch57j6I implements Consumer {
    private final /* synthetic */ Button f$0;

    public /* synthetic */ $$Lambda$ItemInfoFragment$2gYzakf95LWlnmLEe0gch57j6I(Button button) {
        this.f$0 = button;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        ItemInfoFragment.lambda$setupItem$1(this.f$0, (Boolean) obj);
    }
}

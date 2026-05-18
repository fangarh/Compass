package net.afterday.compas.view;

import io.reactivex.functions.Consumer;

/* JADX INFO: renamed from: net.afterday.compas.view.-$$Lambda$Battery$e2kEKbjHOoL0R90y65AyXpLVR3s, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Battery$e2kEKbjHOoL0R90y65AyXpLVR3s implements Consumer {
    private final /* synthetic */ Battery f$0;

    public /* synthetic */ $$Lambda$Battery$e2kEKbjHOoL0R90y65AyXpLVR3s(Battery battery) {
        this.f$0 = battery;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$setStatus$0$Battery((Long) obj);
    }
}

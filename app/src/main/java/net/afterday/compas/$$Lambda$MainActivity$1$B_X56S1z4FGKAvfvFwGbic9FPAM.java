package net.afterday.compas;

import io.reactivex.functions.Consumer;
import net.afterday.compas.MainActivity;
import net.afterday.compas.core.gameState.Frame;

/* JADX INFO: renamed from: net.afterday.compas.-$$Lambda$MainActivity$1$B_X56S1z4FGKAvfvFwGbic9FPAM, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$MainActivity$1$B_X56S1z4FGKAvfvFwGbic9FPAM implements Consumer {
    private final /* synthetic */ MainActivity.AnonymousClass1 f$0;

    public /* synthetic */ $$Lambda$MainActivity$1$B_X56S1z4FGKAvfvFwGbic9FPAM(MainActivity.AnonymousClass1 anonymousClass1) {
        this.f$0 = anonymousClass1;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$onServiceConnected$3$MainActivity$1((Frame) obj);
    }
}

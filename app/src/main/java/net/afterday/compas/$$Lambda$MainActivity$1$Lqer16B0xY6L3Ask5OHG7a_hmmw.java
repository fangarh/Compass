package net.afterday.compas;

import io.reactivex.functions.Consumer;
import net.afterday.compas.MainActivity;

/* JADX INFO: renamed from: net.afterday.compas.-$$Lambda$MainActivity$1$Lqer16B0xY6L3Ask5OHG7a_hmmw, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$MainActivity$1$Lqer16B0xY6L3Ask5OHG7a_hmmw implements Consumer {
    private final /* synthetic */ MainActivity.AnonymousClass1 f$0;

    public /* synthetic */ $$Lambda$MainActivity$1$Lqer16B0xY6L3Ask5OHG7a_hmmw(MainActivity.AnonymousClass1 anonymousClass1) {
        this.f$0 = anonymousClass1;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$onServiceConnected$2$MainActivity$1((Long) obj);
    }
}

package net.afterday.compas.engine.influences.WifiInfluences;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.influences.InfluencesPack;

/* JADX INFO: renamed from: net.afterday.compas.engine.influences.WifiInfluences.-$$Lambda$WifiInfluenceProviderImpl$fLlMdD61IeONjv_-xmqFkxJz3xU, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$WifiInfluenceProviderImpl$fLlMdD61IeONjv_xmqFkxJz3xU implements Consumer {
    private final /* synthetic */ WifiInfluenceProviderImpl f$0;

    public /* synthetic */ $$Lambda$WifiInfluenceProviderImpl$fLlMdD61IeONjv_xmqFkxJz3xU(WifiInfluenceProviderImpl wifiInfluenceProviderImpl) {
        this.f$0 = wifiInfluenceProviderImpl;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$new$1$WifiInfluenceProviderImpl((InfluencesPack) obj);
    }
}

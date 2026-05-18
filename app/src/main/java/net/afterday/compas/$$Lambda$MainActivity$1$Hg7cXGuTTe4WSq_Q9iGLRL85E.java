package net.afterday.compas;

import io.reactivex.functions.Consumer;
import net.afterday.compas.MainActivity;
import net.afterday.compas.core.inventory.items.Events.ItemAdded;

/* JADX INFO: renamed from: net.afterday.compas.-$$Lambda$MainActivity$1$Hg7cXGuT-Te4WSq_Q9iG-LRL85E, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$MainActivity$1$Hg7cXGuTTe4WSq_Q9iGLRL85E implements Consumer {
    private final /* synthetic */ MainActivity.AnonymousClass1 f$0;

    public /* synthetic */ $$Lambda$MainActivity$1$Hg7cXGuTTe4WSq_Q9iGLRL85E(MainActivity.AnonymousClass1 anonymousClass1) {
        this.f$0 = anonymousClass1;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$onServiceConnected$5$MainActivity$1((ItemAdded) obj);
    }
}

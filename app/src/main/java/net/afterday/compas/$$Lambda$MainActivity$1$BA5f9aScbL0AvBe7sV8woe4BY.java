package net.afterday.compas;

import io.reactivex.functions.Consumer;
import net.afterday.compas.MainActivity;

/* JADX INFO: renamed from: net.afterday.compas.-$$Lambda$MainActivity$1$BA5f9aScbL0AvBe-7sV8woe4-BY, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$MainActivity$1$BA5f9aScbL0AvBe7sV8woe4BY implements Consumer {
    private final /* synthetic */ MainActivity.AnonymousClass1 f$0;

    public /* synthetic */ $$Lambda$MainActivity$1$BA5f9aScbL0AvBe7sV8woe4BY(MainActivity.AnonymousClass1 anonymousClass1) {
        this.f$0 = anonymousClass1;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$onServiceConnected$0$MainActivity$1((Integer) obj);
    }
}

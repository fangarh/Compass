package net.afterday.compas.view;

import io.reactivex.functions.Consumer;

/* JADX INFO: renamed from: net.afterday.compas.view.-$$Lambda$Clock$2X3wBUVjZw0igvJS_dWUuH18rLc, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Clock$2X3wBUVjZw0igvJS_dWUuH18rLc implements Consumer {
    private final /* synthetic */ Clock f$0;

    public /* synthetic */ $$Lambda$Clock$2X3wBUVjZw0igvJS_dWUuH18rLc(Clock clock) {
        this.f$0 = clock;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$onAttachedToWindow$0$Clock((Long) obj);
    }
}

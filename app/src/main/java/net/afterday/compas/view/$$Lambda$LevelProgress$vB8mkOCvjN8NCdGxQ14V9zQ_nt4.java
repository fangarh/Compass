package net.afterday.compas.view;

import io.reactivex.functions.Consumer;

/* JADX INFO: renamed from: net.afterday.compas.view.-$$Lambda$LevelProgress$vB8mkOCvjN8NCdGxQ14V9zQ_nt4, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$LevelProgress$vB8mkOCvjN8NCdGxQ14V9zQ_nt4 implements Consumer {
    private final /* synthetic */ LevelProgress f$0;

    public /* synthetic */ $$Lambda$LevelProgress$vB8mkOCvjN8NCdGxQ14V9zQ_nt4(LevelProgress levelProgress) {
        this.f$0 = levelProgress;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$setProgress$3$LevelProgress((Long) obj);
    }
}

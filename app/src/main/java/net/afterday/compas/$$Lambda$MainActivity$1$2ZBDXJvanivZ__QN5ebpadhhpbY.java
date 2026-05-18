package net.afterday.compas;

import net.afterday.compas.MainActivity;
import net.afterday.compas.view.LevelProgress;

/* JADX INFO: renamed from: net.afterday.compas.-$$Lambda$MainActivity$1$2ZBDXJvanivZ__QN5ebpadhhpbY, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$MainActivity$1$2ZBDXJvanivZ__QN5ebpadhhpbY implements LevelProgress.OnLevelChangedListener {
    private final /* synthetic */ MainActivity.AnonymousClass1 f$0;

    public /* synthetic */ $$Lambda$MainActivity$1$2ZBDXJvanivZ__QN5ebpadhhpbY(MainActivity.AnonymousClass1 anonymousClass1) {
        this.f$0 = anonymousClass1;
    }

    @Override // net.afterday.compas.view.LevelProgress.OnLevelChangedListener
    public final void levelChanged(int i) {
        this.f$0.lambda$onServiceConnected$4$MainActivity$1(i);
    }
}

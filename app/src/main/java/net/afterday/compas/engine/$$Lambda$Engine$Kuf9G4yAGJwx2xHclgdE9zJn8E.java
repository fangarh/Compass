package net.afterday.compas.engine;

import io.reactivex.functions.Consumer;
import net.afterday.compas.util.Triple;

/* JADX INFO: renamed from: net.afterday.compas.engine.-$$Lambda$Engine$Kuf9G4y-AGJwx2xHclgdE9zJn8E, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Engine$Kuf9G4yAGJwx2xHclgdE9zJn8E implements Consumer {
    private final /* synthetic */ Engine f$0;

    public /* synthetic */ $$Lambda$Engine$Kuf9G4yAGJwx2xHclgdE9zJn8E(Engine engine) {
        this.f$0 = engine;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$startGame$13$Engine((Triple) obj);
    }
}

package net.afterday.compas.engine;

import io.reactivex.functions.Function;
import net.afterday.compas.core.gameState.State;

/* JADX INFO: renamed from: net.afterday.compas.engine.-$$Lambda$Engine$w6Q3aItYwTLz2ptXxVqV-zA433s, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Engine$w6Q3aItYwTLz2ptXxVqVzA433s implements Function {
    private final /* synthetic */ Engine f$0;

    public /* synthetic */ $$Lambda$Engine$w6Q3aItYwTLz2ptXxVqVzA433s(Engine engine) {
        this.f$0 = engine;
    }

    @Override // io.reactivex.functions.Function
    public final Object apply(Object obj) {
        return this.f$0.lambda$new$1$Engine((State) obj);
    }
}

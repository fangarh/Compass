package net.afterday.compas.engine;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.Game;

/* JADX INFO: renamed from: net.afterday.compas.engine.-$$Lambda$Engine$ggXWF3zuMiXIL2rX4BFfSm6DGgc, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Engine$ggXWF3zuMiXIL2rX4BFfSm6DGgc implements Consumer {
    private final /* synthetic */ Engine f$0;

    public /* synthetic */ $$Lambda$Engine$ggXWF3zuMiXIL2rX4BFfSm6DGgc(Engine engine) {
        this.f$0 = engine;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        Engine.lambda$ggXWF3zuMiXIL2rX4BFfSm6DGgc(this.f$0, (Game) obj);
    }
}

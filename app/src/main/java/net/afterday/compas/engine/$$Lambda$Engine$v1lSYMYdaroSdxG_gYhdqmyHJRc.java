package net.afterday.compas.engine;

import io.reactivex.functions.Function;
import net.afterday.compas.core.player.Player;

/* JADX INFO: renamed from: net.afterday.compas.engine.-$$Lambda$Engine$v1lSYMYdaroSdxG_gYhdqmyHJRc, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Engine$v1lSYMYdaroSdxG_gYhdqmyHJRc implements Function {
    private final /* synthetic */ Engine f$0;
    private final /* synthetic */ Player.STATE f$1;

    public /* synthetic */ $$Lambda$Engine$v1lSYMYdaroSdxG_gYhdqmyHJRc(Engine engine, Player.STATE state) {
        this.f$0 = engine;
        this.f$1 = state;
    }

    @Override // io.reactivex.functions.Function
    public final Object apply(Object obj) {
        return this.f$0.lambda$makeCountDownFor$27$Engine(this.f$1, (Player.STATE) obj);
    }
}

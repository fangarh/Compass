package net.afterday.compas.engine;

import io.reactivex.functions.BiFunction;
import net.afterday.compas.core.player.Player;

/* JADX INFO: renamed from: net.afterday.compas.engine.-$$Lambda$Engine$XWfoFtD1YI17ezb8z4wyazgLSYQ, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Engine$XWfoFtD1YI17ezb8z4wyazgLSYQ implements BiFunction {
    public static final /* synthetic */ $$Lambda$Engine$XWfoFtD1YI17ezb8z4wyazgLSYQ INSTANCE = new $$Lambda$Engine$XWfoFtD1YI17ezb8z4wyazgLSYQ();

    private /* synthetic */ $$Lambda$Engine$XWfoFtD1YI17ezb8z4wyazgLSYQ() {
    }

    @Override // io.reactivex.functions.BiFunction
    public final Object apply(Object obj, Object obj2) {
        return Engine.lambda$setupSuicides$21((String) obj, (Player.STATE) obj2);
    }
}

package net.afterday.compas.engine;

import io.reactivex.functions.Function3;
import net.afterday.compas.core.player.Player;

/* JADX INFO: renamed from: net.afterday.compas.engine.-$$Lambda$Engine$3PNNt2ygH2_ALEbWIcFyvQy8IK8, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Engine$3PNNt2ygH2_ALEbWIcFyvQy8IK8 implements Function3 {
    public static final /* synthetic */ $$Lambda$Engine$3PNNt2ygH2_ALEbWIcFyvQy8IK8 INSTANCE = new $$Lambda$Engine$3PNNt2ygH2_ALEbWIcFyvQy8IK8();

    private /* synthetic */ $$Lambda$Engine$3PNNt2ygH2_ALEbWIcFyvQy8IK8() {
    }

    @Override // io.reactivex.functions.Function3
    public final Object apply(Object obj, Object obj2, Object obj3) {
        return Engine.lambda$startGame$12((Boolean) obj, (Player.STATE) obj2, (Player.FRACTION) obj3);
    }
}

package net.afterday.compas;

import io.reactivex.functions.BiFunction;
import net.afterday.compas.MainActivity;
import net.afterday.compas.core.player.Player;

/* JADX INFO: renamed from: net.afterday.compas.-$$Lambda$MainActivity$1$cPNHwParvMjeuXNyCMYDO8mEoSA, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$MainActivity$1$cPNHwParvMjeuXNyCMYDO8mEoSA implements BiFunction {
    public static final /* synthetic */ $$Lambda$MainActivity$1$cPNHwParvMjeuXNyCMYDO8mEoSA INSTANCE = new $$Lambda$MainActivity$1$cPNHwParvMjeuXNyCMYDO8mEoSA();

    private /* synthetic */ $$Lambda$MainActivity$1$cPNHwParvMjeuXNyCMYDO8mEoSA() {
    }

    @Override // io.reactivex.functions.BiFunction
    public final Object apply(Object obj, Object obj2) {
        return MainActivity.lambda$onServiceConnected$8((Boolean) obj, (Player.FRACTION) obj2);
    }
}

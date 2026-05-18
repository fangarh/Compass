package net.afterday.compas.persistency;

import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

/* JADX INFO: renamed from: net.afterday.compas.persistency.-$$Lambda$HardcodedPersistency$MtIX_ZX-8TyONKmFk3HsUCZkvnI, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$HardcodedPersistency$MtIX_ZX8TyONKmFk3HsUCZkvnI implements SingleOnSubscribe {
    public static final /* synthetic */ $$Lambda$HardcodedPersistency$MtIX_ZX8TyONKmFk3HsUCZkvnI INSTANCE = new $$Lambda$HardcodedPersistency$MtIX_ZX8TyONKmFk3HsUCZkvnI();

    private /* synthetic */ $$Lambda$HardcodedPersistency$MtIX_ZX8TyONKmFk3HsUCZkvnI() {
    }

    @Override // io.reactivex.SingleOnSubscribe
    public final void subscribe(SingleEmitter singleEmitter) {
        HardcodedPersistency.lambda$getStoredPlayer$0(singleEmitter);
    }
}

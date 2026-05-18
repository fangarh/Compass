package net.afterday.compas.persistency;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import java.util.List;
import java8.util.Optional;
import net.afterday.compas.core.fraction.Fraction;
import net.afterday.compas.core.influences.Influence;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.core.player.Player;

/* JADX INFO: loaded from: classes.dex */
public class HardcodedPersistency implements Persistency {
    HardcodedPersistency() {
    }

    @Override // net.afterday.compas.persistency.Persistency
    public Single<List<Item>> getPossibleItems() {
        return null;
    }

    @Override // net.afterday.compas.persistency.Persistency
    public Single<Optional<Player>> getStoredPlayer() {
        return Single.create($$Lambda$HardcodedPersistency$MtIX_ZX8TyONKmFk3HsUCZkvnI.INSTANCE);
    }

    static /* synthetic */ void lambda$getStoredPlayer$0(SingleEmitter emitter) {
        if (!emitter.isDisposed()) {
            emitter.onSuccess(Optional.empty());
        }
    }

    public Single<Fraction> getPossibleFractions() {
        return null;
    }

    @Override // net.afterday.compas.persistency.Persistency
    public Single<List<Influence>> getPossibleInfluences() {
        return null;
    }
}

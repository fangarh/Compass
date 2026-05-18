package net.afterday.compas.persistency;

import io.reactivex.Single;
import java.util.List;
import java8.util.Optional;
import net.afterday.compas.core.influences.Influence;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.core.player.Player;

/* JADX INFO: loaded from: classes.dex */
public interface Persistency {
    Single<List<Influence>> getPossibleInfluences();

    Single<List<Item>> getPossibleItems();

    Single<Optional<Player>> getStoredPlayer();
}

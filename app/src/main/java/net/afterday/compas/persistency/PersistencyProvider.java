package net.afterday.compas.persistency;

import net.afterday.compas.persistency.influences.InfluencesPersistency;
import net.afterday.compas.persistency.initialState.GameStatePersistency;
import net.afterday.compas.persistency.items.ItemsPersistency;
import net.afterday.compas.persistency.player.PlayerPersistency;

/* JADX INFO: loaded from: classes.dex */
public interface PersistencyProvider {
    InfluencesPersistency getInfluencesPersistency();

    GameStatePersistency getInitialStatePersistency();

    ItemsPersistency getItemsPersistency();

    PlayerPersistency getPlayerPersistency();
}

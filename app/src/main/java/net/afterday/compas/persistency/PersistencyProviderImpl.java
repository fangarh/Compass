package net.afterday.compas.persistency;

import net.afterday.compas.persistency.hardcoded.HGameStatePersistency;
import net.afterday.compas.persistency.hardcoded.HInfluencesPersistency;
import net.afterday.compas.persistency.hardcoded.HItemsPersistency;
import net.afterday.compas.persistency.hardcoded.HPlayerPersistency;
import net.afterday.compas.persistency.influences.InfluencesPersistency;
import net.afterday.compas.persistency.initialState.GameStatePersistency;
import net.afterday.compas.persistency.items.ItemsPersistency;
import net.afterday.compas.persistency.player.PlayerPersistency;

/* JADX INFO: loaded from: classes.dex */
public class PersistencyProviderImpl implements PersistencyProvider {
    private final ItemsPersistency itemsPersistency = new HItemsPersistency();
    private final GameStatePersistency gameStatePersistency = new HGameStatePersistency();
    private final InfluencesPersistency influencesPersistency = new HInfluencesPersistency();
    private final PlayerPersistency playerPersistency = new HPlayerPersistency();

    @Override // net.afterday.compas.persistency.PersistencyProvider
    public InfluencesPersistency getInfluencesPersistency() {
        return this.influencesPersistency;
    }

    @Override // net.afterday.compas.persistency.PersistencyProvider
    public ItemsPersistency getItemsPersistency() {
        return this.itemsPersistency;
    }

    @Override // net.afterday.compas.persistency.PersistencyProvider
    public GameStatePersistency getInitialStatePersistency() {
        return this.gameStatePersistency;
    }

    @Override // net.afterday.compas.persistency.PersistencyProvider
    public PlayerPersistency getPlayerPersistency() {
        return this.playerPersistency;
    }
}

package net.afterday.compas.persistency.initialState;

import net.afterday.compas.core.gameState.GameState;

/* JADX INFO: loaded from: classes.dex */
public interface GameStatePersistency {
    GameState loadState();

    void storeState(GameState gameState);
}

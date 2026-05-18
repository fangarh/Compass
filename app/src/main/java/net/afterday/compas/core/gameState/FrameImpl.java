package net.afterday.compas.core.gameState;

import net.afterday.compas.core.player.PlayerProps;

/* JADX INFO: loaded from: classes.dex */
public class FrameImpl implements Frame {
    private PlayerProps mPlayerProps;

    public FrameImpl(PlayerProps playerProps) {
        this.mPlayerProps = playerProps;
    }

    @Override // net.afterday.compas.core.gameState.Frame
    public PlayerProps getPlayerProps() {
        return this.mPlayerProps;
    }
}

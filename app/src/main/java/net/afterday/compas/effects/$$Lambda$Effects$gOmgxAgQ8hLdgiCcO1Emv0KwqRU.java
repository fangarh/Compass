package net.afterday.compas.effects;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.player.Player;

/* JADX INFO: renamed from: net.afterday.compas.effects.-$$Lambda$Effects$gOmgxAgQ8hLdgiCcO1Emv0KwqRU, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Effects$gOmgxAgQ8hLdgiCcO1Emv0KwqRU implements Consumer {
    private final /* synthetic */ Effects f$0;

    public /* synthetic */ $$Lambda$Effects$gOmgxAgQ8hLdgiCcO1Emv0KwqRU(Effects effects) {
        this.f$0 = effects;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$setPlayerStatesStream$10$Effects((Player.STATE) obj);
    }
}

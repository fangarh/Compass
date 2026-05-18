package net.afterday.compas.fragment;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.player.Player;

/* JADX INFO: renamed from: net.afterday.compas.fragment.-$$Lambda$ItemInfoFragment$BM3qN68xYD3nVUVyb-Sh9klJmGs, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$ItemInfoFragment$BM3qN68xYD3nVUVybSh9klJmGs implements Consumer {
    private final /* synthetic */ ItemInfoFragment f$0;

    public /* synthetic */ $$Lambda$ItemInfoFragment$BM3qN68xYD3nVUVybSh9klJmGs(ItemInfoFragment itemInfoFragment) {
        this.f$0 = itemInfoFragment;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$onCreate$0$ItemInfoFragment((Player.STATE) obj);
    }
}

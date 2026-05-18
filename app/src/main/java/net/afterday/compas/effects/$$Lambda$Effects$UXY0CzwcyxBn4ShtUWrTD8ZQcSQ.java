package net.afterday.compas.effects;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.gameState.Frame;

/* JADX INFO: renamed from: net.afterday.compas.effects.-$$Lambda$Effects$UXY0CzwcyxBn4ShtUWrTD8ZQcSQ, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Effects$UXY0CzwcyxBn4ShtUWrTD8ZQcSQ implements Consumer {
    private final /* synthetic */ Effects f$0;

    public /* synthetic */ $$Lambda$Effects$UXY0CzwcyxBn4ShtUWrTD8ZQcSQ(Effects effects) {
        this.f$0 = effects;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$new$0$Effects((Frame) obj);
    }
}

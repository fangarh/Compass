package net.afterday.compas.view;

import io.reactivex.functions.Consumer;
import net.afterday.compas.core.inventory.items.Events.ItemAdded;

/* JADX INFO: renamed from: net.afterday.compas.view.-$$Lambda$LevelProgress$ifDHHP7zs9dR7O72KWe8l84Aa68, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$LevelProgress$ifDHHP7zs9dR7O72KWe8l84Aa68 implements Consumer {
    private final /* synthetic */ LevelProgress f$0;
    private final /* synthetic */ ItemAdded f$1;

    public /* synthetic */ $$Lambda$LevelProgress$ifDHHP7zs9dR7O72KWe8l84Aa68(LevelProgress levelProgress, ItemAdded itemAdded) {
        this.f$0 = levelProgress;
        this.f$1 = itemAdded;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$setProgress$2$LevelProgress(this.f$1, (Long) obj);
    }
}

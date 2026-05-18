package net.afterday.compas.engine;

import io.reactivex.functions.Predicate;
import net.afterday.compas.core.inventory.items.Item;

/* JADX INFO: renamed from: net.afterday.compas.engine.-$$Lambda$Engine$8knwBC9ZN8xFluejut32DK7PWO8, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Engine$8knwBC9ZN8xFluejut32DK7PWO8 implements Predicate {
    public static final /* synthetic */ $$Lambda$Engine$8knwBC9ZN8xFluejut32DK7PWO8 INSTANCE = new $$Lambda$Engine$8knwBC9ZN8xFluejut32DK7PWO8();

    private /* synthetic */ $$Lambda$Engine$8knwBC9ZN8xFluejut32DK7PWO8() {
    }

    @Override // io.reactivex.functions.Predicate
    public final boolean test(Object obj) {
        return Engine.lambda$startGame$14((Item) obj);
    }
}

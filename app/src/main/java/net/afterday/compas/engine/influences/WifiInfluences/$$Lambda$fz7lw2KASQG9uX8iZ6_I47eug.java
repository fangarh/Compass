package net.afterday.compas.engine.influences.WifiInfluences;

import io.reactivex.functions.Function;
import java.util.List;
import net.afterday.compas.core.influences.InfluencesPack;
import net.afterday.compas.engine.influences.InfluenceExtractionStrategy;

/* JADX INFO: renamed from: net.afterday.compas.engine.influences.WifiInfluences.-$$Lambda$fz7l-w2KA-SQG9uX8iZ6_I47eug, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$fz7lw2KASQG9uX8iZ6_I47eug implements Function {
    private final /* synthetic */ InfluenceExtractionStrategy f$0;

    public /* synthetic */ $$Lambda$fz7lw2KASQG9uX8iZ6_I47eug(InfluenceExtractionStrategy influenceExtractionStrategy) {
        this.f$0 = influenceExtractionStrategy;
    }

    @Override // io.reactivex.functions.Function
    public final Object apply(Object obj) {
        return (InfluencesPack) this.f$0.makeInfluences((List) obj);
    }
}

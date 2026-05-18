package net.afterday.compas.logging;

import io.reactivex.functions.BiFunction;
import java.util.List;

/* JADX INFO: renamed from: net.afterday.compas.logging.-$$Lambda$Logger$GmZROCTPpbm7kMwYzRoDXQPmbLk, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Logger$GmZROCTPpbm7kMwYzRoDXQPmbLk implements BiFunction {
    public static final /* synthetic */ $$Lambda$Logger$GmZROCTPpbm7kMwYzRoDXQPmbLk INSTANCE = new $$Lambda$Logger$GmZROCTPpbm7kMwYzRoDXQPmbLk();

    private /* synthetic */ $$Lambda$Logger$GmZROCTPpbm7kMwYzRoDXQPmbLk() {
    }

    @Override // io.reactivex.functions.BiFunction
    public final Object apply(Object obj, Object obj2) {
        return Logger.lambda$new$0((List) obj, (LogLine) obj2);
    }
}

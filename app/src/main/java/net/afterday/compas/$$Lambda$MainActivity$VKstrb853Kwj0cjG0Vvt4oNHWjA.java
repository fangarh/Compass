package net.afterday.compas;

import android.support.v7.widget.RecyclerView;
import io.reactivex.functions.Consumer;
import java.util.List;

/* JADX INFO: renamed from: net.afterday.compas.-$$Lambda$MainActivity$VKstrb853Kwj0cjG0Vvt4oNHWjA, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$MainActivity$VKstrb853Kwj0cjG0Vvt4oNHWjA implements Consumer {
    private final /* synthetic */ MainActivity f$0;
    private final /* synthetic */ RecyclerView.LayoutManager f$1;

    public /* synthetic */ $$Lambda$MainActivity$VKstrb853Kwj0cjG0Vvt4oNHWjA(MainActivity mainActivity, RecyclerView.LayoutManager layoutManager) {
        this.f$0 = mainActivity;
        this.f$1 = layoutManager;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$setupLog$3$MainActivity(this.f$1, (List) obj);
    }
}

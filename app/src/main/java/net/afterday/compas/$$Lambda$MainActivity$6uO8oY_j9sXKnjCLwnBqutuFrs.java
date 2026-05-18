package net.afterday.compas;

import android.view.MotionEvent;
import android.view.View;

/* JADX INFO: renamed from: net.afterday.compas.-$$Lambda$MainActivity$6uO8oY_j9sXKnjCL-wnBqutuFrs, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$MainActivity$6uO8oY_j9sXKnjCLwnBqutuFrs implements View.OnTouchListener {
    private final /* synthetic */ MainActivity f$0;

    public /* synthetic */ $$Lambda$MainActivity$6uO8oY_j9sXKnjCLwnBqutuFrs(MainActivity mainActivity) {
        this.f$0 = mainActivity;
    }

    @Override // android.view.View.OnTouchListener
    public final boolean onTouch(View view, MotionEvent motionEvent) {
        return this.f$0.lambda$setViewListeners$7$MainActivity(view, motionEvent);
    }
}

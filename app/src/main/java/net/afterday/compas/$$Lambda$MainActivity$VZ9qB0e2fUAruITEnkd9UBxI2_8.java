package net.afterday.compas;

import android.view.MotionEvent;
import android.view.View;

/* JADX INFO: renamed from: net.afterday.compas.-$$Lambda$MainActivity$VZ9qB0e2fUAruITEnkd9UBxI2_8, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$MainActivity$VZ9qB0e2fUAruITEnkd9UBxI2_8 implements View.OnTouchListener {
    private final /* synthetic */ MainActivity f$0;

    public /* synthetic */ $$Lambda$MainActivity$VZ9qB0e2fUAruITEnkd9UBxI2_8(MainActivity mainActivity) {
        this.f$0 = mainActivity;
    }

    @Override // android.view.View.OnTouchListener
    public final boolean onTouch(View view, MotionEvent motionEvent) {
        return this.f$0.lambda$setViewListeners$4$MainActivity(view, motionEvent);
    }
}

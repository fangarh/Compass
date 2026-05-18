package net.afterday.compas.fragment;

import android.animation.ValueAnimator;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes.dex */
public class BloodFragment extends DialogFragment {
    private CompositeDisposable subscriptions = new CompositeDisposable();

    @Override // android.app.DialogFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getArguments();
        setStyle(2, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
    }

    @Override // android.app.Fragment
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(net.afterday.compas.R.layout.blood_fragment, container, false);
        ValueAnimator vAnim = ValueAnimator.ofFloat(1.0f, 0.0f);
        vAnim.setDuration(3000L);
        vAnim.addUpdateListener(new $$Lambda$BloodFragment$10vGyI8Fl01ekKIK4EBJ6Kb2mrE(v));
        vAnim.start();
        this.subscriptions.add(Observable.timer(3000L, TimeUnit.MILLISECONDS).subscribe(new $$Lambda$BloodFragment$UuFzAYCW34sCBy6zMPUjzrjBRHM(this)));
        return v;
    }

    static /* synthetic */ void lambda$onCreateView$0(View v, ValueAnimator a) {
        v.setAlpha(((Float) a.getAnimatedValue()).floatValue());
    }

    public /* synthetic */ void lambda$onCreateView$1$BloodFragment(Long t) {
        close();
    }

    public void close() {
        dismiss();
    }

    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        CompositeDisposable compositeDisposable = this.subscriptions;
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            this.subscriptions.dispose();
            this.subscriptions = null;
            try {
                close();
            } catch (Exception e) {
            }
        }
    }
}

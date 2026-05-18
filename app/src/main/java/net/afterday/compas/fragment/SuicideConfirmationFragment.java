package net.afterday.compas.fragment;

import android.app.DialogFragment;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import io.reactivex.Observable;
import java.util.concurrent.TimeUnit;
import net.afterday.compas.R;
import net.afterday.compas.engine.events.PlayerEventBus;
import net.afterday.compas.util.Fonts;

/* JADX INFO: loaded from: classes.dex */
public class SuicideConfirmationFragment extends DialogFragment {
    private Button cancelBtn;
    private Button confirmBtn;
    private View v;

    static /* synthetic */ View access$000(SuicideConfirmationFragment x0) {
        return x0.v;
    }

    @Override // android.app.DialogFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(2, R.style.DialogStyle);
    }

    public /* synthetic */ void lambda$onCreateView$0$SuicideConfirmationFragment(Long t) {
        closePopup(this.v);
    }

    @Override // android.app.Fragment
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Observable.timer(15L, TimeUnit.SECONDS).take(1L).subscribe(new $$Lambda$SuicideConfirmationFragment$J3U5OwuvKNBNQpLHqpeDqYK9YwY(this));
        this.v = inflater.inflate(R.layout.suicide_confirmation, container, false);
        Typeface defaultTf = Fonts.instance().getDefaultTypeFace();
        TextView title = (TextView) this.v.findViewById(R.id.name);
        title.setTypeface(defaultTf);
        title.setTextSize(35.0f);
        Button confirmBtn = (Button) this.v.findViewById(R.id.confirm_suicide);
        Button cancelBtn = (Button) this.v.findViewById(R.id.cancel_suicide);
        confirmBtn.setTypeface(defaultTf);
        confirmBtn.setTextSize(25.0f);
        cancelBtn.setTypeface(defaultTf);
        cancelBtn.setTextSize(25.0f);
        this.v.findViewById(R.id.close).setOnClickListener(new AnonymousClass1());
        cancelBtn.setOnClickListener(new AnonymousClass2());
        confirmBtn.findViewById(R.id.confirm_suicide).setOnClickListener(new AnonymousClass3());
        return this.v;
    }

    /* JADX INFO: renamed from: net.afterday.compas.fragment.SuicideConfirmationFragment$1, reason: invalid class name */
    class AnonymousClass1 implements View.OnClickListener {
        AnonymousClass1() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View v) {
            SuicideConfirmationFragment.this.closePopup(v);
        }
    }

    /* JADX INFO: renamed from: net.afterday.compas.fragment.SuicideConfirmationFragment$2, reason: invalid class name */
    class AnonymousClass2 implements View.OnClickListener {
        AnonymousClass2() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            SuicideConfirmationFragment suicideConfirmationFragment = SuicideConfirmationFragment.this;
            suicideConfirmationFragment.closePopup(SuicideConfirmationFragment.access$000(suicideConfirmationFragment));
        }
    }

    /* JADX INFO: renamed from: net.afterday.compas.fragment.SuicideConfirmationFragment$3, reason: invalid class name */
    class AnonymousClass3 implements View.OnClickListener {
        AnonymousClass3() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View view) {
            PlayerEventBus.instance().suicide();
            SuicideConfirmationFragment suicideConfirmationFragment = SuicideConfirmationFragment.this;
            suicideConfirmationFragment.closePopup(SuicideConfirmationFragment.access$000(suicideConfirmationFragment));
        }
    }

    public void closePopup(View view) {
        try {
            if (!isDetached()) {
                dismiss();
            }
        } catch (Exception e) {
        }
    }
}

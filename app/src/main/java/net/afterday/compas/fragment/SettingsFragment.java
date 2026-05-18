package net.afterday.compas.fragment;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
import net.afterday.compas.R;
import net.afterday.compas.settings.Constants;
import net.afterday.compas.settings.Settings;

/* JADX INFO: loaded from: classes.dex */
public class SettingsFragment extends DialogFragment {
    private Settings settings;

    @Override // android.app.DialogFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(2, R.style.DialogStyle);
        this.settings = Settings.instance(getActivity().getApplication());
    }

    @Override // android.app.Fragment
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.settings_fragment, container, false);
        ((Switch) v.findViewById(R.id.vibroSwitch)).setChecked(this.settings.getBoolSetting(Constants.VIBRATION));
        ((Switch) v.findViewById(R.id.compassSwitch)).setChecked(this.settings.getBoolSetting(Constants.COMPASS));
        ((Switch) v.findViewById(R.id.vibroSwitch)).setOnCheckedChangeListener(new $$Lambda$SettingsFragment$wXkNRi5MmlzGZXGgIzsqEeHPuH0(this));
        ((Switch) v.findViewById(R.id.compassSwitch)).setOnCheckedChangeListener(new $$Lambda$SettingsFragment$aKH7qqTPYK1J3iveQyUL1pOJgLo(this));
        v.findViewById(R.id.close).setOnClickListener(new AnonymousClass1());
        RadioButton p = (RadioButton) v.findViewById(R.id.orientationPort);
        RadioButton h = (RadioButton) v.findViewById(R.id.orientationLand);
        int o = this.settings.getIntSetting(Constants.ORIENTATION);
        if (o == 1) {
            p.setChecked(true);
        } else if (o == 0) {
            h.setChecked(true);
        }
        p.setOnClickListener(new $$Lambda$SettingsFragment$CuoMav_TMU0ubm1XJLh4O4HOk3A(this));
        h.setOnClickListener(new $$Lambda$SettingsFragment$NdpAm3Dz99jMAZns7PA5HuFcWKs(this));
        return v;
    }

    public /* synthetic */ void lambda$onCreateView$0$SettingsFragment(CompoundButton btn, boolean on) {
        this.settings.setBoolSetting(Constants.VIBRATION, on);
    }

    public /* synthetic */ void lambda$onCreateView$1$SettingsFragment(CompoundButton btn, boolean on) {
        this.settings.setBoolSetting(Constants.COMPASS, on);
    }

    /* JADX INFO: renamed from: net.afterday.compas.fragment.SettingsFragment$1, reason: invalid class name */
    class AnonymousClass1 implements View.OnClickListener {
        AnonymousClass1() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View v) {
            try {
                SettingsFragment.this.dismiss();
            } catch (Exception e) {
            }
        }
    }

    public /* synthetic */ void lambda$onCreateView$2$SettingsFragment(View e) {
        orientationPort();
    }

    public /* synthetic */ void lambda$onCreateView$3$SettingsFragment(View e) {
        orientationLand();
    }

    public void orientationPort() {
        this.settings.setIntSetting(Constants.ORIENTATION, 1);
    }

    public void orientationLand() {
        this.settings.setIntSetting(Constants.ORIENTATION, 0);
    }
}

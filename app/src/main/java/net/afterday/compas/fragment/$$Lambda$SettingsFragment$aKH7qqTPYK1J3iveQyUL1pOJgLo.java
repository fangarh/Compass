package net.afterday.compas.fragment;

import android.widget.CompoundButton;

/* JADX INFO: renamed from: net.afterday.compas.fragment.-$$Lambda$SettingsFragment$aKH7qqTPYK1J3iveQyUL1pOJgLo, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$SettingsFragment$aKH7qqTPYK1J3iveQyUL1pOJgLo implements CompoundButton.OnCheckedChangeListener {
    private final /* synthetic */ SettingsFragment f$0;

    public /* synthetic */ $$Lambda$SettingsFragment$aKH7qqTPYK1J3iveQyUL1pOJgLo(SettingsFragment settingsFragment) {
        this.f$0 = settingsFragment;
    }

    @Override // android.widget.CompoundButton.OnCheckedChangeListener
    public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        this.f$0.lambda$onCreateView$1$SettingsFragment(compoundButton, z);
    }
}

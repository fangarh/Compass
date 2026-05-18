package net.afterday.compas.fragment;

import android.widget.CompoundButton;

/* JADX INFO: renamed from: net.afterday.compas.fragment.-$$Lambda$SettingsFragment$wXkNRi5MmlzGZXGgIzsqEeHPuH0, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$SettingsFragment$wXkNRi5MmlzGZXGgIzsqEeHPuH0 implements CompoundButton.OnCheckedChangeListener {
    private final /* synthetic */ SettingsFragment f$0;

    public /* synthetic */ $$Lambda$SettingsFragment$wXkNRi5MmlzGZXGgIzsqEeHPuH0(SettingsFragment settingsFragment) {
        this.f$0 = settingsFragment;
    }

    @Override // android.widget.CompoundButton.OnCheckedChangeListener
    public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        this.f$0.lambda$onCreateView$0$SettingsFragment(compoundButton, z);
    }
}

package net.afterday.compas.devices.vibro;

import net.afterday.compas.settings.SettingsListener;

/* JADX INFO: renamed from: net.afterday.compas.devices.vibro.-$$Lambda$VibroImpl$z3vvsh6UTKeiXMgGf6-gtScAHxo, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$VibroImpl$z3vvsh6UTKeiXMgGf6gtScAHxo implements SettingsListener {
    private final /* synthetic */ VibroImpl f$0;

    public /* synthetic */ $$Lambda$VibroImpl$z3vvsh6UTKeiXMgGf6gtScAHxo(VibroImpl vibroImpl) {
        this.f$0 = vibroImpl;
    }

    @Override // net.afterday.compas.settings.SettingsListener
    public final void onSettingChanged(String str, String str2) {
        this.f$0.lambda$new$0$VibroImpl(str, str2);
    }
}

package net.afterday.compas;

import net.afterday.compas.settings.SettingsListener;

/* JADX INFO: renamed from: net.afterday.compas.-$$Lambda$MainActivity$ZnxmrvBnM8PbxeUw5PO1856bMoU, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$MainActivity$ZnxmrvBnM8PbxeUw5PO1856bMoU implements SettingsListener {
    private final /* synthetic */ MainActivity f$0;

    public /* synthetic */ $$Lambda$MainActivity$ZnxmrvBnM8PbxeUw5PO1856bMoU(MainActivity mainActivity) {
        this.f$0 = mainActivity;
    }

    @Override // net.afterday.compas.settings.SettingsListener
    public final void onSettingChanged(String str, String str2) {
        this.f$0.lambda$setupListeners$11$MainActivity(str, str2);
    }
}

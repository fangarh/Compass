package net.afterday.compas.devices.vibro;

import io.reactivex.functions.Consumer;
import net.afterday.compas.devices.vibro.VibroImpl;

/* JADX INFO: renamed from: net.afterday.compas.devices.vibro.-$$Lambda$VibroImpl$VibroOn$Th-Khv9iVAuUrYwYFykfVU1hpLU, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$VibroImpl$VibroOn$ThKhv9iVAuUrYwYFykfVU1hpLU implements Consumer {
    private final /* synthetic */ VibroImpl.VibroOn f$0;

    public /* synthetic */ $$Lambda$VibroImpl$VibroOn$ThKhv9iVAuUrYwYFykfVU1hpLU(VibroImpl.VibroOn vibroOn) {
        this.f$0 = vibroOn;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$vibrateDeath$4$VibroImpl$VibroOn((Long) obj);
    }
}

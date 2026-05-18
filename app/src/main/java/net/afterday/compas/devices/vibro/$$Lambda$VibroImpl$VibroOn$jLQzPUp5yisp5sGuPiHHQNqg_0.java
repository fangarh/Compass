package net.afterday.compas.devices.vibro;

import io.reactivex.functions.Consumer;
import net.afterday.compas.devices.vibro.VibroImpl;

/* JADX INFO: renamed from: net.afterday.compas.devices.vibro.-$$Lambda$VibroImpl$VibroOn$jLQzPUp5yisp5sGuPiHH-QNqg_0, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$VibroImpl$VibroOn$jLQzPUp5yisp5sGuPiHHQNqg_0 implements Consumer {
    private final /* synthetic */ VibroImpl.VibroOn f$0;

    public /* synthetic */ $$Lambda$VibroImpl$VibroOn$jLQzPUp5yisp5sGuPiHHQNqg_0(VibroImpl.VibroOn vibroOn) {
        this.f$0 = vibroOn;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$vibrateAlarm$7$VibroImpl$VibroOn((Long) obj);
    }
}

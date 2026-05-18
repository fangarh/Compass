package net.afterday.compas.devices.vibro;

import io.reactivex.functions.Consumer;
import net.afterday.compas.devices.vibro.VibroImpl;

/* JADX INFO: renamed from: net.afterday.compas.devices.vibro.-$$Lambda$VibroImpl$VibroOn$2zXoEzRva2l8AtUTcADzDUpKmsE, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$VibroImpl$VibroOn$2zXoEzRva2l8AtUTcADzDUpKmsE implements Consumer {
    private final /* synthetic */ VibroImpl.VibroOn f$0;

    public /* synthetic */ $$Lambda$VibroImpl$VibroOn$2zXoEzRva2l8AtUTcADzDUpKmsE(VibroImpl.VibroOn vibroOn) {
        this.f$0 = vibroOn;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$vibrateTouch$6$VibroImpl$VibroOn((Long) obj);
    }
}

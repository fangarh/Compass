package net.afterday.compas.engine.influences.BluetoothInfluences;

import io.reactivex.functions.Function;
import net.afterday.compas.sensors.Bluetooth.Bluetooth;

/* JADX INFO: renamed from: net.afterday.compas.engine.influences.BluetoothInfluences.-$$Lambda$BluetoothInfluenceProviderImpl$Rn60cUvzb2F3e4O8nroqocNCP-o, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$BluetoothInfluenceProviderImpl$Rn60cUvzb2F3e4O8nroqocNCPo implements Function {
    private final /* synthetic */ Bluetooth f$0;

    public /* synthetic */ $$Lambda$BluetoothInfluenceProviderImpl$Rn60cUvzb2F3e4O8nroqocNCPo(Bluetooth bluetooth) {
        this.f$0 = bluetooth;
    }

    @Override // io.reactivex.functions.Function
    public final Object apply(Object obj) {
        return BluetoothInfluenceProviderImpl.lambda$new$2(this.f$0, (String) obj);
    }
}

package net.afterday.compas.engine.influences.WifiInfluences;

import android.net.wifi.ScanResult;
import java.util.List;
import net.afterday.compas.core.influences.InfluencesPack;
import net.afterday.compas.engine.influences.InfluenceExtractionStrategy;

/* JADX INFO: loaded from: classes.dex */
public class MacIgnoringStrategy extends AbstractWifiExtractor implements InfluenceExtractionStrategy<List<ScanResult>, InfluencesPack> {
    @Override // net.afterday.compas.engine.influences.InfluenceExtractionStrategy
    public /* bridge */ /* synthetic */ InfluencesPack makeInfluences(List<ScanResult> list) {
        return makeInfluences2(list);
    }

    @Override // net.afterday.compas.engine.influences.WifiInfluences.AbstractWifiExtractor
    boolean isValid(ScanResult scanResult) {
        return true;
    }

    /* JADX INFO: renamed from: makeInfluences, reason: avoid collision after fix types in other method */
    public InfluencesPack makeInfluences2(List<ScanResult> i) {
        return extract(i);
    }
}

package net.afterday.compas.engine.influences.WifiInfluences;

import android.net.wifi.ScanResult;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.afterday.compas.core.influences.InfluencesPack;
import net.afterday.compas.engine.influences.InflPack;
import net.afterday.compas.engine.influences.InfluenceExtractionStrategy;

/* JADX INFO: loaded from: classes.dex */
public class ByFirstLetterExtractionStrategy extends AbstractWifiExtractor implements InfluenceExtractionStrategy<List<ScanResult>, InfluencesPack> {
    @Override // net.afterday.compas.engine.influences.InfluenceExtractionStrategy
    public /* bridge */ /* synthetic */ InfluencesPack makeInfluences(List<ScanResult> list) {
        return makeInfluences2(list);
    }

    /* JADX INFO: renamed from: makeInfluences, reason: avoid collision after fix types in other method */
    public InfluencesPack makeInfluences2(List<ScanResult> i) {
        InfluencesPack ip = new InflPack();
        Pattern regex = Pattern.compile("(.*?)(R|A|M|B|C|H|F|Z)");
        for (ScanResult sr : i) {
            Matcher matcher = regex.matcher(sr.SSID);
            if (matcher.find()) {
                String n = matcher.group(2);
                if (types.containsKey(n)) {
                    int tId = types.get(n).intValue();
                    ip.addInfluence(tId, WifiConverter.convert(tId, sr.level) * 1.0d);
                }
            }
        }
        return ip;
    }

    @Override // net.afterday.compas.engine.influences.WifiInfluences.AbstractWifiExtractor
    boolean isValid(ScanResult scanResult) {
        return true;
    }
}

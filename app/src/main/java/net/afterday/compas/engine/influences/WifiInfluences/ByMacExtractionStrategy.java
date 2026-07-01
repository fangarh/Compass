package net.afterday.compas.engine.influences.WifiInfluences;

import android.net.wifi.ScanResult;
import android.os.SystemClock;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.afterday.compas.core.influences.InfluencesPack;
import net.afterday.compas.engine.influences.InfluenceExtractionStrategy;

/* JADX INFO: loaded from: classes.dex */
public class ByMacExtractionStrategy extends AbstractWifiExtractor implements InfluenceExtractionStrategy<List<ScanResult>, InfluencesPack> {
    private static final long FRESH_SCAN_MS = 3500L;
    private static final int MIN_SIGNAL_DBM = -88;
    private Set<String> modules = new HashSet<>();

    @Override // net.afterday.compas.engine.influences.InfluenceExtractionStrategy
    public /* bridge */ /* synthetic */ InfluencesPack makeInfluences(List<ScanResult> list) {
        return makeInfluences2(list);
    }

    public ByMacExtractionStrategy(List<String> modules) {
        if (modules == null) {
            return;
        }
        for (String module : modules) {
            String normalized = normalizeMac(module);
            if (normalized != null) {
                this.modules.add(normalized);
            }
        }
    }

    /* JADX INFO: renamed from: makeInfluences, reason: avoid collision after fix types in other method */
    public InfluencesPack makeInfluences2(List<ScanResult> i) {
        return extract(i);
    }

    @Override // net.afterday.compas.engine.influences.WifiInfluences.AbstractWifiExtractor
    boolean isValid(ScanResult scanResult) {
        if (scanResult == null || scanResult.level < MIN_SIGNAL_DBM || !isFresh(scanResult)) {
            return false;
        }
        String bssid = normalizeMac(scanResult.BSSID);
        return bssid != null && this.modules.contains(bssid);
    }

    private boolean isFresh(ScanResult scanResult) {
        if (scanResult.timestamp <= 0L) {
            return true;
        }
        long seenElapsedMs = scanResult.timestamp / 1000L;
        long ageMs = SystemClock.elapsedRealtime() - seenElapsedMs;
        return ageMs <= FRESH_SCAN_MS;
    }

    private String normalizeMac(String mac) {
        if (mac == null) {
            return null;
        }
        String normalized = mac.trim().toLowerCase(Locale.US);
        return normalized.length() == 0 ? null : normalized;
    }
}

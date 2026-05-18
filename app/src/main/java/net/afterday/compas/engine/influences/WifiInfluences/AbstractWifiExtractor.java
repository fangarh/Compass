package net.afterday.compas.engine.influences.WifiInfluences;

import android.net.wifi.ScanResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.afterday.compas.core.influences.InfluencesPack;
import net.afterday.compas.engine.influences.InflPack;

/* JADX INFO: loaded from: classes.dex */
public abstract class AbstractWifiExtractor {
    protected static Map<String, Integer> types = new HashMap();
    private Pattern regex = Pattern.compile("(.*?([RAMBCHFZ])((\\d+)))");

    abstract boolean isValid(ScanResult scanResult);

    static {
        types.put("R", 0);
        types.put("H", 5);
        types.put("A", 1);
        types.put("M", 2);
        types.put("B", 3);
        types.put("C", 4);
        types.put("F", 6);
        types.put("Z", 7);
    }

    protected InfluencesPack extract(List<ScanResult> scanResults) {
        double multiplier;
        InflPack ip = new InflPack();
        for (ScanResult sr : scanResults) {
            if (isValid(sr)) {
                Matcher matcher = this.regex.matcher(sr.SSID);
                while (matcher.find() && matcher.groupCount() >= 4) {
                    String n = matcher.group(2);
                    if (types.containsKey(n)) {
                        try {
                            int number = Integer.parseInt(matcher.group(3));
                            int tId = types.get(n).intValue();
                            if (tId != 5 || Math.abs(sr.level) <= number) {
                                if (number > 0) {
                                    double d = number;
                                    Double.isNaN(d);
                                    multiplier = d / 100.0d;
                                } else {
                                    multiplier = 1.0d;
                                }
                                ip.addInfluence(tId, WifiConverter.convert(tId, sr.level) * multiplier);
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
        return ip;
    }
}

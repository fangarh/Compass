package net.afterday.compas.engine.influences;

import java.util.Map;
import net.afterday.compas.persistency.Source;

/* JADX INFO: loaded from: classes.dex */
public class InfluencesMap implements Source {
    private Map<String, InfluenceMapper> map;

    public InfluencesMap(Map<String, InfluenceMapper> map) {
        this.map = map;
    }

    public Map<String, InfluenceMapper> getMap() {
        return this.map;
    }
}

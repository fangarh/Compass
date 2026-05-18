package net.afterday.compas.persistency.influences;

import java.util.List;
import net.afterday.compas.core.influences.Emission;
import net.afterday.compas.core.influences.Influence;

/* JADX INFO: loaded from: classes.dex */
public interface InfluencesPersistency {
    List<Emission> getEmissions();

    List<Influence> getPossibleInfluences();

    List<String> getRegisteredWifiModules();
}

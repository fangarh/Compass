package net.afterday.compas.persistency.hardcoded;

import java.util.HashMap;
import java.util.Map;
import net.afterday.compas.core.player.Player;
import net.afterday.compas.persistency.player.PlayerPersistency;

/* JADX INFO: loaded from: classes.dex */
public class HPlayerPersistency implements PlayerPersistency {
    private Map<String, Player.FRACTION> fractions = new HashMap();
    private Map<String, Player.COMMAND> commands = new HashMap();

    public HPlayerPersistency() {
        setupFractions();
        setupCommands();
    }

    @Override // net.afterday.compas.persistency.player.PlayerPersistency
    public Player.FRACTION getFractionByCode(String code) {
        if (this.fractions.containsKey(code)) {
            return this.fractions.get(code);
        }
        return null;
    }

    @Override // net.afterday.compas.persistency.player.PlayerPersistency
    public Player.COMMAND getCommandByCode(String code) {
        if (this.commands.containsKey(code)) {
            return this.commands.get(code);
        }
        return null;
    }

    private void setupFractions() {
        this.fractions.put("monolithknowledgeandlight1", Player.FRACTION.MONOLITH);
        this.fractions.put("supremecreator1", Player.FRACTION.GAMEMASTER);
        this.fractions.put("sampleofradioactivemeat1", Player.FRACTION.STALKER);
        this.fractions.put("DARKEN1", Player.FRACTION.DARKEN);
    }

    private void setupCommands() {
        this.commands.put("eblan1", Player.COMMAND.KILL);
        this.commands.put("risefromthedead1", Player.COMMAND.REVIVE);
    }
}

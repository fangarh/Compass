package net.afterday.compas.core;

import net.afterday.compas.core.fraction.Fraction;
import net.afterday.compas.core.gameState.Frame;
import net.afterday.compas.core.gameState.FrameImpl;
import net.afterday.compas.core.influences.InfluencesPack;
import net.afterday.compas.core.inventory.Inventory;
import net.afterday.compas.core.inventory.InventoryImpl;
import net.afterday.compas.core.inventory.items.Events.DropItem;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.core.player.Player;
import net.afterday.compas.core.player.PlayerImpl;
import net.afterday.compas.core.serialization.Serializer;
import net.afterday.compas.persistency.PersistencyProvider;
import net.afterday.compas.persistency.items.ItemDescriptor;

/* JADX INFO: loaded from: classes.dex */
public class GameImpl implements Game {
    private PlayerImpl mPlayer;
    private PersistencyProvider persistencyProvider;
    private Controls controls;

    public GameImpl(PersistencyProvider persistencyProvider, Serializer serializer) {
        this.persistencyProvider = persistencyProvider;
        this.mPlayer = new PlayerImpl(new InventoryImpl(persistencyProvider.getItemsPersistency(), serializer), serializer);
    }

    @Override // net.afterday.compas.core.Game
    public Frame start() {
        return new FrameImpl(this.mPlayer.getPlayerProps());
    }

    @Override // net.afterday.compas.core.Game
    public Frame acceptInfluences(InfluencesPack influencesPack) {
        return this.mPlayer.acceptInfluences(influencesPack);
    }

    @Override // net.afterday.compas.core.Game
    public Player getPlayer() {
        return this.mPlayer;
    }

    @Override // net.afterday.compas.core.Game
    public Inventory getInventory() {
        return this.mPlayer.getInventory();
    }

    @Override // net.afterday.compas.core.Game
    public boolean acceptCode(String code) {
        ItemDescriptor itemDesc = this.persistencyProvider.getItemsPersistency().getItemForCode(code);
        if (itemDesc != null) {
            return this.mPlayer.addItem(itemDesc, code);
        }
        Player.FRACTION fraction = this.persistencyProvider.getPlayerPersistency().getFractionByCode(code);
        if (fraction != null) {
            this.mPlayer.setFraction(fraction);
            return true;
        }
        Player.COMMAND command = this.persistencyProvider.getPlayerPersistency().getCommandByCode(code);
        if (command != null) {
            int i = AnonymousClass1.$SwitchMap$net$afterday$compas$core$player$Player$COMMAND[command.ordinal()];
            if (i == 1) {
                this.mPlayer.setState(Player.STATE.DEAD_BURER);
                return true;
            }
            if (i == 2) {
                this.mPlayer.reborn();
                return true;
            }
            return false;
        }
        return false;
    }

    /* JADX INFO: renamed from: net.afterday.compas.core.GameImpl$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$net$afterday$compas$core$player$Player$COMMAND = new int[Player.COMMAND.values().length];

        static {
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$COMMAND[Player.COMMAND.KILL.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$net$afterday$compas$core$player$Player$COMMAND[Player.COMMAND.REVIVE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    @Override // net.afterday.compas.core.Game
    public Frame useItem(Item item) {
        return this.mPlayer.useItem(item);
    }

    public Frame dropItem(DropItem dropItem) {
        return null;
    }

    public Frame changeFraction(Fraction fraction) {
        return null;
    }
}

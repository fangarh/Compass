package net.afterday.compas.core.player;

import android.util.Log;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.afterday.compas.core.events.PlayerEventsListener;
import net.afterday.compas.core.gameState.Frame;
import net.afterday.compas.core.gameState.FrameImpl;
import net.afterday.compas.core.influences.Influences;
import net.afterday.compas.core.influences.InfluencesPack;
import net.afterday.compas.core.inventory.Inventory;
import net.afterday.compas.core.inventory.items.Events.ItemAdded;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.core.player.Impacts;
import net.afterday.compas.core.player.Player;
import net.afterday.compas.core.serialization.Jsonable;
import net.afterday.compas.core.serialization.Serializer;
import net.afterday.compas.persistency.items.ItemDescriptor;

/* JADX INFO: loaded from: classes.dex */
public class PlayerImpl implements Player {
    private static final long MINUTE = 60000;
    private static final String PLAYER = "player";
    private static final String TAG = "PlayerImpl";
    private double hBefore;
    private ImpactsImpl impacts;
    private Impacts.STATE impactsState;
    private Inventory mInventory;
    private PlayerProps mPlayerProps;
    private JsonObject o;
    private Player.STATE playerState;
    private double rBefore;
    private Serializer serializer;
    private long mLastInfls = System.currentTimeMillis();
    private List<PlayerEventsListener> playerEventsListeners = new ArrayList();

    public PlayerImpl(Inventory inventory, Serializer serializer) {
        this.serializer = serializer;
        Jsonable jso = serializer.deserialize(PLAYER);
        int xp = 1;
        double health = 100.0d;
        double rad = 0.0d;
        this.playerState = Player.STATE.ALIVE;
        Player.FRACTION f = Player.FRACTION.STALKER;
        if (jso != null) {
            this.o = jso.toJson();
            health = this.o.has("health") ? this.o.get("health").getAsDouble() : 100.0d;
            rad = this.o.has(Influences.RADIATION) ? this.o.get(Influences.RADIATION).getAsDouble() : 0.0d;
            if (this.o.has("state")) {
                this.playerState = Player.STATE.fromString(this.o.get("state").getAsString());
            }
            f = this.o.has("fraction") ? Player.FRACTION.fromString(this.o.get("fraction").getAsString()) : f;
            if (this.o.has("xpPoints")) {
                xp = this.o.get("xpPoints").getAsInt();
            }
        } else {
            this.o = new JsonObject();
            this.o.addProperty("health", Double.valueOf(100.0d));
            this.o.addProperty(Influences.RADIATION, Double.valueOf(0.0d));
            this.o.addProperty("state", this.playerState.toString());
            this.o.addProperty("xpPoints", (Number) 1);
            this.o.addProperty("fraction", f.toString());
        }
        this.hBefore = health;
        this.rBefore = rad;
        this.mPlayerProps = new PlayerPropsImpl(this.playerState);
        this.mPlayerProps.setFraction(f);
        ((PlayerPropsImpl) this.mPlayerProps).setHasHealthInstant(inventory.hasHealthInstant());
        ((PlayerPropsImpl) this.mPlayerProps).setHasRadiationInstant(inventory.hasRadiationInstant());
        if (inventory.hasActiveBooster()) {
            this.mPlayerProps.setBoosterPercents(inventory.getActiveBooster().getPercentsLeft());
        }
        if (inventory.hasActiveDevice()) {
            this.mPlayerProps.setDevicePercents(inventory.getActiveDevice().getPercentsLeft());
        }
        if (inventory.hasActiveArmor()) {
            this.mPlayerProps.setArmorPercents(inventory.getActiveArmor().getPercentsLeft());
        }
        ((PlayerPropsImpl) this.mPlayerProps).setHealth(health);
        ((PlayerPropsImpl) this.mPlayerProps).setRadiation(rad);
        ((PlayerPropsImpl) this.mPlayerProps).setXpPoints(xp);
        this.mInventory = inventory;
        this.impacts = new ImpactsImpl(this.mPlayerProps, serializer);
    }

    public Frame acceptInfluences(InfluencesPack inflPack, long delta) {
        this.impacts.prepare(inflPack, delta);
        this.impacts.artifactsImpact(this.mInventory.getArtifacts());
        if (this.mInventory.hasActiveBooster()) {
            this.impacts.boosterImpact(this.mInventory.consumeBooster(delta));
        }
        if (this.mInventory.hasActiveDevice()) {
            this.impacts.deviceImpact(this.mInventory.consumeDevice(delta));
        }
        if (this.impacts.getState() == Impacts.STATE.HEALING) {
            this.impacts.healByInfluence(delta);
            return makeFrame(this.impacts);
        }
        if (this.playerState.getCode() != 1) {
            return makeFrame(this.impacts);
        }
        if (this.impacts.getState() == Impacts.STATE.CLEAR) {
            if (this.mInventory.hasActiveArmor() && this.mInventory.getActiveArmor().hasModifier(0)) {
                this.impacts.armorImpact(this.mInventory.getActiveArmor());
            }
            this.impacts.calculateAccumulated(delta);
            return makeFrame(this.impacts);
        }
        if (this.impacts.getState() == Impacts.STATE.DAMAGE) {
            if (this.mInventory.hasActiveArmor()) {
                this.impacts.armorImpact(this.mInventory.consumeArmor(delta));
            }
            this.impacts.calculateAccumulated(delta);
            this.impacts.calculateEnvDamage(delta);
            Log.e(TAG, "!!!!!!!!!!!!!!!! acceptInfluences 6 " + Thread.currentThread().getName() + " -- " + this.impacts);
            return makeFrame(this.impacts);
        }
        return new FrameImpl(this.mPlayerProps);
    }

    private Frame makeFrame(ImpactsImpl impacts) {
        boolean dirty = false;
        this.mPlayerProps = impacts.getPlayerProps();
        Player.STATE ps = this.mPlayerProps.getState();
        if (ps != this.playerState) {
            this.o.addProperty("state", ps.toString());
            dirty = true;
        }
        if (this.mPlayerProps.getRadiation() != this.rBefore) {
            this.rBefore = this.mPlayerProps.getRadiation();
            this.o.addProperty(Influences.RADIATION, Double.valueOf(this.rBefore));
            dirty = true;
        }
        if (this.mPlayerProps.getHealth() != this.hBefore) {
            this.hBefore = this.mPlayerProps.getHealth();
            this.o.addProperty("health", Double.valueOf(this.hBefore));
            dirty = true;
        }
        if (dirty) {
            this.serializer.serialize(PLAYER, this);
        }
        validateState(this.playerState, ps);
        Impacts.STATE is = impacts.getState();
        validateImpactsState(this.impactsState, is);
        this.playerState = ps;
        this.impactsState = is;
        return new FrameImpl(this.mPlayerProps);
    }

    private void validateImpactsState(Impacts.STATE prevState, Impacts.STATE newState) {
        if (prevState == newState) {
            return;
        }
        for (PlayerEventsListener l : this.playerEventsListeners) {
            l.onImpactsStateChanged(prevState, newState);
        }
    }

    private void validateState(Player.STATE prevState, Player.STATE newState) {
        if (prevState == newState) {
            return;
        }
        for (PlayerEventsListener l : this.playerEventsListeners) {
            l.onPlayerStateChanged(prevState, newState);
        }
    }

    public Frame acceptInfluences(InfluencesPack inflPack) {
        long now = System.currentTimeMillis();
        long delta = now - this.mLastInfls;
        this.mLastInfls = now;
        return acceptInfluences(inflPack, delta);
    }

    @Override // net.afterday.compas.core.player.Player
    public PlayerProps getPlayerProps() {
        return this.mPlayerProps;
    }

    @Override // net.afterday.compas.core.player.Player
    public Inventory getInventory() {
        return this.mInventory;
    }

    @Override // net.afterday.compas.core.player.Player
    public boolean addItem(ItemDescriptor i, String code) {
        Item item = this.mInventory.addItem(i, code);
        if (item != null) {
            boolean changed = this.mPlayerProps.addXpPoints(item.getItemDescriptor().getXpPoints());
            ItemAddedEvent e = new ItemAddedEvent(item);
            ItemAddedEvent.access$000(e, this.mPlayerProps.getLevelXp());
            ItemAddedEvent.access$100(e, this.mPlayerProps.getLevel());
            ItemAddedEvent.access$200(e, changed);
            this.mInventory.setPlayerLevel(e.getLevel());
            this.o.addProperty("xpPoints", Integer.valueOf(this.mPlayerProps.getXpPoints()));
            this.serializer.serialize(PLAYER, this);
            for (PlayerEventsListener pli : this.playerEventsListeners) {
                if (changed) {
                    pli.onPlayerLevelChanged(this.mPlayerProps.getLevel());
                }
                pli.onItemAdded(e);
            }
        }
        setInstants();
        return item != null;
    }

    @Override // net.afterday.compas.core.player.Player
    public boolean dropItem(Item item) {
        boolean dropped = this.mInventory.dropItem(item);
        setInstants();
        if (dropped) {
            for (PlayerEventsListener l : this.playerEventsListeners) {
                l.onItemDropped(item);
            }
        }
        return dropped;
    }

    @Override // net.afterday.compas.core.player.Player
    public Frame useItem(Item item) {
        this.mPlayerProps = this.mInventory.useItem(item, this.mPlayerProps);
        setInstants();
        for (PlayerEventsListener l : this.playerEventsListeners) {
            l.onItemUsed(item);
        }
        return new FrameImpl(this.mPlayerProps);
    }

    private void setInstants() {
        ((PlayerPropsImpl) this.mPlayerProps).setHasHealthInstant(this.mInventory.hasHealthInstant());
        ((PlayerPropsImpl) this.mPlayerProps).setHasRadiationInstant(this.mInventory.hasRadiationInstant());
    }

    @Override // net.afterday.compas.core.player.Player
    public void addPlayerEventsListener(PlayerEventsListener playerEventsListener) {
        this.playerEventsListeners.add(playerEventsListener);
    }

    @Override // net.afterday.compas.core.player.Player
    public Frame setState(Player.STATE state) {
        Log.e(TAG, "-*-*-*-*-*-*-*setState: " + state);
        getPlayerProps().setState(state);
        if (state.getCode() == 4) {
            ((PlayerPropsImpl) this.mPlayerProps).setHealth(0.0d);
            this.o.addProperty("health", (Number) 0);
        }
        validateState(this.playerState, state);
        this.playerState = state;
        this.o.addProperty("state", state.toString());
        this.serializer.serialize(PLAYER, this);
        return new FrameImpl(this.mPlayerProps);
    }

    @Override // net.afterday.compas.core.player.Player
    public boolean setFraction(Player.FRACTION fraction) {
        Player.FRACTION f = this.mPlayerProps.getFraction();
        if (f == fraction) {
            return false;
        }
        if (this.mPlayerProps.setFraction(fraction)) {
            for (PlayerEventsListener l : this.playerEventsListeners) {
                l.onFractionChanged(fraction, f);
            }
        }
        this.o.addProperty("fraction", fraction.toString());
        this.serializer.serialize(PLAYER, this);
        return true;
    }

    @Override // net.afterday.compas.core.player.Player
    public boolean reborn() {
        this.mPlayerProps.addHealth(100.0d);
        this.mPlayerProps.setRadiation(0.0d);
        this.o.addProperty("health", (Number) 100);
        this.o.addProperty(Influences.RADIATION, (Number) 0);
        setState(Player.STATE.ALIVE);
        return true;
    }

    @Override // net.afterday.compas.core.serialization.Jsonable
    public JsonObject toJson() {
        return this.o;
    }

    private static class ItemAddedEvent implements ItemAdded {
        private Item item;
        private int level;
        private boolean levelChanged;
        private int levelXpPercents;
        private int xp;

        static /* synthetic */ void access$000(ItemAddedEvent x0, int x1) {
            x0.setLevelXpPercents(x1);
        }

        static /* synthetic */ void access$100(ItemAddedEvent x0, int x1) {
            x0.setLevel(x1);
        }

        static /* synthetic */ void access$200(ItemAddedEvent x0, boolean x1) {
            x0.setLevelChanged(x1);
        }

        ItemAddedEvent(Item item) {
            this.item = item;
        }

        @Override // net.afterday.compas.core.player.XpChanged
        public boolean levelChanged() {
            return this.levelChanged;
        }

        @Override // net.afterday.compas.core.player.XpChanged
        public int getLevel() {
            return this.level;
        }

        @Override // net.afterday.compas.core.player.XpChanged
        public int getXp() {
            return this.xp;
        }

        @Override // net.afterday.compas.core.player.XpChanged
        public int getLevelXpPercents() {
            return this.levelXpPercents;
        }

        @Override // net.afterday.compas.core.inventory.items.Events.ItemAdded
        public Item getItem() {
            return this.item;
        }

        private void setLevelChanged(boolean levelChanged) {
            this.levelChanged = levelChanged;
        }

        private void setLevelXpPercents(int xpPercents) {
            this.levelXpPercents = xpPercents;
        }

        private void setXp(int xp) {
            this.xp = xp;
        }

        private void setLevel(int level) {
            this.level = level;
        }
    }
}

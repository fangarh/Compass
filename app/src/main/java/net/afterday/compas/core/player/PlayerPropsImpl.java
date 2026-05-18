package net.afterday.compas.core.player;

import android.util.Log;
import com.google.gson.JsonObject;
import net.afterday.compas.core.player.Player;
import net.afterday.compas.core.serialization.Jsonable;

/* JADX INFO: loaded from: classes.dex */
public class PlayerPropsImpl implements PlayerProps {
    public static final int LEVEL_XP = 50;
    private static final String TAG = "PlayerPropsImpl";
    private boolean anomalyHit;
    private double armorPercents;
    private double boosterPercents;
    private boolean burerHit;
    private boolean controllerHit;
    private double devicePercents;
    private boolean emissionHit;
    private Player.FRACTION fraction;
    private boolean hasHealthInstant;
    private boolean hasRadiationInstant;
    private double health;
    private double[] impacts;
    private boolean mentalHit;
    private boolean monolithHit;
    private JsonObject o;
    private double radiation;
    private double radiationImpact;
    private Player.STATE state;
    private int xpPoints;

    public PlayerPropsImpl(Player.STATE state) {
        this.boosterPercents = 0.0d;
        this.devicePercents = 0.0d;
        this.armorPercents = 0.0d;
        this.state = state;
    }

    public PlayerPropsImpl(PlayerProps pProps) {
        this.boosterPercents = 0.0d;
        this.devicePercents = 0.0d;
        this.armorPercents = 0.0d;
        this.state = pProps.getState();
        this.radiation = pProps.getRadiation();
        this.health = pProps.getHealth();
        this.boosterPercents = pProps.getBoosterPercents();
        this.devicePercents = pProps.getDevicePercents();
        this.armorPercents = pProps.getArmorPercents();
        this.xpPoints = pProps.getXpPoints();
        this.hasHealthInstant = pProps.hasHealthInstant();
        this.hasRadiationInstant = pProps.hasRadiationInstant();
        this.fraction = pProps.getFraction();
    }

    public PlayerPropsImpl(Jsonable jsonable) {
        this.boosterPercents = 0.0d;
        this.devicePercents = 0.0d;
        this.armorPercents = 0.0d;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public double getHealth() {
        return this.health;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public double getRadiation() {
        return this.radiation;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public double getArtefactImpact() {
        double[] dArr = this.impacts;
        if (dArr != null && dArr.length > 6) {
            return dArr[6];
        }
        return 0.0d;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public long getController() {
        return 0L;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public long getZombified() {
        return 0L;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public double getMental() {
        return 0.0d;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public double getRadiationImpact() {
        double[] dArr = this.impacts;
        if (dArr != null && dArr.length >= 0) {
            return dArr[0];
        }
        return 0.0d;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public double getHealthImpact() {
        if (this.impacts != null) {
            if (this.fraction == Player.FRACTION.MONOLITH) {
                double[] dArr = this.impacts;
                if (dArr.length >= 7) {
                    return dArr[7];
                }
            }
            if (this.fraction == Player.FRACTION.DARKEN) {
                double[] dArr2 = this.impacts;
                if (dArr2.length >= 0) {
                    return dArr2[0];
                }
            }
            double[] dArr3 = this.impacts;
            if (dArr3.length >= 5) {
                return dArr3[5];
            }
            return 0.0d;
        }
        return 0.0d;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public double getControllerImpact() {
        double[] dArr = this.impacts;
        if (dArr != null && dArr.length >= 4) {
            return dArr[4];
        }
        return 0.0d;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public double getBurerImpact() {
        double[] dArr = this.impacts;
        if (dArr != null && dArr.length >= 3) {
            return dArr[3];
        }
        return 0.0d;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public double getMentalImpact() {
        double[] dArr = this.impacts;
        if (dArr != null && dArr.length >= 2) {
            return dArr[2];
        }
        return 0.0d;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public double getMonolithImpact() {
        double[] dArr = this.impacts;
        if (dArr != null && dArr.length >= 7) {
            return dArr[7];
        }
        return 0.0d;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public double getAnomalyImpact() {
        double[] dArr = this.impacts;
        if (dArr != null && dArr.length >= 1) {
            return dArr[1];
        }
        return 0.0d;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public double getBoosterPercents() {
        return this.boosterPercents;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public double getDevicePercents() {
        return this.devicePercents;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public double getArmorPercents() {
        return this.armorPercents;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public void addHealth(double health) {
        setHealth(this.health + health);
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public void addRadiation(double radiation) {
        setRadiation(this.radiation + radiation);
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public boolean addXpPoints(int xp) {
        int oldLevel = getLevel();
        this.xpPoints += xp;
        return oldLevel != getLevel();
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public void setXpPoints(int xp) {
        this.xpPoints = xp;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public int getXpPoints() {
        return this.xpPoints;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public int getLevel() {
        return calcLevel(this.xpPoints);
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public void subtractHealth(double health) {
        setHealth(this.health - health);
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public void subtractRadiation(double radiation) {
        setRadiation(this.radiation - radiation);
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public void setBoosterPercents(double percents) {
        this.boosterPercents = normalize(percents);
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public void setDevicePercents(double percents) {
        this.devicePercents = normalize(percents);
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public void setArmorPercents(double percents) {
        this.armorPercents = normalize(percents);
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public Player.STATE getState() {
        return this.state;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public void setState(Player.STATE state) {
        this.state = state;
    }

    private int calcLevel(int xp) {
        return Math.min((xp / 50) + 1, 5);
    }

    public void setHealth(double health) {
        if (health > 100.0d) {
            health = 100.0d;
        } else if (health < 0.0d) {
            health = 0.0d;
        }
        Log.d(TAG, "setHealth: " + health);
        this.health = health;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public void setRadiation(double radiation) {
        if (radiation > 16.0d) {
            radiation = 16.0d;
        } else if (radiation < 0.0d) {
            radiation = 0.0d;
        }
        this.radiation = radiation;
    }

    public void setAnomalyHit(boolean anomalyHit) {
        this.anomalyHit = anomalyHit;
    }

    public void setRadiationImpact(double radiationImpact) {
        this.radiationImpact = radiationImpact;
    }

    public void setImpacts(double[] influences) {
        this.impacts = influences;
    }

    private double normalize(double number) {
        if (number > 100.0d) {
            return 100.0d;
        }
        if (number < 0.0d) {
            return 0.0d;
        }
        return number;
    }

    public void setBurerHit(boolean hit) {
        this.burerHit = hit;
    }

    public void setControllerHit(boolean hit) {
        this.controllerHit = hit;
    }

    public void setMentalHit(boolean hit) {
        this.mentalHit = hit;
    }

    public void setMonolithHit(boolean hit) {
        this.monolithHit = hit;
    }

    public void setEmissionHit(boolean hit) {
        this.emissionHit = true;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public boolean burerHit() {
        return this.burerHit;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public boolean controllerHit() {
        return this.controllerHit;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public boolean anomalyHit() {
        return this.anomalyHit;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public boolean mentalHit() {
        return this.mentalHit;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public boolean monolithHit() {
        return this.monolithHit;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public boolean emissionHit() {
        return this.emissionHit;
    }

    public void setHasHealthInstant(boolean hasHealthInstant) {
        this.hasHealthInstant = hasHealthInstant;
    }

    public void setHasRadiationInstant(boolean hasRadiationInstant) {
        this.hasRadiationInstant = hasRadiationInstant;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public boolean hasHealthInstant() {
        return this.hasHealthInstant;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public boolean hasRadiationInstant() {
        return this.hasRadiationInstant;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public boolean setFraction(Player.FRACTION fraction) {
        if (this.fraction == fraction) {
            return false;
        }
        this.fraction = fraction;
        return true;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public Player.FRACTION getFraction() {
        return this.fraction;
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public int getLevelXp() {
        return (this.xpPoints % 50) * 2;
    }

    public String toString() {
        String str = "Player props:\nRadiation: " + this.radiation + ",\n";
        return ((str + "RadiationImpact: " + this.radiationImpact + ",\n") + "Health: " + this.health + ",\n") + "State: " + this.state.toString();
    }

    @Override // net.afterday.compas.core.player.PlayerProps
    public double[] getImpacts() {
        return this.impacts;
    }

    @Override // net.afterday.compas.core.serialization.Jsonable
    public JsonObject toJson() {
        return null;
    }
}

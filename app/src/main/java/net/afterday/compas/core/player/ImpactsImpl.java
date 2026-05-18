package net.afterday.compas.core.player;

import android.util.Log;
import net.afterday.compas.core.influences.InfluencesPack;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.core.player.Impacts;
import net.afterday.compas.core.player.Player;
import net.afterday.compas.core.serialization.Serializer;
import net.afterday.compas.util.Convert;

/* JADX INFO: loaded from: classes.dex */
public class ImpactsImpl implements Impacts {
    private static final int ANOMALY_COOLDOWN = 5000;
    private static final int BURER_COOLDOWN = 5000;
    private static final int CONTROLLER_COOLDOWN = 2000;
    private static final int EMISSION_COOLDOWN = 2000;
    private static final int HOUR = 3600000;
    private static final int MENTAL_COOLDOWN = 10000;
    private static final int MINUTE = 60000;
    private static final int MONOLITH_COOLDOWN = 20000;
    private static final String PLAYER_PROPS = "playerProps";
    private static final String TAG = "ImpactsImpl";
    private long delta;
    private InfluencesPack inflPack;
    private double[] mImpacts;
    private PlayerPropsImpl newProps;
    private PlayerProps oldProps;
    private Impacts.STATE state;
    private double healthModifier = 1.0d;
    private double radModifier = 1.0d;
    private long accumulatedBurer = 0;
    private long accumulatedController = 0;
    private long accumulatedAnomaly = 0;
    private long accumulatedMental = 0;
    private long accumulatedMonolith = 0;
    private long accumulatedEmission = 0;

    public ImpactsImpl(PlayerProps playerProps, Serializer serializer) {
        this.newProps = (PlayerPropsImpl) playerProps;
    }

    public void prepare(InfluencesPack inflPack, long delta) {
        this.state = null;
        if (this.newProps.getState().getCode() == 4 && isHealing(this.newProps, inflPack)) {
            this.newProps.setState(Player.STATE.ALIVE);
        }
        this.inflPack = inflPack;
        this.oldProps = this.newProps;
        this.newProps = new PlayerPropsImpl(this.oldProps);
        this.mImpacts = inflPack.getInfluences();
        this.delta = delta;
        this.healthModifier = 1.0d;
        this.radModifier = 1.0d;
    }

    public void healByInfluence(long delta) {
        PlayerPropsImpl playerPropsImpl = this.newProps;
        double health = playerPropsImpl.getHealth();
        double d = delta;
        Double.isNaN(d);
        playerPropsImpl.setHealth(health + ((d / 60000.0d) * 2.0d));
        if (this.newProps.getHealth() >= 30.0d) {
            PlayerPropsImpl playerPropsImpl2 = this.newProps;
            double radiation = playerPropsImpl2.getRadiation();
            double d2 = this.radModifier * 3.0d;
            double d3 = delta;
            Double.isNaN(d3);
            playerPropsImpl2.setRadiation(radiation - (d2 * (d3 / 60000.0d)));
        }
    }

    public void calculateAccumulated(long delta) {
        if (this.newProps.getState().getCode() == 4) {
            return;
        }
        if (this.newProps.getRadiation() > 1.0d || this.mImpacts[0] > 0.0d || this.state == Impacts.STATE.DAMAGE) {
            if (this.newProps.getRadiation() > 1.0d) {
                double radMod = getRadMod(this.newProps.getRadiation());
                PlayerPropsImpl playerPropsImpl = this.newProps;
                double health = playerPropsImpl.getHealth();
                double d = delta;
                Double.isNaN(d);
                playerPropsImpl.setHealth(health - ((d / 60000.0d) * radMod));
                if (this.newProps.getHealth() <= 0.0d) {
                    this.newProps.setState(Player.STATE.W_DEAD_RADIATION);
                    return;
                }
                return;
            }
            return;
        }
        PlayerPropsImpl playerPropsImpl2 = this.newProps;
        double health2 = playerPropsImpl2.getHealth();
        double d2 = this.healthModifier * 0.05d;
        double d3 = delta;
        Double.isNaN(d3);
        playerPropsImpl2.setHealth(health2 + (d2 * (d3 / 60000.0d)));
        PlayerPropsImpl playerPropsImpl3 = this.newProps;
        double radiation = playerPropsImpl3.getRadiation();
        double d4 = this.radModifier * 0.005d;
        double d5 = delta;
        Double.isNaN(d5);
        playerPropsImpl3.setRadiation(radiation - (d4 * (d5 / 60000.0d)));
    }

    private double getHealthToSet(int inflId, long delta, double strength) {
        if (strength >= 16.0d) {
            if (inflId == 0) {
                double health = this.newProps.getHealth();
                double d = delta;
                Double.isNaN(d);
                return health - ((d / 60000.0d) * 10.0d);
            }
            double health2 = this.newProps.getHealth();
            double d2 = delta;
            Double.isNaN(d2);
            return health2 - ((d2 / 60000.0d) * 4.0d);
        }
        if (strength >= 7.0d) {
            double health3 = this.newProps.getHealth();
            double d3 = delta;
            Double.isNaN(d3);
            return health3 - ((d3 / 60000.0d) * 2.0d);
        }
        if (strength >= 1.0d) {
            double health4 = this.newProps.getHealth();
            double d4 = delta;
            Double.isNaN(d4);
            return health4 - ((d4 / 60000.0d) * 1.0d);
        }
        if (strength >= 0.1d) {
            double health5 = this.newProps.getHealth();
            double d5 = delta;
            Double.isNaN(d5);
            return health5 - ((d5 / 60000.0d) * 0.5d);
        }
        return this.newProps.getHealth();
    }

    private double getRadiationToSet(long delta, double strength) {
        double[] dArr = this.mImpacts;
        if (dArr[0] >= 16.0d) {
            double radiation = this.newProps.getRadiation();
            double d = delta;
            Double.isNaN(d);
            return radiation + ((d / 3600000.0d) * 1000.0d);
        }
        if (dArr[0] <= 0.1d) {
            return this.newProps.getRadiation();
        }
        double radiation2 = this.newProps.getRadiation();
        double d2 = this.mImpacts[0];
        double d3 = delta;
        Double.isNaN(d3);
        return radiation2 + (d2 * (d3 / 3600000.0d));
    }

    private boolean hit(int infl, long delta) {
        Log.e(TAG, "TryToHit: " + infl);
        if (infl == 1) {
            this.accumulatedAnomaly += delta;
            if (this.accumulatedAnomaly < 5000) {
                return false;
            }
            this.accumulatedAnomaly = 0L;
            this.newProps.subtractHealth(30.0d);
            if (this.newProps.getHealth() <= 0.0d) {
                this.newProps.setState(Player.STATE.W_DEAD_ANOMALY);
            }
            this.newProps.setAnomalyHit(true);
            return true;
        }
        if (infl == 2) {
            if (this.newProps.getFraction() == Player.FRACTION.MONOLITH) {
                return false;
            }
            this.accumulatedMental += delta;
            if (this.accumulatedMental < 10000) {
                return false;
            }
            this.accumulatedMental = 0L;
            this.newProps.subtractHealth(30.0d);
            if (this.newProps.getHealth() <= 0.0d) {
                this.newProps.setState(Player.STATE.W_MENTALLED);
            }
            this.newProps.setMentalHit(true);
            return true;
        }
        if (infl == 3) {
            this.accumulatedBurer += delta;
            if (this.accumulatedBurer < 5000) {
                return false;
            }
            this.accumulatedBurer = 0L;
            this.newProps.subtractHealth(10.0d);
            if (this.newProps.getHealth() <= 0.0d) {
                this.newProps.setState(Player.STATE.W_DEAD_BURER);
            }
            this.newProps.setBurerHit(true);
            return true;
        }
        if (infl == 4) {
            this.accumulatedController += delta;
            if (this.accumulatedController < 2000) {
                return false;
            }
            this.accumulatedController = 0L;
            this.newProps.subtractHealth(50.0d);
            if (this.newProps.getHealth() <= 0.0d) {
                this.newProps.setState(Player.STATE.W_CONTROLLED);
            }
            this.newProps.setControllerHit(true);
            return true;
        }
        if (infl == 7) {
            if (this.newProps.getFraction() == Player.FRACTION.MONOLITH) {
                return false;
            }
            this.accumulatedMonolith += delta;
            if (this.accumulatedMonolith < 20000) {
                return false;
            }
            this.accumulatedMonolith = 0L;
            this.newProps.subtractHealth(10.0d);
            if (this.newProps.getHealth() <= 0.0d) {
                this.newProps.setState(Player.STATE.W_MENTALLED);
            }
            this.newProps.setMonolithHit(true);
            return true;
        }
        if (infl != 8 || this.newProps.getFraction() == Player.FRACTION.MONOLITH) {
            return false;
        }
        this.accumulatedEmission += delta;
        if (this.accumulatedEmission < 2000) {
            return false;
        }
        this.accumulatedEmission = 0L;
        this.newProps.subtractHealth(30.0d);
        if (this.newProps.getHealth() <= 0.0d) {
            this.newProps.setState(Player.STATE.DEAD_EMISSION);
        }
        this.newProps.setEmissionHit(true);
        return true;
    }

    private Player.STATE getDeathState(int infl) {
        if (infl == 0) {
            return Player.STATE.DEAD_RADIATION;
        }
        if (infl == 1) {
            return Player.STATE.W_DEAD_ANOMALY;
        }
        if (infl == 2) {
            return Player.STATE.W_MENTALLED;
        }
        if (infl == 3) {
            return Player.STATE.W_DEAD_BURER;
        }
        if (infl == 4) {
            return Player.STATE.W_CONTROLLED;
        }
        if (infl == 8) {
            return Player.STATE.DEAD_EMISSION;
        }
        return Player.STATE.ALIVE;
    }

    public void calculateEnvDamage(long delta) {
        Log.e(TAG, "calculateEnvDamage " + Thread.currentThread().getName());
        if (this.newProps.getFraction() == Player.FRACTION.GAMEMASTER) {
            return;
        }
        int i = 0;
        while (true) {
            double[] dArr = this.mImpacts;
            if (i >= dArr.length) {
                break;
            }
            double strength = dArr[i];
            if (strength > 0.0d && i != 5 && i != 6 && ((this.newProps.getFraction() != Player.FRACTION.MONOLITH || (i != 2 && i != 7)) && (this.newProps.getFraction() != Player.FRACTION.DARKEN || i != 0))) {
                if (i == 0) {
                    this.newProps.setHealth(getHealthToSet(i, delta, strength));
                    if (this.newProps.getHealth() > 0.0d) {
                        this.newProps.setRadiation(getRadiationToSet(delta, strength));
                    } else {
                        this.newProps.setState(Player.STATE.W_DEAD_RADIATION);
                        break;
                    }
                } else if (i != 8 || !this.inflPack.isEmission() || strength < 8.0d || this.newProps.getFraction() == Player.FRACTION.MONOLITH || hit(i, delta)) {
                    if (strength < 16.0d || !hit(i, delta)) {
                        double h = getHealthToSet(i, delta, strength);
                        Log.e(TAG, "------------------------------------------ health: " + h + " infl: " + i + " strength: " + strength);
                        this.newProps.setHealth(h);
                        if (this.newProps.getHealth() <= 0.0d) {
                            this.newProps.setState(getDeathState(i));
                            break;
                        }
                    }
                } else {
                    PlayerPropsImpl playerPropsImpl = this.newProps;
                    double health = playerPropsImpl.getHealth();
                    double d = delta;
                    Double.isNaN(d);
                    playerPropsImpl.setHealth(health - ((d / 60000.0d) * 2.0d));
                    if (this.newProps.getHealth() <= 0.0d) {
                        this.newProps.setState(Player.STATE.DEAD_EMISSION);
                        break;
                    }
                }
            }
            i++;
        }
        Log.e(TAG, "" + this.newProps.getState());
    }

    public PlayerProps getPlayerProps() {
        this.newProps.setImpacts(this.mImpacts);
        return this.newProps;
    }

    private double getRadMod(double rad) {
        return Convert.map(rad, 1.0d, 16.0d, 1.0d, 7.0d);
    }

    @Override // net.afterday.compas.core.player.Impacts
    public void artifactsImpact(double[] artifacts) {
        this.healthModifier = 1.0d;
        this.radModifier = 1.0d;
        if (this.inflPack.influencedBy(5)) {
            this.healthModifier = artifacts[0];
            double[] dArr = this.mImpacts;
            dArr[5] = dArr[5] * artifacts[0];
            return;
        }
        Log.d(TAG, " -- " + artifacts[6]);
        this.radModifier = Math.max(artifacts[1], 1.0d) * Math.max(artifacts[5], 1.0d);
        this.healthModifier = artifacts[0];
        double[] dArr2 = this.mImpacts;
        dArr2[5] = dArr2[5] * artifacts[0];
        dArr2[0] = dArr2[0] + artifacts[5];
        dArr2[0] = dArr2[0] * artifacts[1];
        dArr2[1] = dArr2[1] * artifacts[2];
        dArr2[3] = dArr2[3] * artifacts[4];
        dArr2[4] = dArr2[4] * artifacts[6];
        dArr2[2] = dArr2[2] * artifacts[3];
        dArr2[7] = dArr2[7] * artifacts[9];
    }

    @Override // net.afterday.compas.core.player.Impacts
    public void itemImpact(Item item) {
        item.hasModifier(2);
    }

    private void consumeItem(Item item, long delta) {
        if (item.hasModifier(1)) {
            double[] dArr = this.mImpacts;
            dArr[0] = dArr[0] * item.getModifier(1);
            this.radModifier *= item.getModifier(1);
        }
        if (item.hasModifier(2)) {
            double[] dArr2 = this.mImpacts;
            dArr2[1] = dArr2[1] * item.getModifier(2);
        }
        if (item.hasModifier(3)) {
            double[] dArr3 = this.mImpacts;
            dArr3[2] = dArr3[2] * item.getModifier(3);
        }
        if (item.hasModifier(9)) {
            double[] dArr4 = this.mImpacts;
            dArr4[7] = dArr4[7] * item.getModifier(9);
        }
        if (item.hasModifier(6)) {
            double[] dArr5 = this.mImpacts;
            dArr5[4] = dArr5[4] * item.getModifier(6);
        }
        if (item.hasModifier(0)) {
            double[] dArr6 = this.mImpacts;
            dArr6[5] = dArr6[5] * item.getModifier(0);
            this.healthModifier *= item.getModifier(0);
        }
    }

    public void addHealthModifier(double healthModifier) {
        this.healthModifier *= healthModifier;
    }

    @Override // net.afterday.compas.core.player.Impacts
    public void boosterImpact(Item item) {
        consumeItem(item, this.delta);
        this.newProps.setBoosterPercents(item.getPercentsLeft());
    }

    public void deviceImpact(Item item) {
        consumeItem(item, this.delta);
        this.newProps.setDevicePercents(item.getPercentsLeft());
    }

    @Override // net.afterday.compas.core.player.Impacts
    public void armorImpact(Item item) {
        consumeItem(item, this.delta);
        this.newProps.setArmorPercents(item.getPercentsLeft());
    }

    @Override // net.afterday.compas.core.player.Impacts
    public Impacts.STATE getState() {
        Impacts.STATE state = this.state;
        if (state != null) {
            return state;
        }
        if (isHealing(this.newProps, this.inflPack)) {
            this.state = Impacts.STATE.HEALING;
            return this.state;
        }
        boolean clear = true;
        int i = 0;
        while (true) {
            if (i >= this.mImpacts.length) {
                break;
            }
            if (i != 6 && ((i != 8 && i != 2) || this.newProps.getFraction() != Player.FRACTION.MONOLITH)) {
                if (i == 8 && this.inflPack.isEmission() && this.mImpacts[i] >= 8.0d) {
                    clear = false;
                    break;
                }
                if (this.mImpacts[i] > 0.0d) {
                    clear = false;
                    break;
                }
            }
            i++;
        }
        if (clear) {
            this.state = Impacts.STATE.CLEAR;
            return this.state;
        }
        double[] dArr = this.mImpacts;
        if (dArr[0] > 0.0d || dArr[2] > 0.0d || dArr[7] > 0.0d || dArr[4] > 0.0d || dArr[1] > 0.0d || dArr[3] > 0.0d || dArr[8] > 8.0d) {
            this.state = Impacts.STATE.DAMAGE;
            return this.state;
        }
        return Impacts.STATE.CLEAR;
    }

    private boolean isHealing(PlayerProps playerProps, InfluencesPack inflPack) {
        Player.FRACTION fraction = playerProps.getFraction();
        return (fraction == Player.FRACTION.MONOLITH && inflPack.influencedBy(7)) || (fraction != Player.FRACTION.MONOLITH && inflPack.influencedBy(5)) || ((fraction == Player.FRACTION.DARKEN && inflPack.influencedBy(0)) || (fraction != Player.FRACTION.DARKEN && inflPack.influencedBy(5)));
    }

    private void printImpacts() {
        Log.e(TAG, "<<<<<<<<<<<<<<<<<<<<<<<<<<Impacts:");
        Log.e(TAG, "HEALTH: " + this.mImpacts[5]);
        Log.e(TAG, "RADIATION: " + this.mImpacts[0]);
        Log.e(TAG, "MENTAL: " + this.mImpacts[2]);
        Log.e(TAG, "CONTROLLER: " + this.mImpacts[4]);
        Log.e(TAG, "ANOMALY: " + this.mImpacts[1]);
        Log.e(TAG, "BURER: " + this.mImpacts[3]);
        Log.e(TAG, "ARTEFACT: " + this.mImpacts[6]);
        Log.e(TAG, "MONOLITH: " + this.mImpacts[7]);
    }
}

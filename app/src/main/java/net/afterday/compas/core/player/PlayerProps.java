package net.afterday.compas.core.player;

import net.afterday.compas.core.player.Player;
import net.afterday.compas.core.serialization.Jsonable;

/* JADX INFO: loaded from: classes.dex */
public interface PlayerProps extends Jsonable {
    void addHealth(double d);

    void addRadiation(double d);

    boolean addXpPoints(int i);

    boolean anomalyHit();

    boolean burerHit();

    boolean controllerHit();

    boolean emissionHit();

    double getAnomalyImpact();

    double getArmorPercents();

    double getArtefactImpact();

    double getBoosterPercents();

    double getBurerImpact();

    long getController();

    double getControllerImpact();

    double getDevicePercents();

    Player.FRACTION getFraction();

    double getHealth();

    double getHealthImpact();

    double[] getImpacts();

    int getLevel();

    int getLevelXp();

    double getMental();

    double getMentalImpact();

    double getMonolithImpact();

    double getRadiation();

    double getRadiationImpact();

    Player.STATE getState();

    int getXpPoints();

    long getZombified();

    boolean hasHealthInstant();

    boolean hasRadiationInstant();

    boolean mentalHit();

    boolean monolithHit();

    void setArmorPercents(double d);

    void setBoosterPercents(double d);

    void setDevicePercents(double d);

    boolean setFraction(Player.FRACTION fraction);

    void setRadiation(double d);

    void setState(Player.STATE state);

    void setXpPoints(int i);

    void subtractHealth(double d);

    void subtractRadiation(double d);
}

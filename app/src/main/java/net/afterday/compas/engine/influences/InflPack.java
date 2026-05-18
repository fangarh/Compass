package net.afterday.compas.engine.influences;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import net.afterday.compas.core.influences.Influence;
import net.afterday.compas.core.influences.InfluencesPack;

/* JADX INFO: loaded from: classes.dex */
public class InflPack implements InfluencesPack {
    private static final String TAG = "InflPack";
    private long averageInflUpdatingTime;
    private boolean mInfRad = false;
    private boolean mInflAno = false;
    private boolean mInflMen = false;
    private boolean mInflBur = false;
    private boolean mInflCon = false;
    private boolean mInflHel = false;
    private boolean mInflArt = false;
    private boolean mInflMon = false;
    private boolean mInflEm = false;
    private boolean isEmission = false;
    private double[] mInfluences = new double[9];
    private List<Influence> influences = new ArrayList();
    private long time = System.currentTimeMillis();

    public InflPack() {
        for (int i = 0; i < 9; i++) {
            this.mInfluences[i] = 0.0d;
        }
    }

    @Override // net.afterday.compas.core.influences.InfluencesPack
    public void addInfluence(int type, double strength) {
        Log.d(TAG, "addInfluence: " + getInfluenceName(type) + " " + strength);
        double[] dArr = this.mInfluences;
        dArr[type] = Math.max(dArr[type], strength);
        switch (type) {
            case 0:
                this.mInfRad = true;
                break;
            case 1:
                this.mInflAno = true;
                break;
            case 2:
                this.mInflMen = true;
                break;
            case 3:
                this.mInflBur = true;
                break;
            case 4:
                this.mInflCon = true;
                break;
            case 5:
                this.mInflHel = true;
                break;
            case 6:
                this.mInflArt = true;
                break;
            case 7:
                this.mInflMon = true;
                break;
            case 8:
                this.mInflEm = true;
                break;
        }
    }

    @Override // net.afterday.compas.core.influences.InfluencesPack
    public void setEmission(boolean emission) {
        this.isEmission = emission;
    }

    @Override // net.afterday.compas.core.influences.InfluencesPack
    public boolean isEmission() {
        return this.isEmission;
    }

    public void setAvgInfluenceEmittingTime(long avgTime) {
        this.averageInflUpdatingTime = avgTime;
    }

    @Override // net.afterday.compas.core.influences.InfluencesPack
    public boolean inDanger() {
        return this.mInfRad || this.mInflBur || this.mInflMen || this.mInflCon || this.mInflAno || this.mInflMon;
    }

    @Override // net.afterday.compas.core.influences.InfluencesPack
    public boolean isClear() {
        return (this.mInfRad || this.mInflBur || this.mInflMen || this.mInflCon || this.mInflAno || this.mInflHel || this.mInflMon) ? false : true;
    }

    @Override // net.afterday.compas.core.influences.InfluencesPack
    public boolean influencedBy(int influenceType) {
        switch (influenceType) {
            case 0:
                return this.mInfRad;
            case 1:
                return this.mInflAno;
            case 2:
                return this.mInflMen;
            case 3:
                return this.mInflBur;
            case 4:
                return this.mInflCon;
            case 5:
                return this.mInflHel;
            case 6:
                return this.mInflArt;
            case 7:
                return this.mInflMon;
            case 8:
                return this.mInflEm;
            default:
                return false;
        }
    }

    @Override // net.afterday.compas.core.influences.InfluencesPack
    public long creationTime() {
        return this.time;
    }

    @Override // net.afterday.compas.core.influences.InfluencesPack
    public double[] getInfluences() {
        return (double[]) this.mInfluences.clone();
    }

    @Override // net.afterday.compas.core.influences.InfluencesPack
    public double getInfluence(int influenceType) {
        return this.mInfluences[influenceType];
    }

    @Override // net.afterday.compas.core.influences.InfluencesPack
    public int getSource() {
        return 0;
    }

    private String getInfluenceName(int type) {
        switch (type) {
            case 0:
                return "Radiation";
            case 1:
                return "Anomaly";
            case 2:
                return "Mental";
            case 3:
                return "Burer";
            case 4:
                return "Controller";
            case 5:
                return "Health";
            case 6:
                return "Artefact";
            case 7:
                return "Monolith";
            default:
                return "Unknown";
        }
    }

    public String toString() {
        String newL = System.getProperty("line.separator");
        String str = "Influences pack:" + newL;
        String str2 = str + "Time: " + creationTime() + newL;
        int size = 0;
        String infls = "";
        for (int i = 0; i < 9; i++) {
            if (influencedBy(i)) {
                size++;
                infls = infls + getInfluenceName(i) + ": " + getInfluence(i) + "\n";
            }
        }
        return (str2 + "Influences: (" + size + ")\n") + infls;
    }
}

package net.afterday.compas.settings;

/* JADX INFO: loaded from: classes.dex */
public class Defaults {
    public static boolean getDefaultBool(String key) {
        return Constants.VIBRATION.equals(key) || Constants.COMPASS.equals(key);
    }

    public static int getDefaultInt(String key) {
        return ((key.hashCode() == 713460144 && key.equals(Constants.ORIENTATION)) ? (byte) 0 : (byte) -1) != 0 ? -1 : 0;
    }
}

package net.afterday.compas.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class Settings {
    private static Settings instance;
    private Context context;
    private SharedPreferences.OnSharedPreferenceChangeListener innerListener;
    private List<WeakReference<SettingsListener>> listeners = new ArrayList();
    private SharedPreferences prefs;
    private Resources res;

    private Settings(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(Constants.SETTINGS, 0);
        this.res = context.getResources();
    }

    public static Settings instance() {
        Settings settings = instance;
        if (settings == null) {
            throw new IllegalStateException("Settings must be initialized.");
        }
        return settings;
    }

    public static Settings instance(Context context) {
        if (instance == null) {
            instance = new Settings(context);
        }
        return instance;
    }

    public boolean getBoolSetting(String key) {
        return this.prefs.getBoolean(key, Defaults.getDefaultBool(key));
    }

    public void setBoolSetting(String key, boolean val) {
        this.prefs.edit().putBoolean(key, val).apply();
        notifySettingChanged(key, String.valueOf(val));
    }

    public int getIntSetting(String key) {
        return this.prefs.getInt(key, Defaults.getDefaultInt(key));
    }

    public void setIntSetting(String key, int val) {
        this.prefs.edit().putInt(key, val).apply();
        notifySettingChanged(key, Integer.toString(val));
    }

    private void notifySettingChanged(String key, String val) {
        Log.e("SETTINGS", "notifySettingChanged: " + Thread.currentThread().getName());
        Iterator<WeakReference<SettingsListener>> iter = this.listeners.iterator();
        while (iter.hasNext()) {
            SettingsListener l = iter.next().get();
            if (l == null) {
                iter.remove();
            } else {
                l.onSettingChanged(key, val);
            }
        }
    }

    public void addSettingsListener(SettingsListener listener) {
        this.listeners.add(new WeakReference<>(listener));
    }
}

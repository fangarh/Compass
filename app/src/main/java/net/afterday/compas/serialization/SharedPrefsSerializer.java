package net.afterday.compas.serialization;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.afterday.compas.core.serialization.Jsonable;
import net.afterday.compas.core.serialization.Serializer;

/* JADX INFO: loaded from: classes.dex */
public class SharedPrefsSerializer implements Serializer {
    private static SharedPrefsSerializer instance;
    private Context context;
    private Map<String, SharedPreferences> prefs = new HashMap();

    private SharedPrefsSerializer(Context context) {
        this.context = context;
    }

    public static SharedPrefsSerializer instance(Context context) {
        instance = new SharedPrefsSerializer(context);
        return instance;
    }

    public static SharedPrefsSerializer instance() {
        SharedPrefsSerializer sharedPrefsSerializer = instance;
        if (sharedPrefsSerializer == null) {
            throw new IllegalStateException("SharedPrefsSerializer not initialized");
        }
        return sharedPrefsSerializer;
    }

    @Override // net.afterday.compas.core.serialization.Serializer
    public void serialize(String key, Jsonable object) {
        getSp(key).edit().putString(key, object.toJson().toString()).apply();
    }

    @Override // net.afterday.compas.core.serialization.Serializer
    public void serialize(String key, String id, Jsonable object) {
        getSp(key).edit().putString(id, object.toJson().toString()).apply();
    }

    @Override // net.afterday.compas.core.serialization.Serializer
    public void remove(String key, String id) {
        getSp(key).edit().remove(id).apply();
    }

    @Override // net.afterday.compas.core.serialization.Serializer
    public Jsonable deserialize(String key) {
        SharedPreferences sp = getSp(key);
        if (sp.contains(key)) {
            return new JsonableImpl(sp.getString(key, null));
        }
        return null;
    }

    @Override // net.afterday.compas.core.serialization.Serializer
    public List<Jsonable> deserializeList(String key) {
        Map<String, ?> all = getSp(key).getAll();
        List<Jsonable> jsonables = new ArrayList<>();
        Iterator<? extends Map.Entry<String, ?>> it = all.entrySet().iterator();
        while (it.hasNext()) {
            jsonables.add(new JsonableImpl((String) it.next().getValue()));
        }
        return jsonables;
    }

    private SharedPreferences getSp(String key) {
        if (this.prefs.containsKey(key)) {
            return this.prefs.get(key);
        }
        SharedPreferences sp = this.context.getSharedPreferences(key, 0);
        this.prefs.put(key, sp);
        return sp;
    }

    private static class JsonableImpl implements Jsonable {
        private String jsonString;
        private JsonObject o;

        JsonableImpl(String string) {
            this.jsonString = string;
        }

        @Override // net.afterday.compas.core.serialization.Jsonable
        public JsonObject toJson() {
            JsonObject jsonObject = this.o;
            if (jsonObject != null) {
                return jsonObject;
            }
            if (this.jsonString == null) {
                return null;
            }
            this.o = new JsonParser().parse(this.jsonString).getAsJsonObject();
            return this.o;
        }
    }
}

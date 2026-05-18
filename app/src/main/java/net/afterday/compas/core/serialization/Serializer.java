package net.afterday.compas.core.serialization;

import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public interface Serializer {
    Jsonable deserialize(String str);

    List<Jsonable> deserializeList(String str);

    void remove(String str, String str2);

    void serialize(String str, String str2, Jsonable jsonable);

    void serialize(String str, Jsonable jsonable);
}

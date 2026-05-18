package net.afterday.compas.db;

import android.content.Context;
import net.afterday.compas.db.log.LogDb;

/* JADX INFO: loaded from: classes.dex */
public class DataBase {
    private static DataBase instance;
    private Context ctx;
    private SQLiteHelper dbHelper;

    private DataBase(Context ctx) {
        this.ctx = ctx;
        this.dbHelper = new SQLiteHelper(ctx);
    }

    public static DataBase instance() {
        DataBase dataBase = instance;
        if (dataBase == null) {
            throw new IllegalStateException("Database not initialized");
        }
        return dataBase;
    }

    public static DataBase instance(Context ctx) {
        instance = new DataBase(ctx);
        return instance;
    }

    public LogDb logDb() {
        return new LogDb(this.dbHelper);
    }
}

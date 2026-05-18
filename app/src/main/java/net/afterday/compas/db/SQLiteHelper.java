package net.afterday.compas.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/* JADX INFO: loaded from: classes.dex */
public class SQLiteHelper extends SQLiteOpenHelper {
    public static final String COLUMN_DATE = "created";
    public static final String COLUMN_DATE_DISPLAY = "date_display";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TEXT = "content";
    public static final String COLUMN_TYPE = "type";
    private static final String CREATE_TABLE_LOG = "create table messages( id integer primary key autoincrement, created datetime default CURRENT_TIMESTAMP, date_display text not null, content text not null, type integer default 0);";
    private static final String DATABASE_NAME = "database.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_INVENTORY = "inventory";
    public static final String TABLE_LOG = "messages";

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, 1);
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_LOG);
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS messages");
        onCreate(db);
    }
}

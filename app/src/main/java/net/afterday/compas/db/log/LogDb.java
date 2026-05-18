package net.afterday.compas.db.log;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;
import net.afterday.compas.db.SQLiteHelper;
import net.afterday.compas.logging.LogLine;

/* JADX INFO: loaded from: classes.dex */
public class LogDb {
    private String[] allColumns = {SQLiteHelper.COLUMN_ID, SQLiteHelper.COLUMN_DATE_DISPLAY, SQLiteHelper.COLUMN_TEXT, "type"};
    private SQLiteHelper dbHelper;

    public LogDb(SQLiteHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public List<LogLine> getLogLines() {
        SQLiteDatabase database = this.dbHelper.getReadableDatabase();
        List<LogLine> list = new ArrayList<>();
        Cursor cursor = database.query(SQLiteHelper.TABLE_LOG, this.allColumns, null, null, null, null, "created ASC", "100");
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(cursorToResult(cursor));
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    public void putLogLine(LogLine line) {
        SQLiteDatabase database = this.dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.COLUMN_TEXT, line.getText());
        values.put("type", Integer.valueOf(line.getColor()));
        values.put(SQLiteHelper.COLUMN_DATE_DISPLAY, line.getDate());
        database.insert(SQLiteHelper.TABLE_LOG, null, values);
    }

    private LogLine cursorToResult(Cursor cursor) {
        LogLine logLine = new LogLine();
        logLine.setId(cursor.getLong(0)).setDate(cursor.getString(1)).setText(cursor.getString(2)).setColor(cursor.getInt(3));
        return logLine;
    }
}

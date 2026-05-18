package net.afterday.compas.logging;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import net.afterday.compas.R;
import net.afterday.compas.core.inventory.items.Events.ItemAdded;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.db.DataBase;
import net.afterday.compas.db.log.LogDb;
import net.afterday.compas.devices.vibro.Vibro;
import net.afterday.compas.persistency.items.ItemDescriptor;
import net.afterday.compas.view.SmallLogListAdapter;

/* JADX INFO: loaded from: classes.dex */
public class Logger {
    private static Context ctx;
    private static Logger instance;
    private static LogDb logDb;
    private static SmallLogListAdapter logListAdapter;
    private static Vibro vibro;
    private static Subject<LogLine> logLinesSubj = BehaviorSubject.create();
    private static Subject<List<LogLine>> logLines = BehaviorSubject.create();

    private Logger(Context ctx2, Vibro vibro2) {
        ctx = ctx2;
        logDb = DataBase.instance().logDb();
        vibro = vibro2;
        logLinesSubj.scan(logDb.getLogLines(), $$Lambda$Logger$GmZROCTPpbm7kMwYzRoDXQPmbLk.INSTANCE).observeOn(AndroidSchedulers.mainThread()).subscribe($$Lambda$Logger$iKzIrYJMkgFS30rjDLCBGFUGH1k.INSTANCE);
    }

    static /* synthetic */ List lambda$new$0(List lst, LogLine line) {
        lst.add(line);
        if (lst.size() > 50) {
            return new ArrayList(lst.subList(2, lst.size()));
        }
        return lst;
    }

    static /* synthetic */ void lambda$new$1(List list) {
        logLines.onNext(list);
    }

    public static Logger instance(Context ctx2, Vibro vibro2) {
        instance = new Logger(ctx2, vibro2);
        return instance;
    }

    public static Logger instance() {
        Logger logger = instance;
        if (logger != null) {
            return logger;
        }
        throw new IllegalStateException("Logger not initialized");
    }

    public static void i(String message) {
        String time = getTime();
        log(new LogLine(formMsg(time, message), time, ContextCompat.getColor(ctx, R.color.ok)), true);
    }

    public static void d(String message) {
        String time = getTime();
        Log.d("LOGGER:", message);
        log(new LogLine(formMsg(time, message), time, ContextCompat.getColor(ctx, R.color.norm)), true);
    }

    public static void d(int id) {
        String time = getTime();
        log(new LogLine(formMsg(time, ctx.getResources().getString(id)), time, ContextCompat.getColor(ctx, R.color.norm)), true);
    }

    public static void e(String message) {
        String time = getTime();
        log(new LogLine(formMsg(time, message), time, ContextCompat.getColor(ctx, R.color.bad)), true);
    }

    public static void e(int id) {
        String time = getTime();
        log(new LogLine(formMsg(time, ctx.getResources().getString(id)), time, ContextCompat.getColor(ctx, R.color.bad)), true);
    }

    public Observable<List<LogLine>> getLogStream() {
        return logLines;
    }

    public static void logItemAdded(ItemAdded itemAdded) {
        i(String.format(ctx.getString(R.string.message_received_item), getItemName(itemAdded.getItem().getItemDescriptor())));
    }

    public static void logItemUsed(Item item) {
        i(String.format(ctx.getString(R.string.message_used_item), getItemName(item.getItemDescriptor())));
    }

    public static void logItemDropped(Item item) {
        i(String.format(ctx.getString(R.string.message_dropped_item), getItemName(item.getItemDescriptor())));
    }

    private static void log(LogLine logLine, boolean addToDb) {
        logLinesSubj.onNext(logLine);
        if (addToDb) {
            logDb.putLogLine(logLine);
        }
    }

    private static String getTime() {
        Object objValueOf;
        Object objValueOf2;
        Calendar c = Calendar.getInstance();
        int hour = c.get(11);
        int min = c.get(12);
        StringBuilder sb = new StringBuilder();
        if (hour < 10) {
            objValueOf = "0" + hour;
        } else {
            objValueOf = Integer.valueOf(hour);
        }
        sb.append(objValueOf);
        sb.append(":");
        if (min < 10) {
            objValueOf2 = "0" + min;
        } else {
            objValueOf2 = Integer.valueOf(min);
        }
        sb.append(objValueOf2);
        return sb.toString();
    }

    private static String formMsg(String time, String msg) {
        return msg;
    }

    private static String formTime(String time, String msg) {
        return time;
    }

    private static String getItemName(ItemDescriptor itemD) {
        String itemName = ctx.getString(itemD.getNameId());
        String itemName2 = itemName != null ? itemName : itemD.getName();
        return itemName2 != null ? itemName2 : "UNKNOWN ITEM";
    }
}

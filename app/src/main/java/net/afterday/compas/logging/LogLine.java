package net.afterday.compas.logging;

/* JADX INFO: loaded from: classes.dex */
public class LogLine {
    private int color;
    private String date;
    private long id;
    private int resId;
    private String text;

    public LogLine(String text, String date, int color) {
        this(text, date, color, -1);
    }

    public LogLine(String text, String date, int color, int resId) {
        this.resId = -1;
        this.text = text;
        this.date = date;
        this.color = color;
        this.resId = resId;
    }

    public LogLine() {
        this.resId = -1;
    }

    public String getText() {
        return this.text;
    }

    public String getDate() {
        return this.date;
    }

    public int getColor() {
        return this.color;
    }

    public int getResId() {
        return this.resId;
    }

    public LogLine setId(long id) {
        this.id = id;
        return this;
    }

    public LogLine setDate(String date) {
        this.date = date;
        return this;
    }

    public LogLine setText(String text) {
        this.text = text;
        return this;
    }

    public LogLine setColor(int color) {
        this.color = color;
        return this;
    }

    public boolean hasResId() {
        return this.resId > -1;
    }
}

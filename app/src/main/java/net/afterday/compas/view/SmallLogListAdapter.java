package net.afterday.compas.view;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import net.afterday.compas.R;
import net.afterday.compas.logging.LogLine;

/* JADX INFO: loaded from: classes.dex */
public class SmallLogListAdapter extends RecyclerView.Adapter {
    private Context context;
    private List<LogLine> mDataset;
    private TimeZone mTimezone;
    private Typeface mTypeface;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mText;
        public TextView mTime;

        public ViewHolder(View container) {
            super(container);
            this.mTime = (TextView) container.findViewById(R.id.time);
            this.mText = (TextView) container.findViewById(R.id.text);
        }
    }

    public SmallLogListAdapter(Context ctx, ArrayList<LogLine> dataset) {
        this.mDataset = dataset;
        this.context = ctx;
        try {
            this.mTypeface = Typeface.createFromAsset(ctx.getAssets(), "fonts/console.ttf");
        } catch (RuntimeException e) {
        }
        this.mTimezone = TimeZone.getTimeZone("GMT+02:00");
    }

    public void setDataset(List<LogLine> dataset) {
        this.mDataset = dataset;
        notifyDataSetChanged();
    }

    @Override // android.support.v7.widget.RecyclerView.Adapter
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.small_log_list_item, parent, false);
        ((TextView) v.findViewById(R.id.time)).setTypeface(this.mTypeface);
        ((TextView) v.findViewById(R.id.text)).setTypeface(this.mTypeface);
        return new ViewHolder(v);
    }

    @Override // android.support.v7.widget.RecyclerView.Adapter
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ViewHolder vh = (ViewHolder) holder;
        LogLine line = this.mDataset.get(position);
        vh.mTime.setText(line.getDate());
        vh.mText.setText(line.getText());
        int color = line.getColor();
        vh.mTime.setTextColor(color);
        vh.mText.setTextColor(color);
    }

    @Override // android.support.v7.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.mDataset.size();
    }
}

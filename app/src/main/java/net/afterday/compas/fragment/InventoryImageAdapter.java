package net.afterday.compas.fragment;

import android.content.Context;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import java.util.List;
import net.afterday.compas.core.inventory.items.Item;

/* JADX INFO: loaded from: classes.dex */
public class InventoryImageAdapter extends BaseAdapter {
    public static final int CATEGORIES = 99;
    private Context mContext;
    private List<Item> mInventory;

    public InventoryImageAdapter(Context c, List<Item> inventory) {
        this.mContext = c;
        this.mInventory = inventory;
    }

    @Override // android.widget.Adapter
    public int getCount() {
        return this.mInventory.size();
    }

    @Override // android.widget.Adapter
    public Object getItem(int position) {
        return this.mInventory.get(position);
    }

    @Override // android.widget.Adapter
    public long getItemId(int position) {
        return 0L;
    }

    @Override // android.widget.Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(this.mContext);
            imageView.setLayoutParams(new AbsListView.LayoutParams(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }
        imageView.setImageResource(this.mInventory.get(position).getItemDescriptor().getImage());
        return imageView;
    }
}

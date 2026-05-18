package net.afterday.compas.fragment;

import android.content.Context;
import android.support.v4.util.Pair;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.afterday.compas.R;
import net.afterday.compas.core.inventory.items.Item;

/* JADX INFO: loaded from: classes.dex */
public class CategoriesAdapter extends BaseAdapter {
    private static final List<Pair<Item.CATEGORY, Integer>> categories = new ArrayList();
    private static final Map<Item.CATEGORY, Integer> categoryImages = new HashMap();
    private final List<Pair<Item.CATEGORY, Pair<List<Item>, Integer>>> categoryItems = new ArrayList();
    private final Context context;

    static {
        categories.add(new Pair<>(Item.CATEGORY.MEDKITS, Integer.valueOf(R.drawable.cat_medkits)));
        categories.add(new Pair<>(Item.CATEGORY.ANTIRADS, Integer.valueOf(R.drawable.cat_antirads)));
        categories.add(new Pair<>(Item.CATEGORY.BOOSTERS, Integer.valueOf(R.drawable.cat_boosters)));
        categories.add(new Pair<>(Item.CATEGORY.ARTIFACTS, Integer.valueOf(R.drawable.cat_artefacts)));
        categories.add(new Pair<>(Item.CATEGORY.WEAPONS, Integer.valueOf(R.drawable.cat_weapons)));
        categories.add(new Pair<>(Item.CATEGORY.UPGRADES, Integer.valueOf(R.drawable.cat_upgrades)));
        categories.add(new Pair<>(Item.CATEGORY.ARMORS, Integer.valueOf(R.drawable.cat_suits)));
        categories.add(new Pair<>(Item.CATEGORY.HABAR, Integer.valueOf(R.drawable.cat_habar)));
        categories.add(new Pair<>(Item.CATEGORY.FOOD, Integer.valueOf(R.drawable.cat_food)));
        categories.add(new Pair<>(Item.CATEGORY.DEVICES, Integer.valueOf(R.drawable.cat_devices)));
    }

    public CategoriesAdapter(Context c, List<Item> inventory) {
        this.context = c;
        Map<Item.CATEGORY, List<Item>> itemsOfCategory = new HashMap<>();
        for (Item i : inventory) {
            Item.CATEGORY cat = i.getItemDescriptor().getCategory();
            if (itemsOfCategory.containsKey(cat)) {
                itemsOfCategory.get(i.getItemDescriptor().getCategory()).add(i);
            } else {
                List<Item> items = new ArrayList<>();
                items.add(i);
                itemsOfCategory.put(cat, items);
            }
        }
        for (Pair<Item.CATEGORY, Integer> catImg : categories) {
            if (itemsOfCategory.containsKey(catImg.first)) {
                this.categoryItems.add(new Pair<>(catImg.first, new Pair(itemsOfCategory.get(catImg.first), catImg.second)));
            } else {
                this.categoryItems.add(new Pair<>(catImg.first, new Pair(new ArrayList(), catImg.second)));
            }
        }
    }

    @Override // android.widget.Adapter
    public int getCount() {
        return categories.size();
    }

    @Override // android.widget.Adapter
    public Object getItem(int i) {
        return this.categoryItems.get(i);
    }

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    @Override // android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        ImageView imageView;
        if (view == null) {
            imageView = new ImageView(this.context);
            imageView.setLayoutParams(new AbsListView.LayoutParams(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) view;
        }
        Pair<List<Item>, Integer> img = this.categoryItems.get(i).second;
        imageView.setImageResource(img.second.intValue());
        if (img.first.size() == 0) {
            imageView.setAlpha(50);
        }
        return imageView;
    }

    public List<Item> getItemsByCategory(int position) {
        return this.categoryItems.get(position).second.first;
    }
}

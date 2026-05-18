package net.afterday.compas.fragment;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.afterday.compas.LocalMainService;
import net.afterday.compas.R;
import net.afterday.compas.core.inventory.Inventory;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.core.player.Player;
import net.afterday.compas.engine.events.ItemEventsBus;
import net.afterday.compas.engine.events.PlayerEventBus;

/* JADX INFO: loaded from: classes.dex */
public class InventoryFragment extends DialogFragment {
    public static final int ALL = 0;
    public static final int ARMORS = 2;
    public static final int BOOSTERS = 1;
    public static final int DEVICES = 3;
    public static final String TAG_CATEGORY = "category";
    public static final String TAG_INVENTORY = "inventory";
    public static final String TYPE = "type";
    List<Item.CATEGORY> categories;
    private CategoriesAdapter categoriesAdapter;
    private InventoryImageAdapter itemsAdapter;
    Map<Item.CATEGORY, List<Item>> itemsByCategory;
    List<Item> mInventory;
    GridView mInventoryGrid;
    private int type;
    private View v;
    private boolean SHOW_CATEGORIES_ON_BACK = false;
    private CompositeDisposable subscriptions = new CompositeDisposable();

    static /* synthetic */ boolean access$000(InventoryFragment x0) {
        return x0.SHOW_CATEGORIES_ON_BACK;
    }

    static /* synthetic */ void access$100(InventoryFragment x0) {
        x0.showCategories();
    }

    @Override // android.app.DialogFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getArguments();
        if (b != null) {
            this.type = b.getInt("type", 99);
        }
        setStyle(2, R.style.DialogStyle);
        this.subscriptions.add(Observable.merge(ItemEventsBus.instance().getItemUsedEvents(), ItemEventsBus.instance().getItemDroppedEvents()).observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$InventoryFragment$rVng5uWEvbEI9a7L4N5mNZBg3Lc(this)));
        this.subscriptions.add(PlayerEventBus.instance().getPlayerStateStream().observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$InventoryFragment$JOkctnXL9jyG3eS23ByAuKzB0Y(this)));
    }

    public /* synthetic */ void lambda$onCreate$0$InventoryFragment(Item i) {
        clearItem(i);
        showItemsOfCategory(i.getItemDescriptor().getCategory().getId());
    }

    public /* synthetic */ void lambda$onCreate$1$InventoryFragment(Player.STATE ps) {
        if (ps.getCode() != 1) {
            close();
        }
    }

    @Override // android.app.Fragment
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.popup_inventory, container, false);
        v.findViewById(R.id.openPrefs).setOnClickListener(new $$Lambda$InventoryFragment$g26bwzYpsIwvPVigBXnlHicGM(this));
        v.findViewById(R.id.close).setOnClickListener(new AnonymousClass1());
        this.mInventoryGrid = (GridView) v.findViewById(R.id.inventory_grid);
        loadInventory();
        return v;
    }

    /* JADX INFO: renamed from: net.afterday.compas.fragment.InventoryFragment$1, reason: invalid class name */
    class AnonymousClass1 implements View.OnClickListener {
        AnonymousClass1() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View v) {
            if (InventoryFragment.access$000(InventoryFragment.this)) {
                InventoryFragment.access$100(InventoryFragment.this);
            } else {
                InventoryFragment.this.close();
            }
        }
    }

    public /* synthetic */ void lambda$onCreateView$2$InventoryFragment(View c) {
        openPrefs(c);
    }

    public void openPrefs(View view) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("settings");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        SettingsFragment settingsFragment = new SettingsFragment();
        settingsFragment.show(ft, "settings");
    }

    public void close() {
        dismiss();
    }

    private void loadInventory() {
        ItemEventsBus itemEventsBus = LocalMainService.getInstance().getItemEventBus();
        itemEventsBus.getUserItems().observeOn(AndroidSchedulers.mainThread()).take(1L).subscribe(new $$Lambda$InventoryFragment$6JreOIGFDM00xtOhbIhwdb4Dmlc(this));
        itemEventsBus.requestItems();
    }

    public /* synthetic */ void lambda$loadInventory$3$InventoryFragment(Inventory ui) {
        this.mInventory = ui.getItems();
        int i = this.type;
        if (i == 99) {
            showCategories();
        } else {
            showItemsOfCategory(i);
        }
    }

    private void openItemInfo(Item item) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("item");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        ItemInfoFragment newFragment = ItemInfoFragment.newInstance(item);
        newFragment.show(ft, "item");
        newFragment.setCallback(new AnonymousClass2());
    }

    /* JADX INFO: renamed from: net.afterday.compas.fragment.InventoryFragment$2, reason: invalid class name */
    class AnonymousClass2 implements ItemInfoCallback {
        AnonymousClass2() {
        }

        @Override // net.afterday.compas.fragment.ItemInfoCallback
        public void onItemInfoClosed(Item item) {
        }

        @Override // net.afterday.compas.fragment.ItemInfoCallback
        public void onItemUsed(Item item) {
        }

        @Override // net.afterday.compas.fragment.ItemInfoCallback
        public void onItemDropped(Item item) {
        }
    }

    @Override // android.app.DialogFragment, android.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        CompositeDisposable compositeDisposable = this.subscriptions;
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            this.subscriptions.dispose();
        }
    }

    private void clearItem(Item item) {
        if (this.mInventory.contains(item)) {
            this.mInventory.remove(item);
        }
    }

    private void showCategories() {
        this.SHOW_CATEGORIES_ON_BACK = false;
        this.categoriesAdapter = new CategoriesAdapter(getActivity(), this.mInventory);
        this.mInventoryGrid.setAdapter((ListAdapter) this.categoriesAdapter);
        this.mInventoryGrid.setOnItemClickListener(new $$Lambda$InventoryFragment$GTtsNrgXX7qoN3A9aeNor7dr9Q(this));
    }

    public /* synthetic */ void lambda$showCategories$4$InventoryFragment(AdapterView parent, View view, int position, long id) {
        List<Item> items = this.categoriesAdapter.getItemsByCategory(position);
        if (items.size() > 0) {
            openCategoryItems(items);
        }
    }

    private void showItemsOfCategory(int category) {
        List<Item> itemsOfCategory = new ArrayList<>();
        for (Item i : this.mInventory) {
            if (i.getItemDescriptor().getCategory().getId() == category) {
                itemsOfCategory.add(i);
            }
        }
        openCategoryItems(itemsOfCategory);
    }

    private void openCategoryItems(List<Item> items) {
        this.SHOW_CATEGORIES_ON_BACK = true;
        this.itemsAdapter = new InventoryImageAdapter(getActivity(), items);
        this.mInventoryGrid.setAdapter((ListAdapter) this.itemsAdapter);
        this.mInventoryGrid.setOnItemClickListener(new $$Lambda$InventoryFragment$_fNe5jLbo7gA40PTXGjHtoWDmIA(this));
    }

    public /* synthetic */ void lambda$openCategoryItems$5$InventoryFragment(AdapterView parent, View view, int position, long id) {
        openItemInfo((Item) this.itemsAdapter.getItem(position));
    }
}

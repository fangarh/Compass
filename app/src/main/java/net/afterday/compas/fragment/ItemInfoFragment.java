package net.afterday.compas.fragment;

import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import java.util.Locale;
import net.afterday.compas.R;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.core.player.Player;
import net.afterday.compas.engine.events.EmissionEventBus;
import net.afterday.compas.engine.events.ItemEventsBus;
import net.afterday.compas.engine.events.PlayerEventBus;
import net.afterday.compas.util.Fonts;

/* JADX INFO: loaded from: classes.dex */
public class ItemInfoFragment extends DialogFragment {
    private static Observable<Item> itemViews;
    private static Item mItem;
    private ItemInfoCallback mCallback;
    private LinearLayout mEffectHolder;
    private Typeface mTypeface;
    private CompositeDisposable subscriptions = new CompositeDisposable();
    private View v;

    static /* synthetic */ void access$000(ItemInfoFragment x0, Item x1) {
        x0.dropItem(x1);
    }

    static /* synthetic */ void access$100(ItemInfoFragment x0) {
        x0.useItem();
    }

    public static ItemInfoFragment newInstance(Item item) {
        ItemInfoFragment f = new ItemInfoFragment();
        mItem = item;
        return f;
    }

    @Override // android.app.DialogFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(2, R.style.DialogStyle);
        this.subscriptions.add(PlayerEventBus.instance().getPlayerStateStream().observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$ItemInfoFragment$BM3qN68xYD3nVUVybSh9klJmGs(this)));
    }

    public /* synthetic */ void lambda$onCreate$0$ItemInfoFragment(Player.STATE ps) {
        if (ps.getCode() != 1) {
            close();
        }
    }

    @Override // android.app.Fragment
    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        this.v = inflater.inflate(R.layout.popup_item_info, container);
        setupItem(mItem);
        this.v.findViewById(R.id.close).setOnClickListener(new AnonymousClass1());
        return this.v;
    }

    /* JADX INFO: renamed from: net.afterday.compas.fragment.ItemInfoFragment$1, reason: invalid class name */
    class AnonymousClass1 implements View.OnClickListener {
        AnonymousClass1() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View v) {
            ItemInfoFragment.this.closePopup(v);
        }
    }

    @Override // android.app.DialogFragment, android.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        if (!this.subscriptions.isDisposed()) {
            this.subscriptions.dispose();
        }
    }

    private void setupItem(Item mItem2) {
        TextView itemName = (TextView) this.v.findViewById(R.id.name);
        if (mItem2.getItemDescriptor().getNameId() > 0) {
            itemName.setText(mItem2.getItemDescriptor().getNameId());
        } else {
            itemName.setText(mItem2.getItemDescriptor().getName());
        }
        itemName.setTextSize(30.0f);
        TextView description = (TextView) this.v.findViewById(R.id.description);
        description.setTextSize(21.0f);
        if (mItem2.getItemDescriptor().getDescriptionId() > 0) {
            description.setText(mItem2.getItemDescriptor().getDescriptionId());
        } else {
            description.setText(mItem2.getItemDescriptor().getDescription());
        }
        Button dropButton = (Button) this.v.findViewById(R.id.drop);
        Button useButton = (Button) this.v.findViewById(R.id.use);
        try {
            this.mTypeface = Fonts.instance().getDefaultTypeFace();
            itemName.setTypeface(this.mTypeface);
            dropButton.setTypeface(this.mTypeface);
            dropButton.setTextSize(25.0f);
            useButton.setTypeface(this.mTypeface);
            useButton.setTextSize(25.0f);
            description.setTypeface(this.mTypeface);
        } catch (RuntimeException e) {
        }
        if (!mItem2.getItemDescriptor().isConsumable() || mItem2.getItemDescriptor().isArtefact() || mItem2.isActive()) {
            useButton.setVisibility(8);
        }
        if (!mItem2.getItemDescriptor().isDropable() || mItem2.isActive()) {
            dropButton.setVisibility(8);
        }
        this.mEffectHolder = (LinearLayout) this.v.findViewById(R.id.effect_holder);
        ImageView itemImage = (ImageView) this.v.findViewById(R.id.item_image);
        itemImage.setImageResource(mItem2.getItemDescriptor().getImage());
        dropButton.setOnClickListener(new AnonymousClass2(mItem2));
        useButton.setOnClickListener(new AnonymousClass3());
        for (int i = 0; i < 10; i++) {
            if (mItem2.hasModifier(i)) {
                createEffectView(i, mItem2.getModifier(i));
            }
        }
        if (mItem2.getItemDescriptor().getName() == "Anabiotic") {
            this.subscriptions.add(EmissionEventBus.instance().getEmissionStateStream().observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$ItemInfoFragment$2gYzakf95LWlnmLEe0gch57j6I(useButton)));
        }
    }

    /* JADX INFO: renamed from: net.afterday.compas.fragment.ItemInfoFragment$2, reason: invalid class name */
    class AnonymousClass2 implements View.OnClickListener {
        final /* synthetic */ Item val$mItem;

        AnonymousClass2(Item item) {
            this.val$mItem = item;
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View v) {
            ItemInfoFragment.access$000(ItemInfoFragment.this, this.val$mItem);
        }
    }

    /* JADX INFO: renamed from: net.afterday.compas.fragment.ItemInfoFragment$3, reason: invalid class name */
    class AnonymousClass3 implements View.OnClickListener {
        AnonymousClass3() {
        }

        @Override // android.view.View.OnClickListener
        public void onClick(View v) {
            ItemInfoFragment.access$100(ItemInfoFragment.this);
        }
    }

    static /* synthetic */ void lambda$setupItem$1(Button useButton, Boolean active) {
        if (active.booleanValue()) {
            useButton.setVisibility(0);
        } else {
            useButton.setVisibility(8);
        }
    }

    public void closePopup(View view) {
        try {
            dismiss();
        } catch (Exception e) {
        }
    }

    public void close() {
        try {
            dismiss();
            if (this.mCallback != null) {
                this.mCallback.onItemInfoClosed(mItem);
            }
        } catch (Exception e) {
        }
    }

    public void setCallback(ItemInfoCallback callback) {
        this.mCallback = callback;
    }

    private void dropItem(Item item) {
        ItemEventsBus.instance().dropItem(item);
        close();
    }

    private void useItem() {
        ItemInfoCallback itemInfoCallback = this.mCallback;
        if (itemInfoCallback != null) {
            itemInfoCallback.onItemUsed(mItem);
        }
        ItemEventsBus.instance().useItem(mItem);
        close();
    }

    private void receiveItem() {
        Intent intent = new Intent("StalkerGiveItem");
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        close();
    }

    private LinearLayout createEffectView(int type, double amount) {
        LinearLayout l = new LinearLayout(getActivity());
        l.setOrientation(1);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -1);
        int px = (int) TypedValue.applyDimension(1, 2.0f, getResources().getDisplayMetrics());
        params.setMargins(px, px, px, px);
        l.setLayoutParams(params);
        ImageView effectImage = new ImageView(getActivity());
        effectImage.setLayoutParams(new ViewGroup.LayoutParams(-2, -2));
        effectImage.setImageResource(getEffectImage(type, amount));
        l.addView(effectImage);
        TextView text = new TextView(getActivity());
        text.setLayoutParams(new ViewGroup.LayoutParams(-1, -2));
        text.setText(getEffectText(type, amount));
        text.setTextColor(-1);
        text.setTextSize(2, 20.0f);
        text.setGravity(17);
        Typeface typeface = this.mTypeface;
        if (typeface != null) {
            text.setTypeface(typeface);
        }
        l.addView(text);
        this.mEffectHolder.addView(l);
        return l;
    }

    private int getEffectImage(int type, double amount) {
        if (amount > 1.0d) {
            switch (type) {
                case 0:
                    return R.drawable.info_health_clear;
                case 1:
                    return R.drawable.info_radiation_negative;
                case 2:
                    return R.drawable.info_anomaly_negative;
                case 3:
                    return R.drawable.info_psi_negative;
                case 4:
                    return R.drawable.info_burer_negative;
                case 5:
                    return R.drawable.info_radiation;
                case 6:
                    return R.drawable.info_controller_negative;
                case 7:
                    return R.drawable.info_health;
                case 8:
                    return R.drawable.info_radiation_negative;
                case 9:
                    return R.drawable.info_monolith;
                default:
                    return 0;
            }
        }
        switch (type) {
            case 0:
                return R.drawable.info_health_clear_negative;
            case 1:
                return R.drawable.info_radiation;
            case 2:
                return R.drawable.info_anomaly;
            case 3:
                return R.drawable.info_psi;
            case 4:
                return R.drawable.info_burer;
            case 5:
                return R.drawable.info_radiation_negative;
            case 6:
                return R.drawable.info_controller;
            case 7:
                return R.drawable.info_health_negative;
            case 8:
                return R.drawable.info_radiation;
            case 9:
                return R.drawable.info_monolith;
            default:
                return 0;
        }
    }

    private String getEffectText(int type, double amount) {
        switch (type) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 6:
            case 9:
                return String.format(Locale.US, "%1.0f%%", Double.valueOf(amountToPercent(amount)));
            case 5:
                return String.format(Locale.US, "%1.0fSv/h", Double.valueOf(amount));
            case 7:
                return String.format(Locale.US, "%1.0f%%", Double.valueOf(amount));
            case 8:
                return String.format(Locale.US, "%1.0f Sv", Double.valueOf(amount));
            default:
                return "50%";
        }
    }

    private double amountToPercent(double amount) {
        return amount < 1.0d ? (1.0d - amount) * 100.0d : (amount - 1.0d) * 100.0d;
    }
}

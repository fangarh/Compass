package net.afterday.compas.persistency.items;

import net.afterday.compas.core.inventory.items.Item;

/* JADX INFO: loaded from: classes.dex */
public class ItemDescriptorImpl implements ItemDescriptor {
    private static final String TAG = "ItemDescriptorImpl";
    private Item.CATEGORY category;
    private String description;
    private int descriptionId;
    private long duration;
    private int imageId;
    private boolean isArmor;
    private boolean isArtifact;
    private boolean isBooster;
    private boolean isConsumable;
    private boolean isDevice;
    private boolean isDropable;
    private boolean isUsable;
    private double[] modifiers;
    private String title;
    private int titleId;
    private int xpPoints;

    public ItemDescriptorImpl(Item.CATEGORY category) {
        this.titleId = -1;
        this.modifiers = new double[10];
        this.isArtifact = false;
        this.isBooster = false;
        this.isDevice = false;
        this.isArmor = false;
        this.isConsumable = true;
        this.isUsable = false;
        this.isDropable = true;
        this.descriptionId = -1;
        this.category = category;
        initModifiers();
    }

    public ItemDescriptorImpl(Item.CATEGORY category, String title) {
        this.titleId = -1;
        this.modifiers = new double[10];
        this.isArtifact = false;
        this.isBooster = false;
        this.isDevice = false;
        this.isArmor = false;
        this.isConsumable = true;
        this.isUsable = false;
        this.isDropable = true;
        this.descriptionId = -1;
        this.title = title;
        this.category = category;
        initModifiers();
    }

    public ItemDescriptorImpl(Item.CATEGORY category, int titleId) {
        this.titleId = -1;
        this.modifiers = new double[10];
        this.isArtifact = false;
        this.isBooster = false;
        this.isDevice = false;
        this.isArmor = false;
        this.isConsumable = true;
        this.isUsable = false;
        this.isDropable = true;
        this.descriptionId = -1;
        this.titleId = titleId;
        this.category = category;
        initModifiers();
    }

    private void initModifiers() {
        for (int i = 0; i < 10; i++) {
            this.modifiers[i] = -9.9999999E7d;
        }
    }

    public ItemDescriptorImpl setImage(int image) {
        this.imageId = image;
        return this;
    }

    public ItemDescriptorImpl setTitle(String title) {
        this.title = title;
        return this;
    }

    public ItemDescriptorImpl setTitleId(int titleId) {
        this.titleId = titleId;
        return this;
    }

    public ItemDescriptorImpl setDescription(String description) {
        this.description = description;
        return this;
    }

    public ItemDescriptorImpl setDescription(int descriptionId) {
        this.descriptionId = descriptionId;
        return this;
    }

    public ItemDescriptorImpl addModifier(int modifier, double value) {
        this.modifiers[modifier] = value;
        return this;
    }

    public ItemDescriptorImpl setArtefact(boolean isArtifact) {
        this.isArtifact = isArtifact;
        return this;
    }

    public ItemDescriptorImpl setBooster(boolean isBooster) {
        this.isBooster = isBooster;
        return this;
    }

    public ItemDescriptorImpl setDevice(boolean isDevice) {
        this.isDevice = isDevice;
        return this;
    }

    public ItemDescriptorImpl setArmor(boolean isArmor) {
        this.isArmor = isArmor;
        return this;
    }

    public ItemDescriptorImpl setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    public ItemDescriptorImpl setXpPoints(int xpPoints) {
        this.xpPoints = xpPoints;
        return this;
    }

    public ItemDescriptorImpl setConsumable(boolean isConsumable) {
        this.isConsumable = isConsumable;
        return this;
    }

    public ItemDescriptorImpl setUsable(boolean isUsable) {
        this.isUsable = isUsable;
        return this;
    }

    public ItemDescriptorImpl setDropable(boolean isDropable) {
        this.isDropable = isDropable;
        return this;
    }

    public ItemDescriptorImpl setCategory(Item.CATEGORY category) {
        this.category = category;
        return this;
    }

    @Override // net.afterday.compas.persistency.items.ItemDescriptor
    public int getImage() {
        return this.imageId;
    }

    @Override // net.afterday.compas.persistency.items.ItemDescriptor
    public String getName() {
        return this.title;
    }

    @Override // net.afterday.compas.persistency.items.ItemDescriptor
    public int getNameId() {
        return this.titleId;
    }

    @Override // net.afterday.compas.persistency.items.ItemDescriptor
    public boolean isBooster() {
        return this.isBooster;
    }

    @Override // net.afterday.compas.persistency.items.ItemDescriptor
    public boolean isDevice() {
        return this.isDevice;
    }

    @Override // net.afterday.compas.persistency.items.ItemDescriptor
    public boolean isArmor() {
        return this.isArmor;
    }

    @Override // net.afterday.compas.persistency.items.ItemDescriptor
    public boolean isArtefact() {
        return this.isArtifact;
    }

    @Override // net.afterday.compas.persistency.items.ItemDescriptor
    public boolean isSingleUse() {
        return (this.isArtifact || this.isBooster || this.isDevice || this.isArmor) ? false : true;
    }

    @Override // net.afterday.compas.persistency.items.ItemDescriptor
    public boolean isConsumable() {
        return this.isConsumable;
    }

    @Override // net.afterday.compas.persistency.items.ItemDescriptor
    public long getDuration() {
        return this.duration;
    }

    @Override // net.afterday.compas.persistency.items.ItemDescriptor
    public double[] getModifiers() {
        return this.modifiers;
    }

    @Override // net.afterday.compas.persistency.items.ItemDescriptor
    public int getXpPoints() {
        return this.xpPoints;
    }

    @Override // net.afterday.compas.persistency.items.ItemDescriptor
    public String getDescription() {
        return this.description;
    }

    @Override // net.afterday.compas.persistency.items.ItemDescriptor
    public int getDescriptionId() {
        return this.descriptionId;
    }

    @Override // net.afterday.compas.persistency.items.ItemDescriptor
    public boolean isUsable() {
        return this.isUsable;
    }

    @Override // net.afterday.compas.persistency.items.ItemDescriptor
    public boolean isDropable() {
        return this.isDropable;
    }

    @Override // net.afterday.compas.persistency.items.ItemDescriptor
    public Item.CATEGORY getCategory() {
        return this.category;
    }

    public String toString() {
        return "";
    }
}

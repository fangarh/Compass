package net.afterday.compas.persistency.hardcoded;

import android.util.Log;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.afterday.compas.R;
import net.afterday.compas.core.inventory.items.Item;
import net.afterday.compas.persistency.items.ItemDescriptor;
import net.afterday.compas.persistency.items.ItemDescriptorImpl;
import net.afterday.compas.persistency.items.ItemsPersistency;

/* JADX INFO: loaded from: classes.dex */
public class HItemsPersistency implements ItemsPersistency {
    private static final String TAG = "HItemsPersistency";
    private Map<String, ItemDescriptor> possibleItems = new HashMap();
    private Map<Integer, List<ItemDescriptor>> itemsByLevel = new HashMap();

    public HItemsPersistency() {
        setupItems();
    }

    @Override // net.afterday.compas.persistency.items.ItemsPersistency
    public Map<String, ItemDescriptor> getItemsByCode() {
        return this.possibleItems;
    }

    @Override // net.afterday.compas.persistency.items.ItemsPersistency
    public ItemDescriptor getItemForCode(String code) {
        if (this.possibleItems.containsKey(code)) {
            return this.possibleItems.get(code);
        }
        return null;
    }

    @Override // net.afterday.compas.persistency.items.ItemsPersistency
    public Map<Integer, List<ItemDescriptor>> getItemsAddeWithLevel() {
        return this.itemsByLevel;
    }

    private void setupItems() {
        Log.w(TAG, "setupItems");
        this.possibleItems.put("dageban1", new ItemDescriptorImpl(Item.CATEGORY.MEDKITS, R.string.item_bandage).setImage(R.drawable.item_bandage).setArtefact(false).addModifier(7, 10.0d).setDescription(R.string.desc_bandage));
        this.possibleItems.put("staidkitfir1", new ItemDescriptorImpl(Item.CATEGORY.MEDKITS, R.string.item_medkit).setImage(R.drawable.item_medkit).setArtefact(false).addModifier(7, 20.0d).setDescription(R.string.desc_medkit));
        this.possibleItems.put("yfirstaidkitarm1", new ItemDescriptorImpl(Item.CATEGORY.MEDKITS, R.string.item_army_medkit).setImage(R.drawable.item_army_medkit).setArtefact(false).addModifier(7, 30.0d).setDescription(R.string.desc_army_medkit));
        this.possibleItems.put("iwinkletabletsper1", new ItemDescriptorImpl(Item.CATEGORY.MEDKITS, R.string.item_vinca).setImage(R.drawable.item_vinca).setArtefact(false).addModifier(7, 40.0d).addModifier(8, -1.0d).setDescription(R.string.desc_vinca));
        this.possibleItems.put("entificfirstaidkitsci1", new ItemDescriptorImpl(Item.CATEGORY.MEDKITS, R.string.item_scientific_medkit).setImage(R.drawable.item_scientific_medkit).setArtefact(false).addModifier(7, 50.0d).addModifier(8, -3.0d).setDescription(R.string.desc_scientific_medkit));
        this.possibleItems.put("skcassocakdov1", new ItemDescriptorImpl(Item.CATEGORY.ANTIRADS, R.string.item_vodka).setImage(R.drawable.item_vodka).setArtefact(false).addModifier(7, -10.0d).addModifier(8, -1.0d).setDescription(R.string.desc_vodka));
        this.possibleItems.put("iradant1", new ItemDescriptorImpl(Item.CATEGORY.ANTIRADS, R.string.item_antirad).setImage(R.drawable.item_antirad).setArtefact(false).addModifier(7, -20.0d).addModifier(8, -7.0d).setDescription(R.string.desc_antirad));
        this.possibleItems.put("bioticana1", new ItemDescriptorImpl(Item.CATEGORY.ANTIRADS, R.string.item_anabiotic).setImage(R.drawable.item_anabiotic).setTitle("Anabiotic").setArtefact(false).addModifier(7, -30.0d).setDescription(R.string.desc_anabiotic));
        this.possibleItems.put("ioprotectorrad1", new ItemDescriptorImpl(Item.CATEGORY.BOOSTERS, R.string.item_b190).setImage(R.drawable.item_b190).setArtefact(false).addModifier(1, 0.0d).setBooster(true).setDuration(60000L).setDescription(R.string.desc_b190));
        this.possibleItems.put("psickadeblo1", new ItemDescriptorImpl(Item.CATEGORY.BOOSTERS, R.string.item_psyblock).setImage(R.drawable.item_psy_block).setArtefact(false).addModifier(3, 0.0d).setBooster(true).setDuration(60000L).setDescription(R.string.desc_psyblock));
        this.possibleItems.put("idoteant1", new ItemDescriptorImpl(Item.CATEGORY.BOOSTERS, R.string.item_ip2).setImage(R.drawable.item_ip2_antidote).setArtefact(false).addModifier(2, 0.0d).setBooster(true).setDuration(60000L).setDescription(R.string.desc_ip2));
        this.possibleItems.put("ctureoffreedomradvinoktin1", new ItemDescriptorImpl(Item.CATEGORY.BOOSTERS, R.string.item_green_svoboda).setImage(R.drawable.item_green_svoboda_serum).setArtefact(false).setBooster(true).setDuration(7200000L).addModifier(1, 0.1d).setDescription(R.string.desc_green_svoboda));
        this.possibleItems.put("ertytincturemenvinoklib1", new ItemDescriptorImpl(Item.CATEGORY.BOOSTERS, R.string.item_yellow_svoboda).setImage(R.drawable.item_yellow_svoboda_serum).setArtefact(false).setBooster(true).setDuration(7200000L).addModifier(3, 0.1d).setDescription(R.string.desc_yellow_svoboda));
        this.possibleItems.put("PsionicHelmetMark1691", new ItemDescriptorImpl(Item.CATEGORY.DEVICES, R.string.item_shlem1).setImage(R.drawable.item_shlem1).setArtefact(false).setDevice(true).setDuration(1800000L).addModifier(3, 0.01d).addModifier(6, 0.7d).setDescription(R.string.desc_shlem1));
        this.possibleItems.put("PsionicHelmetMark2851", new ItemDescriptorImpl(Item.CATEGORY.DEVICES, R.string.item_shlem2).setImage(R.drawable.item_shlem2).setArtefact(false).setDevice(true).setDuration(3600000L).addModifier(3, 0.0d).addModifier(9, 0.01d).addModifier(6, 0.4d).setDescription(R.string.desc_shlem2));
        this.possibleItems.put("therjacketlea1", new ItemDescriptorImpl(Item.CATEGORY.ARMORS, R.string.item_leather_jacket).setImage(R.drawable.item_leather_jacket).setArtefact(false).setArmor(true).addModifier(1, 0.9d).setDuration(3600000L).setDescription(R.string.desc_leather_jacket));
        this.possibleItems.put("rjvtprjkjucyb1", new ItemDescriptorImpl(Item.CATEGORY.ARMORS, R.string.item_ssp99).setImage(R.drawable.item_ssp_99_ecologist).setArtefact(false).setArmor(true).addModifier(1, 0.2d).addModifier(2, 0.4d).addModifier(3, 0.6d).setDuration(3600000L).setDescription(R.string.desc_ssp99));
        this.possibleItems.put("lkersuitzaryasta1", new ItemDescriptorImpl(Item.CATEGORY.ARMORS, R.string.item_stalker_suit).setImage(R.drawable.item_stalker_suit).setArtefact(false).setArmor(true).addModifier(1, 0.7d).addModifier(2, 0.8d).addModifier(3, 0.9d).setDuration(7200000L).setDescription(R.string.desc_stalker_suit));
        this.possibleItems.put("aoverallssev1", new ItemDescriptorImpl(Item.CATEGORY.ARMORS, R.string.item_seva_suit).setImage(R.drawable.item_seva_suit).setArtefact(false).setArmor(true).addModifier(1, 0.5d).addModifier(2, 0.6d).addModifier(3, 0.7d).setDuration(7200000L).setDescription(R.string.desc_seva_suit));
        this.possibleItems.put("skeletonexo1", new ItemDescriptorImpl(Item.CATEGORY.ARMORS, R.string.item_exoskeleton).setImage(R.drawable.item_exoskeleton).setArtefact(false).setArmor(true).addModifier(1, 0.7d).addModifier(2, 0.6d).addModifier(3, 0.9d).addModifier(0, 2.0d).setDuration(7200000L).setDescription(R.string.desc_exoskeleton));
        this.possibleItems.put("oredsuitberylarm1", new ItemDescriptorImpl(Item.CATEGORY.ARMORS, R.string.item_berill_suit).setImage(R.drawable.item_berill).setArtefact(false).setArmor(true).addModifier(1, 0.8d).addModifier(2, 0.8d).addModifier(3, 0.8d).addModifier(0, 3.0d).setDuration(10800000L).setDescription(R.string.desc_berill_suit));
        this.possibleItems.put("cenaryjumpsuitmer1", new ItemDescriptorImpl(Item.CATEGORY.ARMORS, R.string.item_merc_suit).setImage(R.drawable.item_merc_suit).setArtefact(false).setArmor(true).addModifier(1, 0.7d).addModifier(2, 0.8d).addModifier(3, 0.9d).setDuration(10800000L).setDescription(R.string.desc_merc_suit));
        this.possibleItems.put("bezdebtcom1", new ItemDescriptorImpl(Item.CATEGORY.ARMORS, R.string.item_duty_suit).setImage(R.drawable.item_duty).setArtefact(false).setArmor(true).addModifier(1, 0.7d).addModifier(2, 0.8d).addModifier(3, 0.9d).setDuration(10800000L).setDescription(R.string.desc_duty_suit));
        this.possibleItems.put("psuitfreedomjum1", new ItemDescriptorImpl(Item.CATEGORY.ARMORS, R.string.item_freedom_suit).setImage(R.drawable.item_freedom).setArtefact(false).setArmor(true).addModifier(1, 0.7d).addModifier(2, 0.8d).addModifier(3, 0.9d).setDuration(10800000L).setDescription(R.string.desc_freedom_suit));
        this.possibleItems.put("arskyjumpsuitcle1", new ItemDescriptorImpl(Item.CATEGORY.ARMORS, R.string.item_clearsky_suit).setImage(R.drawable.item_clear_sky).setArtefact(false).setArmor(true).addModifier(1, 0.7d).addModifier(2, 0.8d).addModifier(3, 0.9d).setDuration(10800000L).setDescription(R.string.desc_clearsky_suit));
        this.possibleItems.put("ditcloakban1", new ItemDescriptorImpl(Item.CATEGORY.ARMORS, R.string.item_bandit_suit).setImage(R.drawable.item_bandit).setArtefact(false).setArmor(true).addModifier(1, 0.7d).addModifier(2, 0.8d).addModifier(3, 0.9d).setDuration(10800000L).setDescription(R.string.desc_bandit_suit));
        this.possibleItems.put("olithjumpsuitmon1", new ItemDescriptorImpl(Item.CATEGORY.ARMORS, R.string.item_monolith_suit).setImage(R.drawable.item_monolith).setArtefact(false).setArmor(true).addModifier(1, 0.0d).addModifier(2, 0.8d).addModifier(3, 0.0d).setDuration(10800000L).setDescription(R.string.desc_monolith_suit));
        this.possibleItems.put("psdro1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_droplets).setImage(R.drawable.item_droplets).setArtefact(true).addModifier(1, 0.9d).setXpPoints(2).setDescription(R.string.desc_droplets));
        this.possibleItems.put("blebub1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_bubble).setImage(R.drawable.item_bubble).setArtefact(true).addModifier(1, 0.8d).setXpPoints(5).setDescription(R.string.desc_bubble));
        this.possibleItems.put("rnfireballtho1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_fireball).setImage(R.drawable.item_fireball).setArtefact(true).addModifier(1, 0.7d).setXpPoints(10).setDescription(R.string.desc_fireball));
        this.possibleItems.put("stalcry1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_crystal).setImage(R.drawable.item_crystal).setArtefact(true).addModifier(1, 0.6d).setXpPoints(10).setDescription(R.string.desc_crystal));
        this.possibleItems.put("ckakolu1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_thorn).setImage(R.drawable.item_thorn).setArtefact(true).addModifier(1, 0.5d).addModifier(3, 1.25d).addModifier(2, 1.25d).setXpPoints(10).setDescription(R.string.desc_thorn));
        this.possibleItems.put("lyfishjel1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_jellyfish).setImage(R.drawable.item_jellyfish).setArtefact(true).addModifier(0, 1.5d).setXpPoints(1).setDescription(R.string.desc_jellyfish));
        this.possibleItems.put("azgl1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_eye).setImage(R.drawable.item_eye).setArtefact(true).addModifier(0, 2.0d).setXpPoints(2).setDescription(R.string.desc_eye));
        this.possibleItems.put("obokkol1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_kolobok).setImage(R.drawable.item_kolobok).setArtefact(true).addModifier(0, 3.0d).setXpPoints(5).setDescription(R.string.desc_kolobok));
        this.possibleItems.put("dfishgol1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_goldfish).setImage(R.drawable.item_goldfish).setArtefact(true).addModifier(0, 4.0d).addModifier(6, 0.1d).setXpPoints(15).setDescription(R.string.desc_goldfish));
        this.possibleItems.put("sbeadsmom1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_mamas_beads).setImage(R.drawable.item_mamas_beads).setArtefact(true).addModifier(0, 5.0d).addModifier(4, 0.1d).setXpPoints(15).setDescription(R.string.desc_mamas_beads));
        this.possibleItems.put("amic1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_mica).setImage(R.drawable.item_mica).setArtefact(true).addModifier(0, 6.0d).addModifier(4, 0.1d).addModifier(6, 0.1d).addModifier(1, 1.25d).addModifier(3, 1.25d).addModifier(2, 1.25d).setXpPoints(20).setDescription(R.string.desc_mica));
        this.possibleItems.put("nlightspringmoo1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_moonlight).setImage(R.drawable.item_moonlight).setArtefact(true).addModifier(1, 1.2d).addModifier(2, 0.5d).setXpPoints(10).setDescription(R.string.desc_moonlight));
        this.possibleItems.put("neflowersto1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_stone_flower).setImage(R.drawable.item_stone_flower).setArtefact(true).addModifier(1, 1.2d).addModifier(3, 0.8d).setXpPoints(5).setDescription(R.string.desc_stone_flower));
        this.possibleItems.put("mfil1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_pellicle).setImage(R.drawable.item_pellicle).setArtefact(true).addModifier(1, 1.4d).addModifier(3, 0.6d).setXpPoints(10).setDescription(R.string.desc_pellicle));
        this.possibleItems.put("nightstareye1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_night_star).setImage(R.drawable.item_night_star).setArtefact(true).addModifier(1, 1.6d).addModifier(3, 0.4d).setXpPoints(15).setDescription(R.string.desc_night_star));
        this.possibleItems.put("passcom1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_compass).setImage(R.drawable.item_compass).setArtefact(true).addModifier(1, 2.0d).addModifier(3, 0.4d).addModifier(2, 0.01d).setXpPoints(30).setDescription(R.string.desc_compass));
        this.possibleItems.put("mydum1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_shell).setImage(R.drawable.item_shell).setArtefact(true).addModifier(5, 1.0d).setDescription(R.string.desc_shell));
        this.possibleItems.put("ioactiveinsulatorrad1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_altered_insulator).setImage(R.drawable.item_altered_insulator).setArtefact(true).addModifier(5, 7.0d).setDescription(R.string.desc_altered_insulator));
        this.possibleItems.put("ckenergybla1", new ItemDescriptorImpl(Item.CATEGORY.ARTIFACTS, R.string.item_black_energy).setImage(R.drawable.item_black_energy).setArtefact(true).addModifier(5, 14.0d).setDescription(R.string.desc_black_energy));
        this.possibleItems.put("Binoculars135", new ItemDescriptorImpl(Item.CATEGORY.UPGRADES, R.string.item_binocl).setImage(R.drawable.item_binocl).setArtefact(false).setConsumable(false).setXpPoints(0).setDescription(R.string.desc_binocl));
        this.possibleItems.put("icalsightopt", new ItemDescriptorImpl(Item.CATEGORY.UPGRADES, R.string.item_optics).setImage(R.drawable.item_optics).setArtefact(false).setConsumable(false).setXpPoints(0).setDescription(R.string.desc_optics));
        this.possibleItems.put("limatorcol", new ItemDescriptorImpl(Item.CATEGORY.UPGRADES, R.string.item_collimator).setImage(R.drawable.item_collimator).setArtefact(false).setConsumable(false).setXpPoints(0).setDescription(R.string.desc_collimator));
        this.possibleItems.put("htvisiondevicenig", new ItemDescriptorImpl(Item.CATEGORY.UPGRADES, R.string.item_night_vision).setImage(R.drawable.item_night_vision).setArtefact(false).setConsumable(false).setXpPoints(0).setDescription(R.string.desc_night_vision));
        this.possibleItems.put("kietalkiewal", new ItemDescriptorImpl(Item.CATEGORY.UPGRADES, R.string.item_walkie_talkie).setImage(R.drawable.item_walkie_talkie).setArtefact(false).setConsumable(false).setXpPoints(0).setDescription(R.string.desc_walkie_talkie));
        this.possibleItems.put("nadelaunchergre", new ItemDescriptorImpl(Item.CATEGORY.UPGRADES, R.string.item_grenade_launcher).setImage(R.drawable.item_grenade_launcher).setArtefact(false).setConsumable(false).setXpPoints(0).setDescription(R.string.desc_grenade_launcher));
        this.possibleItems.put("dratde1", new ItemDescriptorImpl(Item.CATEGORY.FOOD, R.string.item_dead_rat).setImage(R.drawable.item_rat).setArtefact(false).addModifier(7, 30.0d).addModifier(8, 2.0d).setDescription(R.string.desc_dead_rat));
        this.possibleItems.put("onbat1", new ItemDescriptorImpl(Item.CATEGORY.FOOD, R.string.item_bread).setImage(R.drawable.item_bread).setArtefact(false).addModifier(7, 3.0d).setXpPoints(1).setDescription(R.string.desc_bread));
        this.possibleItems.put("sagesticksau1", new ItemDescriptorImpl(Item.CATEGORY.FOOD, R.string.item_sausage).setImage(R.drawable.item_sausage).setArtefact(false).addModifier(7, 6.0d).setXpPoints(2).setDescription(R.string.desc_sausage));
        this.possibleItems.put("ofstewcan1", new ItemDescriptorImpl(Item.CATEGORY.FOOD, R.string.item_canned_meat).setImage(R.drawable.item_can).setArtefact(false).addModifier(7, 9.0d).setXpPoints(3).setDescription(R.string.desc_canned_meat));
        this.possibleItems.put("Doktorskaja1481", new ItemDescriptorImpl(Item.CATEGORY.FOOD, R.string.item_doktorskaja).setImage(R.drawable.item_doktorskaja).setArtefact(false).addModifier(7, 12.0d).setXpPoints(5).setDescription(R.string.desc_doktorskaja));
        this.possibleItems.put("Delikates1481", new ItemDescriptorImpl(Item.CATEGORY.FOOD, R.string.item_delikates).setImage(R.drawable.item_delikates).setArtefact(false).addModifier(7, 15.0d).setXpPoints(6).setDescription(R.string.desc_delikates));
        this.possibleItems.put("culesher1", new ItemDescriptorImpl(Item.CATEGORY.FOOD, R.string.item_hercules).setImage(R.drawable.item_hercules).setArtefact(false).setBooster(true).setDuration(1800000L).addModifier(0, 1.5d).setDescription(R.string.desc_hercules));
        this.possibleItems.put("energystalker1", new ItemDescriptorImpl(Item.CATEGORY.FOOD, R.string.item_energy_drink).setImage(R.drawable.item_energy_drink).setArtefact(false).setBooster(true).setDuration(1800000L).addModifier(0, 2.0d).setDescription(R.string.desc_energy_drink));
        this.possibleItems.put("tpowerengineerlef1", new ItemDescriptorImpl(Item.CATEGORY.FOOD, R.string.item_red_devil).setImage(R.drawable.item_red_devil).setArtefact(false).setBooster(true).setDuration(900000L).addModifier(0, 0.5d).addModifier(1, 0.5d).setDescription(R.string.desc_red_devil));
        this.possibleItems.put("batknifecom", new ItemDescriptorImpl(Item.CATEGORY.WEAPONS, R.string.item_knife).setImage(R.drawable.item_knife).setArtefact(false).setConsumable(false).setXpPoints(0).setDescription(R.string.desc_knife));
        this.possibleItems.put("tolpis", new ItemDescriptorImpl(Item.CATEGORY.WEAPONS, R.string.item_pistol).setImage(R.drawable.item_pistol).setArtefact(false).setConsumable(false).setXpPoints(0).setDescription(R.string.desc_pistol));
        this.possibleItems.put("tgunsho", new ItemDescriptorImpl(Item.CATEGORY.WEAPONS, R.string.item_shotgun).setImage(R.drawable.item_shotgun).setArtefact(false).setConsumable(false).setXpPoints(0).setDescription(R.string.desc_shotgun));
        this.possibleItems.put("machinegunsub", new ItemDescriptorImpl(Item.CATEGORY.WEAPONS, R.string.item_submachinegun).setImage(R.drawable.item_smg).setArtefact(false).setConsumable(false).setXpPoints(0).setDescription(R.string.desc_submachinegun));
        this.possibleItems.put("hinemac", new ItemDescriptorImpl(Item.CATEGORY.WEAPONS, R.string.item_ak).setImage(R.drawable.item_ak).setArtefact(false).setConsumable(false).setXpPoints(0).setDescription(R.string.desc_ak));
        this.possibleItems.put("M4type148", new ItemDescriptorImpl(Item.CATEGORY.WEAPONS, R.string.item_m4).setImage(R.drawable.item_m4).setArtefact(false).setConsumable(false).setXpPoints(0).setDescription(R.string.desc_m4));
        this.possibleItems.put("VAL648", new ItemDescriptorImpl(Item.CATEGORY.WEAPONS, R.string.item_val).setImage(R.drawable.item_val).setArtefact(false).setConsumable(false).setXpPoints(0).setDescription(R.string.desc_val));
        this.possibleItems.put("perriflesni", new ItemDescriptorImpl(Item.CATEGORY.WEAPONS, R.string.item_sniper).setImage(R.drawable.item_sniper).setArtefact(false).setConsumable(false).setXpPoints(0).setDescription(R.string.desc_sniper));
        this.possibleItems.put("hinegunmac", new ItemDescriptorImpl(Item.CATEGORY.WEAPONS, R.string.item_machinegun).setImage(R.drawable.item_machinegun).setArtefact(false).setConsumable(false).setXpPoints(0).setDescription(R.string.desc_machinegun));
        this.possibleItems.put("kenproductbro", new ItemDescriptorImpl(Item.CATEGORY.WEAPONS, R.string.item_gauss_gun).setImage(R.drawable.item_gauss).setArtefact(false).setConsumable(false).setXpPoints(50).setDescription(R.string.desc_gauss_gun));
        this.possibleItems.put("Garmoshka8531", new ItemDescriptorImpl(Item.CATEGORY.HABAR, R.string.item_garmoshka).setImage(R.drawable.item_garmoshka).setArtefact(false).setConsumable(false).setXpPoints(4).setDescription(R.string.desc_garmoshka));
        this.possibleItems.put("Bag8531", new ItemDescriptorImpl(Item.CATEGORY.HABAR, R.string.item_bag).setImage(R.drawable.item_bag).setArtefact(false).setConsumable(false).setXpPoints(5).setDescription(R.string.desc_bag));
        this.possibleItems.put("Gasoline1", new ItemDescriptorImpl(Item.CATEGORY.HABAR, R.string.item_gasoline).setImage(R.drawable.item_gasoline).setArtefact(false).setConsumable(false).setXpPoints(10).setDescription(R.string.desc_gasoline));
        this.possibleItems.put("Maps1", new ItemDescriptorImpl(Item.CATEGORY.HABAR, R.string.item_maps).setImage(R.drawable.item_maps).setArtefact(false).setConsumable(false).setXpPoints(15).setDescription(R.string.desc_maps));
        this.possibleItems.put("teredguitarbat1", new ItemDescriptorImpl(Item.CATEGORY.HABAR, R.string.item_guitar).setImage(R.drawable.item_guitar).setArtefact(false).setConsumable(false).setXpPoints(20).setDescription(R.string.desc_guitar));
        this.possibleItems.put("lboxtoo1", new ItemDescriptorImpl(Item.CATEGORY.HABAR, R.string.item_instruments).setImage(R.drawable.item_instruments).setArtefact(false).setConsumable(false).setXpPoints(30).setDescription(R.string.desc_instruments));
        this.possibleItems.put("omprehensibledeviceinc1", new ItemDescriptorImpl(Item.CATEGORY.HABAR, R.string.item_psionic_device).setImage(R.drawable.item_psionic_device).setArtefact(false).setConsumable(false).setXpPoints(50).setDescription(R.string.desc_psionic_device));
        this.possibleItems.put("InfoPDA1", new ItemDescriptorImpl(Item.CATEGORY.HABAR, R.string.item_pda).setImage(R.drawable.item_pda).setArtefact(false).setConsumable(false).setXpPoints(50).setDescription(R.string.desc_pda));
        this.possibleItems.put("Calibrating0011", new ItemDescriptorImpl(Item.CATEGORY.DEVICES, R.string.item_calibrating_10).setImage(R.drawable.item_calibrating_10).setArtefact(true).setConsumable(false).setXpPoints(0).addModifier(1, 0.9d).addModifier(2, 0.9d).addModifier(3, 0.9d).addModifier(4, 0.9d).addModifier(6, 0.9d).addModifier(9, 0.9d).setDescription(R.string.desc_calibrating_10));
        this.possibleItems.put("Calibrating0251", new ItemDescriptorImpl(Item.CATEGORY.DEVICES, R.string.item_calibrating_25).setImage(R.drawable.item_calibrating_25).setArtefact(true).setConsumable(false).setXpPoints(0).addModifier(1, 0.75d).addModifier(2, 0.75d).addModifier(3, 0.75d).addModifier(4, 0.75d).addModifier(6, 0.75d).addModifier(9, 0.75d).setDescription(R.string.desc_calibrating_25));
        this.possibleItems.put("Calibrating051", new ItemDescriptorImpl(Item.CATEGORY.DEVICES, R.string.item_calibrating_50).setImage(R.drawable.item_calibrating_50).setArtefact(true).setConsumable(false).setXpPoints(0).addModifier(1, 0.5d).addModifier(2, 0.5d).addModifier(3, 0.5d).addModifier(4, 0.5d).addModifier(6, 0.5d).addModifier(9, 0.5d).setDescription(R.string.desc_calibrating_50));
        this.possibleItems.put("Calibrating0751", new ItemDescriptorImpl(Item.CATEGORY.DEVICES, R.string.item_calibrating_75).setImage(R.drawable.item_calibrating_75).setArtefact(true).setConsumable(false).setXpPoints(0).addModifier(1, 0.25d).addModifier(2, 0.25d).addModifier(3, 0.25d).addModifier(4, 0.25d).addModifier(6, 0.25d).addModifier(9, 0.25d).setDescription(R.string.desc_calibrating_75));
        setupLevels();
    }

    private void setupLevels() {
    }
}

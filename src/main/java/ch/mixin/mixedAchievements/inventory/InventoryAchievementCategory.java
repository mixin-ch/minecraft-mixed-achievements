package ch.mixin.mixedAchievements.inventory;

import ch.mixin.mixedAchievements.api.InfoAchievement;
import ch.mixin.mixedAchievements.api.InfoAchievementStage;
import ch.mixin.mixedAchievements.blueprint.AchievementItemSetup;
import ch.mixin.mixedAchievements.data.DataPlayerAchievement;
import ch.mixin.mixedAchievements.main.MixedAchievementsData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InventoryAchievementCategory extends InventoryAchievementElement {
    protected final String inventoryName;
    protected final AchievementItemSetup achievementItemSetup;
    protected HashMap<Integer, InventoryAchievementElement> inventoryAchievementElementMap;

    public InventoryAchievementCategory(MixedAchievementsData mixedAchievementsData, InventoryAchievementCategory parent, String inventoryName, AchievementItemSetup achievementItemSetup) {
        super(mixedAchievementsData, parent);
        this.inventoryName = inventoryName;
        this.achievementItemSetup = achievementItemSetup;
        inventoryAchievementElementMap = new HashMap<>();
    }

    public Inventory generateInventory(Player player) {
        int inventorySize = inventoryAchievementElementMap.size();
        Inventory inventory = Bukkit.createInventory(null, 9 * (int) Math.ceil((inventorySize + 1) / 9.0), inventoryName);

        for (int slot : inventoryAchievementElementMap.keySet()) {
            InventoryAchievementElement iae = inventoryAchievementElementMap.get(slot);
            inventory.setItem(slot, makeItemSlot(iae, player));
        }

        inventory.setItem(InventoryAchievementManager.CancelSlot, makeItemBasicSlot(InventoryAchievementManager.CancelItem));
        return inventory;
    }

    private ItemStack makeItemSlot(InventoryAchievementElement iae, Player player) {
        if (iae instanceof InventoryAchievementLeaf) {
            return makeItemLeafSlot((InventoryAchievementLeaf) iae, player);
        } else {
            return makeItemFolderSlot((InventoryAchievementCategory) iae);
        }
    }

    private ItemStack makeItemBasicSlot(AchievementItemSetup ais) {
        ItemStack item = new ItemStack(ais.getMaterial(), ais.getAmount());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ais.getName());
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack makeItemLeafSlot(InventoryAchievementLeaf ial, Player player) {
        InfoAchievement infoAchievement = ial.getInfoAchievement();
        DataPlayerAchievement dpa = mixedAchievementsData.getAchievementManager().fetchDataPlayerAchievement(infoAchievement.getSetId(), infoAchievement.getDataAchievement().getAchievementId(), player.getUniqueId().toString());
        List<AchievementItemSetup> achievementItemSetupList = ial.getAchievementItemSetupList();
        int stageSize = achievementItemSetupList.size();
        int currentStage = dpa.getStage();
        int topCurrentStage = currentStage;
        boolean absolutCompleted = false;

        while (topCurrentStage >= stageSize) {
            absolutCompleted = true;
            topCurrentStage--;
        }

        AchievementItemSetup achievementItemSetup = achievementItemSetupList.get(topCurrentStage);

        ItemStack item = new ItemStack(achievementItemSetup.getMaterial(), achievementItemSetup.getAmount());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(achievementItemSetup.getName());
            ArrayList<String> Lore = new ArrayList<>();
            InfoAchievementStage ias = infoAchievement.getInfoAchievementStageList().get(topCurrentStage);

            if (absolutCompleted) {
                meta.addEnchant(Enchantment.LOYALTY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                Lore.add(InventoryAchievementManager.CompletedColor + "" + ChatColor.BOLD + ChatColor.ITALIC + "Completed");

                if (infoAchievement.usesPoints()) {
                    Lore.add(InventoryAchievementManager.CompletedColor + "" + ChatColor.BOLD + ChatColor.ITALIC + dpa.getPoints() + "/" + ias.getMaxPoints());
                }
            } else {
                if (infoAchievement.usesPoints()) {
                    Lore.add(InventoryAchievementManager.IncompletedColor + "" + ChatColor.BOLD + ChatColor.ITALIC + dpa.getPoints() + "/" + ias.getMaxPoints());
                }
            }

            Lore.addAll(achievementItemSetup.getLoreList());

            if (stageSize > 1) {
                if (currentStage > 0) {
                    Lore.add("");

                    Lore.add(InventoryAchievementManager.StageColor + "" + ChatColor.BOLD + ChatColor.ITALIC + "Completed Stages");
                }

                if (infoAchievement.usesPoints()) {
                    for (int i = 0; i < currentStage; i++) {
                        String name = achievementItemSetupList.get(i).getName();
                        int maxPoints = infoAchievement.getInfoAchievementStageList().get(i).getMaxPoints();
                        Lore.add(ChatColor.RESET + "" + ChatColor.WHITE + "[" + maxPoints + "] " + name);
                    }
                } else {
                    for (int i = 0; i < currentStage; i++) {
                        String name = achievementItemSetupList.get(i).getName();
                        Lore.add(ChatColor.RESET + "" + ChatColor.WHITE + name);
                    }
                }
            }

            meta.setLore(Lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack makeItemFolderSlot(InventoryAchievementCategory aife) {
        AchievementItemSetup ais = aife.getAchievementItemSetup();
        ItemStack item = new ItemStack(ais.getMaterial(), ais.getAmount());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ais.getName());
            ArrayList<String> Lore = new ArrayList<>();
            Lore.add(InventoryAchievementManager.CategoryColor + "" + ChatColor.BOLD + ChatColor.ITALIC + "Category");
            Lore.addAll(ais.getLoreList());
            meta.setLore(Lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    public String getInventoryName() {
        return inventoryName;
    }

    public AchievementItemSetup getAchievementItemSetup() {
        return achievementItemSetup;
    }

    public HashMap<Integer, InventoryAchievementElement> getInventoryAchievementElementMap() {
        return inventoryAchievementElementMap;
    }
}

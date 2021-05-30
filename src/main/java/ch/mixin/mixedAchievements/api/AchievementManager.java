package ch.mixin.mixedAchievements.api;

import ch.mixin.mixedAchievements.blueprint.*;
import ch.mixin.mixedAchievements.data.DataAchievement;
import ch.mixin.mixedAchievements.data.DataAchievementRoot;
import ch.mixin.mixedAchievements.data.DataAchievementSet;
import ch.mixin.mixedAchievements.data.DataPlayerAchievement;
import ch.mixin.mixedAchievements.inventory.*;
import ch.mixin.mixedAchievements.main.MixedAchievementsManagerAccessor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class AchievementManager {
    private final MixedAchievementsManagerAccessor mixedAchievementsManagerAccessor;

    private final TreeMap<String, InfoAchievementSet> infoAchievementSetMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public AchievementManager(MixedAchievementsManagerAccessor mixedAchievementsManagerAccessor) {
        this.mixedAchievementsManagerAccessor = mixedAchievementsManagerAccessor;
    }


    public void integrateAchievementSet(BlueprintAchievementSet blueprintAchievementSet) {
        InventoryAchievementManager inventoryAchievementManager = mixedAchievementsManagerAccessor.getAchievementInventoryManager();
        String setId = blueprintAchievementSet.getSetId();

        if (infoAchievementSetMap.containsKey(setId))
            return;

        InfoAchievementSet infoAchievementSet = new InfoAchievementSet();
        infoAchievementSetMap.put(setId, infoAchievementSet);

        InventoryAchievementRoot inventoryAchievementRoot = inventoryAchievementManager.getAchievementRootInventory();
        InventoryAchievementSet inventoryAchievementSet = new InventoryAchievementSet(mixedAchievementsManagerAccessor, inventoryAchievementRoot, setId, blueprintAchievementSet.getInventoryName(), blueprintAchievementSet.getAchievementItemSetup());
        inventoryAchievementRoot.getInventoryAchievementSetMap().put(setId, inventoryAchievementSet);

        integrateAchievementCategory(blueprintAchievementSet, infoAchievementSet, inventoryAchievementSet, setId);
        inventoryAchievementManager.reload();
    }

    private void integrateAchievementCategory(BlueprintAchievementCategory blueprintAchievementCategory, InfoAchievementSet infoAchievementSet, InventoryAchievementCategory inventoryAchievementCategory, String setId) {
        for (int slot : blueprintAchievementCategory.getBlueprintAchievementElementMap().keySet()) {
            BlueprintAchievementElement blueprintElement = blueprintAchievementCategory.getBlueprintAchievementElementMap().get(slot);

            if (blueprintElement instanceof BlueprintAchievementCategory) {
                BlueprintAchievementCategory subBlueprintFolder = (BlueprintAchievementCategory) blueprintElement;
                InventoryAchievementCategory subInventoryFolder = new InventoryAchievementCategory(mixedAchievementsManagerAccessor, inventoryAchievementCategory, subBlueprintFolder.getInventoryName(), subBlueprintFolder.getAchievementItemSetup());
                inventoryAchievementCategory.getInventoryAchievementElementMap().put(slot, subInventoryFolder);
                integrateAchievementCategory(subBlueprintFolder, infoAchievementSet, subInventoryFolder, setId);
            } else {
                BlueprintAchievementLeaf blueprintLeaf = (BlueprintAchievementLeaf) blueprintElement;
                List<AchievementItemSetup> achievementItemSetupList = new ArrayList<>();

                for (BlueprintAchievementStage blueprintAchievementStage : blueprintLeaf.getBlueprintAchievementStageList()) {
                    achievementItemSetupList.add(blueprintAchievementStage.getAchievementItemSetup());
                }

                InventoryAchievementLeaf inventoryLeaf = new InventoryAchievementLeaf(mixedAchievementsManagerAccessor, inventoryAchievementCategory, achievementItemSetupList);
                inventoryAchievementCategory.getInventoryAchievementElementMap().put(slot, inventoryLeaf);
                integrateAchievementLeaf(blueprintLeaf, infoAchievementSet, inventoryLeaf, setId);
            }
        }
    }

    private void integrateAchievementLeaf(BlueprintAchievementLeaf blueprintAchievementLeaf, InfoAchievementSet infoAchievementSet, InventoryAchievementLeaf inventoryAchievementLeaf, String setId) {
        String achievementId = blueprintAchievementLeaf.getAchievementId();
        DataAchievementRoot dataAchievementRoot = mixedAchievementsManagerAccessor.getAchievementDataManager().getAchievementDataRoot();
        DataAchievementSet dataAchievementSet = dataAchievementRoot.getDataAchievementSetMap().get(setId);

        if (dataAchievementSet == null) {
            dataAchievementSet = new DataAchievementSet(dataAchievementRoot, setId);
            dataAchievementRoot.getDataAchievementSetMap().put(setId, dataAchievementSet);
        }

        DataAchievement dataAchievement = dataAchievementSet.getDataAchievementMap().get(achievementId);

        if (dataAchievement == null) {
            dataAchievement = new DataAchievement(dataAchievementSet, achievementId);
            dataAchievementSet.getDataAchievementMap().put(achievementId, dataAchievement);
        }

        InfoAchievement infoAchievement = infoAchievementSet.getInfoAchievementMap().get(achievementId);

        if (infoAchievement == null) {
            infoAchievement = new InfoAchievement(dataAchievement, setId, blueprintAchievementLeaf.getAchievementId());
            List<InfoAchievementStage> infoAchievementStageList = new ArrayList<>();
            int maxPoints = 0;

            for (int i = 0; i < blueprintAchievementLeaf.getBlueprintAchievementStageList().size(); i++) {
                BlueprintAchievementStage blueprintAchievementStage = blueprintAchievementLeaf.getBlueprintAchievementStageList().get(i);
                maxPoints = Math.max(maxPoints, blueprintAchievementStage.getMaxPoints());
                infoAchievementStageList.add(new InfoAchievementStage(infoAchievement, i, maxPoints));
            }

            infoAchievement.setInfoAchievementStageList(infoAchievementStageList);
            infoAchievement.setUsesPoints(maxPoints > 0);
            infoAchievementSet.getInfoAchievementMap().put(achievementId, infoAchievement);
        }

        inventoryAchievementLeaf.setInfoAchievement(infoAchievement);
    }

    public void completeStage(String setId, String achievementId, String playerId) {
        DataPlayerAchievement dataPlayerAchievement = fetchDataPlayerAchievement(setId, achievementId, playerId);
        InfoAchievement infoAchievement = fetchInfoAchievement(setId, achievementId);

        if (dataPlayerAchievement.getStage() >= infoAchievement.getStageNumber())
            return;

        dataPlayerAchievement.setStage(1 + dataPlayerAchievement.getStage());
        achievementUnlocked(infoAchievement, playerId);
    }

    public void completeAbsolut(String setId, String achievementId, String playerId) {
        DataPlayerAchievement dataPlayerAchievement = fetchDataPlayerAchievement(setId, achievementId, playerId);
        InfoAchievement infoAchievement = fetchInfoAchievement(setId, achievementId);

        while (dataPlayerAchievement.getStage() < infoAchievement.getStageNumber()) {
            completeStage(setId, achievementId, playerId);
        }
    }

    public boolean isAbsolutCompleted(String setId, String achievementId, String playerId) {
        DataPlayerAchievement dataPlayerAchievement = fetchDataPlayerAchievement(setId, achievementId, playerId);
        InfoAchievement infoAchievement = fetchInfoAchievement(setId, achievementId);
        return dataPlayerAchievement.getStage() >= infoAchievement.getStageNumber();
    }

    public void setStage(String setId, String achievementId, String playerId, int value) {
        DataPlayerAchievement dataPlayerAchievement = fetchDataPlayerAchievement(setId, achievementId, playerId);
        InfoAchievement infoAchievement = fetchInfoAchievement(setId, achievementId);
        int stageNumber = infoAchievement.getStageNumber();
        value = Math.min(stageNumber, value);

        while (dataPlayerAchievement.getStage() < value) {
            completeStage(setId, achievementId, playerId);
        }

        dataPlayerAchievement.setStage(value);
    }

    public void addStage(String setId, String achievementId, String playerId, int value) {
        DataPlayerAchievement dataPlayerAchievement = fetchDataPlayerAchievement(setId, achievementId, playerId);
        InfoAchievement infoAchievement = fetchInfoAchievement(setId, achievementId);
        int stageNumber = infoAchievement.getStageNumber();
        value = Math.min(stageNumber, value + dataPlayerAchievement.getStage());

        while (dataPlayerAchievement.getStage() < value) {
            completeStage(setId, achievementId, playerId);
        }

        dataPlayerAchievement.setStage(value);
    }

    public int getStage(String setId, String achievementId, String playerId) {
        return fetchDataPlayerAchievement(setId, achievementId, playerId).getStage();
    }

    public void setPoints(String setId, String achievementId, String playerId, int value) {
        if (!fetchInfoAchievement(setId, achievementId).usesPoints())
            return;

        DataPlayerAchievement dataPlayerAchievement = fetchDataPlayerAchievement(setId, achievementId, playerId);
        dataPlayerAchievement.setPoints(value);
        checkPointCompletion(setId, achievementId, playerId);
    }

    public void addPoints(String setId, String achievementId, String playerId, int value) {
        if (!fetchInfoAchievement(setId, achievementId).usesPoints())
            return;

        DataPlayerAchievement dataPlayerAchievement = fetchDataPlayerAchievement(setId, achievementId, playerId);
        dataPlayerAchievement.setPoints(value + dataPlayerAchievement.getPoints());
        checkPointCompletion(setId, achievementId, playerId);
    }

    public int getPoints(String setId, String achievementId, String playerId) {
        return fetchDataPlayerAchievement(setId, achievementId, playerId).getPoints();
    }

    public void revaluePointCompletion(String setId, String achievementId, String playerId) {
        InfoAchievement infoAchievement = fetchInfoAchievement(setId, achievementId);

        if (!infoAchievement.usesPoints())
            return;

        checkPointCompletion(setId, achievementId, playerId);
        DataPlayerAchievement dataPlayerAchievement = fetchDataPlayerAchievement(setId, achievementId, playerId);
        InfoAchievementStage infoAchievementStage = infoAchievement.getInfoAchievementStageList().get(dataPlayerAchievement.getStage());

        if (dataPlayerAchievement.getPoints() >= infoAchievementStage.getMaxPoints()) {
            if (dataPlayerAchievement.getStage() >= infoAchievement.getStageNumber())
                return;

            completeStage(setId, achievementId, playerId);
        } else {

            dataPlayerAchievement.setStage(dataPlayerAchievement.getStage() - 1);
        }

        revaluePointCompletion(setId, achievementId, playerId);
    }

    private void checkPointCompletion(String setId, String achievementId, String playerId) {
        InfoAchievement infoAchievement = fetchInfoAchievement(setId, achievementId);

        if (!infoAchievement.usesPoints())
            return;

        DataPlayerAchievement dataPlayerAchievement = fetchDataPlayerAchievement(setId, achievementId, playerId);

        if (dataPlayerAchievement.getStage() >= infoAchievement.getStageNumber())
            return;

        InfoAchievementStage infoAchievementStage = infoAchievement.getInfoAchievementStageList().get(dataPlayerAchievement.getStage());

        if (dataPlayerAchievement.getPoints() < infoAchievementStage.getMaxPoints())
            return;

        completeStage(setId, achievementId, playerId);
        checkPointCompletion(setId, achievementId, playerId);
    }

    private InfoAchievement fetchInfoAchievement(String setId, String achievementId) {
        InfoAchievementSet infoAchievementSet = infoAchievementSetMap.get(setId);

        if (infoAchievementSet == null)
            throw new IllegalArgumentException("Set key not found.");

        InfoAchievement infoAchievement = infoAchievementSet.getInfoAchievementMap().get(achievementId);

        if (infoAchievement == null)
            throw new IllegalArgumentException("Achievement key not found.");

        return infoAchievement;
    }

    public DataPlayerAchievement fetchDataPlayerAchievement(String setId, String achievementId, String playerId) throws IllegalArgumentException {
        InfoAchievement infoAchievement = fetchInfoAchievement(setId, achievementId);
        DataAchievement dataAchievement = infoAchievement.getDataAchievement();
        DataPlayerAchievement dataPlayerAchievement = dataAchievement.getDataPlayerAchievementMap().get(playerId);

        if (dataPlayerAchievement == null) {
            dataPlayerAchievement = new DataPlayerAchievement(dataAchievement, playerId);
            dataAchievement.getDataPlayerAchievementMap().put(playerId, dataPlayerAchievement);
        }

        return dataPlayerAchievement;
    }

    private void achievementUnlocked(InfoAchievement infoAchievement, String playerId) {
        Player player = mixedAchievementsManagerAccessor.getPlugin().getServer().getPlayer(playerId);

        if (player == null)
            return;

        player.sendMessage("Achievement unlocked: " + infoAchievement.getAchievementId());
    }
}

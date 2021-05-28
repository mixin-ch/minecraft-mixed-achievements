package ch.mixin.mixedAchievements.metaData;

import ch.mixin.mixedAchievements.customConfig.CustomConfig;
import ch.mixin.mixedAchievements.main.MixedAchievementsManagerAccessor;
import org.bukkit.configuration.ConfigurationSection;

public class AchievementDataManager {
    private final MixedAchievementsManagerAccessor mixedAchievementsManagerAccessor;
    private final CustomConfig achievementsConfig;
    private AchievementDataRoot achievementDataRoot = new AchievementDataRoot();

    public AchievementDataManager(MixedAchievementsManagerAccessor mixedAchievementsManagerAccessor, CustomConfig achievementsConfig) {
        this.mixedAchievementsManagerAccessor = mixedAchievementsManagerAccessor;
        this.achievementsConfig = achievementsConfig;
        loadFromConfig();
        saveToConfig();
    }

    public void loadFromConfig() {
        achievementsConfig.reload();
        ConfigurationSection achievementDataRootConfig = achievementsConfig.getConfiguration().getConfigurationSection("achievementDataRoot");

        if (achievementDataRootConfig == null)
            return;

        achievementDataRoot = new AchievementDataRoot(achievementDataRootConfig);
    }

    public void saveToConfig() {
        ConfigurationSection achievementDataRootConfig = achievementsConfig.getConfiguration().createSection("achievementDataRoot");
        achievementDataRoot.saveToConfig(achievementDataRootConfig);
        achievementsConfig.save();
    }

    public CustomConfig getAchievementsConfig() {
        return achievementsConfig;
    }

    public AchievementDataRoot getAchievementDataRoot() {
        return achievementDataRoot;
    }

    public void setAchievementDataRoot(AchievementDataRoot achievementDataRoot) {
        this.achievementDataRoot = achievementDataRoot;
    }
}
package ch.mixin.mixedAchievements.blueprint;

public abstract class AchievementBlueprintElement {
    protected AchievementItemSetup achievementItemSetup;

    public AchievementBlueprintElement(AchievementItemSetup achievementItemSetup) {
        this.achievementItemSetup = achievementItemSetup;
    }

    public AchievementItemSetup getAchievementItemSetup() {
        return achievementItemSetup;
    }

    public void setAchievementItemSetup(AchievementItemSetup achievementItemSetup) {
        this.achievementItemSetup = achievementItemSetup;
    }
}

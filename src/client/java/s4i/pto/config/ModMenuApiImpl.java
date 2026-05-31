package s4i.pto.config;

import com.mojang.blaze3d.platform.InputConstants;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.lwjgl.glfw.GLFW;

public class ModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return screen ->  getConfigBuilder().setParentScreen(screen).build();
    }

    public static ConfigBuilder getConfigBuilder() {
        ModConfig config = ModConfig.getInstance();

        ConfigBuilder builder = ConfigBuilder.create().setTitle(Component.literal("S4I's Trajectory Prediction Overlay"));
        builder.setGlobalized(false);
        builder.setGlobalizedExpanded(false);
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        BooleanListEntry unimplementedSettingsDisabler = entryBuilder.startBooleanToggle(Component.empty(), false).build();

        // General settings
        ConfigCategory generalSettings = builder.getOrCreateCategory(Component.literal("General"));
        generalSettings.addEntry(entryBuilder
                .startKeyCodeField(Component.literal("Toggle key"), InputConstants.Type.KEYSYM.getOrCreate(config.toggleKey))
                .setDefaultValue(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_KP_1))
                .setAllowMouse(true)
                .setTooltip(Component.literal("Toggle trajectory prediction on/off using this key"))
                .setKeySaveConsumer(key -> config.toggleKey = key.getValue())
                .build());
        generalSettings.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Show trajectory prediction lines"), config.showTrajectoryPredictionLine)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.showTrajectoryPredictionLine = value)
                .build());

        SubCategoryBuilder generalColors = entryBuilder
                .startSubCategory(Component.literal("Default line color and width"))
                .setExpanded(true);

        generalColors.add(entryBuilder
                .startIntSlider(Component.literal("Line width"), (int) (config.lineWidth * 100), 50, 1000)
                .setDefaultValue(200)
                .setTextGetter(value -> Component.literal(String.format("%.2f", value / 100f)))
                .setSaveConsumer(scaledValue -> config.lineWidth = scaledValue / 100f)
                .build());
        generalColors.add(entryBuilder
                .startIntSlider(Component.literal("Line width scaling"), (int) (config.lineWidthScaling * 100), 10, 800)
                .setDefaultValue(100)
                .setTextGetter(value -> Component.literal(String.format("%.2f", value / 100f)))
                .setTooltip(Component.literal("Scale for distant line width. Higher value makes it thicker."))
                .setSaveConsumer(scaledValue -> config.lineWidthScaling = scaledValue / 100f)
                .build());
        generalColors.add(entryBuilder
                .startColorField(Component.literal("Line RGB value"), config.lineColor)
                .setDefaultValue(0x33ff00)
                .setSaveConsumer(value -> config.lineColor = value)
                .setAlphaMode(false).build());
        generalColors.add(entryBuilder.startIntSlider(Component.literal("Line opacity"), (int) (config.lineColorA * 100), 0, 100)
                .setDefaultValue(100)
                .setTextGetter(value -> Component.literal(String.valueOf(value)))
                .setSaveConsumer(scaledValue -> config.lineColorA = scaledValue / 100f)
                .build());
        generalSettings.addEntry(generalColors.build());

        generalSettings.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Hide estimation if weapon has no charge"), config.hideEstimationsIfWeaponHasNoCharge)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.hideEstimationsIfWeaponHasNoCharge = value)
                .setTooltip(Component.literal("Hide trajectory for items like crossbows if they're not loaded"))
                .build());
        generalSettings.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Show different color at max draw/charge"), config.showDifferentTrajectoryColorAtMaxPower)
                .setDefaultValue(false)
                .setSaveConsumer(value -> config.showDifferentTrajectoryColorAtMaxPower = value)
                .setTooltip(Component.literal("Color trajectory line in a different color if player has weapon at max draw/charge"))
                .build());
        generalSettings.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Blend max charge color"), config.showTrajectoryColorGradientDependingOnPower)
                .setDefaultValue(false)
                .setSaveConsumer(value -> config.showTrajectoryColorGradientDependingOnPower = value)
                .setTooltip(Component.literal("Blend color at max draw with the default color of the line"))
                .build());

        SubCategoryBuilder maxDrawColors = entryBuilder.startSubCategory(Component.literal("Max draw/charge colors"));
        maxDrawColors.add(entryBuilder
                .startColorField(Component.literal("Max draw RGB value"), config.maxDrawColor)
                .setDefaultValue(0x000000)
                .setSaveConsumer(value -> config.maxDrawColor = value)
                .setAlphaMode(false)
                .build());
        maxDrawColors.add(entryBuilder
                .startIntSlider(Component.literal("Max draw opacity"), (int) (config.maxDrawColorA * 100), 0, 100)
                .setDefaultValue(100)
                .setTextGetter(value -> Component.literal(String.valueOf(value)))
                .setSaveConsumer(scaledValue -> config.maxDrawColorA = scaledValue / 100f)
                .build());
        generalSettings.addEntry(maxDrawColors.build());
        generalSettings.addEntry(entryBuilder
                .startIntSlider(Component.literal("Dotted line scale"), (int) (config.dottedLineScale * 100), 0, 100)
                .setDefaultValue(100)
                .setTooltip(Component.literal("Changes lines to dotted lines. Lower value means smaller dots."))
                .setTextGetter(value -> Component.literal(String.valueOf(value)))
                .setSaveConsumer(scaledValue -> config.dottedLineScale = scaledValue / 100f)
                .build());

        // Offset settings
        ConfigCategory offsetSettings = builder.getOrCreateCategory(Component.literal("Offset"));
        offsetSettings.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Offset trajectory away from player camera"), config.offsetTrajectorySelf)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.offsetTrajectorySelf = value)
                .setTooltip(Component.literal("Move the start of the trajectory slightly away from the player's camera"))
                .build());
        offsetSettings.addEntry(entryBuilder
                .startIntSlider(Component.literal("Offset multiplier"), (int) (config.offsetMultiplier * 100), 0, 100)
                .setDefaultValue(100)
                .setTextGetter(value -> Component.literal(String.format("%.2f", value / 100f)))
                .setSaveConsumer(scaledValue -> config.offsetMultiplier = scaledValue / 100f)
                .setTooltip(Component.literal("Adjusts how much offset to apply"))
                .build());
        offsetSettings.addEntry(entryBuilder
                .startIntSlider(Component.literal("Offset early falloff strength"), (int) (config.offsetFalloffStrength * 100), 0, 500)
                .setDefaultValue(50)
                .setTextGetter(value -> Component.literal(String.format("%.2f", value / 100f)))
                .setSaveConsumer(scaledValue -> config.offsetFalloffStrength = scaledValue / 100f)
                .setTooltip(Component.literal("Adjusts how fast the offset returns to 0 for shorter lines.\nHigher value is less offset."))
                .build());
        offsetSettings.addEntry(entryBuilder
                .startIntSlider(Component.literal("Offset hardness"), (int) (config.offsetSmoothnessMultiplier * 100), 100, 300)
                .setDefaultValue(135)
                .setTextGetter(value -> Component.literal(String.format("%.2f", value / 100f)))
                .setSaveConsumer(scaledValue -> config.offsetSmoothnessMultiplier = scaledValue / 100f)
                .setTooltip(Component.literal("Adjusts how smooth the line will be.\nLower is smoother"))
                .build());
        offsetSettings.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Hide closest part of trajectory"), config.hideClosestPartOfTrajectory)
                .setDefaultValue(false)
                .setSaveConsumer(value -> config.hideClosestPartOfTrajectory = value)
                .setTooltip(Component.literal("Hide part of the trajectory closest to the camera.\nShort estimations might not render properly."))
                .build());

        // Highlights
        ConfigCategory highlightSettings = builder.getOrCreateCategory(Component.literal("Highlights"));
        highlightSettings.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Highlight landing point"), config.highlightLandingPoint)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.highlightLandingPoint = value)
                .build());
        highlightSettings.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Highlight landing block"), config.highlightLandingBlock)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.highlightLandingBlock = value)
                .build());
        SubCategoryBuilder highlightColors = entryBuilder
                .startSubCategory(Component.literal("Highlight point and block colors"));
        highlightColors.add(entryBuilder.startColorField(Component.literal("Highlight point RGB value"), config.highlightPointColor)
                .setDefaultValue(0xffffff)
                .setAlphaMode(false)
                .setSaveConsumer(value -> config.highlightPointColor = value)
                .build());
        highlightColors.add(entryBuilder.startColorField(Component.literal("Highlight block RGB value"), config.highlightBlockColor)
                .setDefaultValue(0xffffff)
                .setAlphaMode(false)
                .setSaveConsumer(value -> config.highlightBlockColor = value)
                .build());
        highlightSettings.addEntry(highlightColors.build());
        highlightSettings.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Highlight targeted entity"), config.highlightTargetedEntity)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Cover predicted entity to be hit in a box"))
                .setSaveConsumer(value -> config.highlightTargetedEntity = value)
                .build());
        highlightSettings.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Highlight targeted entity's edges"), config.highlightTargetedEntityEdges)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Draw box outline for the targeted entity"))
                .setSaveConsumer(value -> config.highlightTargetedEntityEdges = value)
                .build());
        SubCategoryBuilder highlightEntityColor = entryBuilder.startSubCategory(Component.literal("Entity highlight color"));
        highlightEntityColor.add(entryBuilder
                .startColorField(Component.literal("Entity highlight RGB value"), config.highlightEntityColor)
                .setDefaultValue(0x33ff00)
                .setAlphaMode(false)
                .setSaveConsumer(value -> config.highlightEntityColor = value)
                .build());
        highlightEntityColor.add(entryBuilder.startColorField(Component.literal("Hostile entity highlight RGB value"), config.highlightHostileEntityColor)
                .setDefaultValue(0x8800ff)
                .setAlphaMode(false)
                .setSaveConsumer(value -> config.highlightHostileEntityColor = value)
                .build());
        highlightEntityColor.add(entryBuilder
                .startIntSlider(Component.literal("Max draw opacity"), (int) (config.highlightEntityColorA * 100), 0, 100)
                .setDefaultValue(40)
                .setTextGetter(value -> Component.literal(String.valueOf(value)))
                .setSaveConsumer(scaledValue -> config.highlightEntityColorA = scaledValue / 100f)
                .build());
        highlightSettings.addEntry(highlightEntityColor.build());

        // Performance
        ConfigCategory advancedSettings = builder.getOrCreateCategory(Component.literal("Performance"));
        advancedSettings.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Use complex collision checker"), config.useComplexPhysics)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.useComplexPhysics = value)
                .setTooltip(Component.literal("Medium performance impact\n")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true))
                        .append(Component.literal("Fully simulates underwater physics and block collisions.\n" +
                                        "Disable for more performance at the cost of simulation accuracy.")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))))
                .build());
        advancedSettings.addEntry(entryBuilder
                .startIntSlider(Component.literal("Number of ticks to simulate"), config.maxNumberOfTicksToSimulate, 20, 1000)
                .setDefaultValue(200)
                .setSaveConsumer(value -> config.maxNumberOfTicksToSimulate = value)
                .setTextGetter(value -> Component.literal(String.valueOf(value)))
                .setTooltip(Component.literal("Medium performance impact")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true))
                        .append(Component.literal("\nNumber of ticks to simulate ahead.\n" +
                                        "Turn lower than 100 when very high in the air or if there is no ground for a performance boost")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))))
                .build());
        advancedSettings.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Show trajectory estimation for self"), config.showProjectileTrajectoryForSelf)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.showProjectileTrajectoryForSelf = value)
                .setTooltip(Component.literal("Low performance impact")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true))
                        .append(Component.literal("\nSimulates and displays trajectory prediction for self (this means YOU)")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))))
                .build());
        BooleanListEntry showTrajectoriesFromPlayersEntry = entryBuilder
                .startBooleanToggle(Component.literal("Show trajectory estimation for other players"), config.showProjectileTrajectoriesFromOtherPlayers)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.showProjectileTrajectoriesFromOtherPlayers = value)
                .setTooltip(Component.literal("High performance impact")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true))
                        .append(Component.literal("\nSimulates and displays trajectory prediction for all the players in the nearby area")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))))
                .build();
        BooleanListEntry showTrajectoriesFromFiredProjectiles = entryBuilder
                .startBooleanToggle(Component.literal("Show trajectory estimation for in-air projectiles"), config.showAlreadyFiredProjectileTrajectories)
                .setDefaultValue(true)
                .setSaveConsumer(value -> config.showAlreadyFiredProjectileTrajectories = value)
                .setTooltip(Component.literal("High performance impact")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true))
                        .append(Component.literal("\nKeep showing trajectory prediction for fired projectiles.\nProjectiles not fired by the player are also counted.")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))))
                .build();
        advancedSettings.addEntry(showTrajectoriesFromPlayersEntry);
        advancedSettings.addEntry(showTrajectoriesFromFiredProjectiles);

        SubCategoryBuilder otherProjectileSettings = entryBuilder
                .startSubCategory(Component.literal("Alternative color scheme for other players and projectiles"))
                .setDisplayRequirement(Requirement.any(
                        Requirement.isTrue(showTrajectoriesFromFiredProjectiles), Requirement.isTrue(showTrajectoriesFromPlayersEntry)))
                .setRequirement(Requirement.any(
                        Requirement.isTrue(showTrajectoriesFromFiredProjectiles), Requirement.isTrue(showTrajectoriesFromPlayersEntry)));
        otherProjectileSettings.add(entryBuilder
                .startIntSlider(Component.literal("Player/projectile search radius"), config.searchRadius, 5, 1000)
                .setDefaultValue(250)
                .setTooltip(Component.literal("High performance impact")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true))
                        .append(Component.literal("\nHow far to search for other players and projectiles (in blocks).")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))))
                .setSaveConsumer(value -> config.searchRadius = value)
                .build());
        otherProjectileSettings.add(entryBuilder.startColorField(Component.literal("Alternative line color"), config.alternativePlayerLineColor)
                .setDefaultValue(0xffff00)
                .setAlphaMode(false)
                .setSaveConsumer(value -> config.alternativePlayerLineColor = value)
                .build());
        otherProjectileSettings.add(entryBuilder
                .startIntSlider(Component.literal("Alternative line color opacity"), (int) (config.alternativePlayerLineColorA * 100), 0, 100)
                .setDefaultValue(100)
                .setTextGetter(value -> Component.literal(String.valueOf(value)))
                .setSaveConsumer(scaledValue -> config.alternativePlayerLineColorA = scaledValue / 100f)
                .build());
        otherProjectileSettings.add(entryBuilder
                .startColorField(Component.literal("Alternative projectile color"), config.alternativeProjectileLineColor)
                .setDefaultValue(0xffbb00)
                .setAlphaMode(false)
                .setSaveConsumer(value -> config.alternativeProjectileLineColor = value)
                .build());
        otherProjectileSettings.add(entryBuilder
                .startIntSlider(Component.literal("Alternative projectile color opacity"), (int) (config.alternativeProjectileLineColorA * 100), 0, 100)
                .setDefaultValue(100)
                .setTextGetter(value -> Component.literal(String.valueOf(value)))
                .setSaveConsumer(scaledValue -> config.alternativeProjectileLineColorA = scaledValue / 100f)
                .build());
        otherProjectileSettings.add(entryBuilder
                .startColorField(Component.literal("Alternative highlight point color"), config.alternativeHighlightPointColor)
                .setDefaultValue(0xffff99)
                .setAlphaMode(false)
                .setSaveConsumer(value -> config.alternativeHighlightPointColor = value)
                .build());
        otherProjectileSettings.add(entryBuilder
                .startIntSlider(Component.literal("Alternative highlight point color opacity"), (int) (config.alternativeHighlightPointColorA * 100), 0, 100)
                .setDefaultValue(100)
                .setTextGetter(value -> Component.literal(String.valueOf(value)))
                .setSaveConsumer(scaledValue -> config.alternativeHighlightPointColorA = scaledValue / 100f)
                .build());
        otherProjectileSettings.add(entryBuilder
                .startColorField(Component.literal("Alternative highlight entity color"), config.alternativeHighlightEntityColor)
                .setDefaultValue(0xffbb00)
                .setAlphaMode(false)
                .setSaveConsumer(value -> config.alternativeHighlightEntityColor = value)
                .build());
        otherProjectileSettings.add(entryBuilder
                .startIntSlider(Component.literal("Alternative highlight entity color opacity"), (int) (config.alternativeHighlightEntityColorA * 100), 0, 100)
                .setDefaultValue(30)
                .setTextGetter(value -> Component.literal(String.valueOf(value)))
                .setSaveConsumer(scaledValue -> config.alternativeHighlightEntityColorA = scaledValue / 100f)
                .build());
        otherProjectileSettings.add(entryBuilder
                .startColorField(Component.literal("Alternative highlight block color"), config.alternativeHighlightBlockColor)
                .setDefaultValue(0xffbb00)
                .setAlphaMode(false)
                .setSaveConsumer(value -> config.alternativeHighlightBlockColor = value)
                .build());
        advancedSettings.addEntry(otherProjectileSettings.build());


        ConfigCategory experimentalSettings = builder.getOrCreateCategory(Component.literal("Experimental"));
        experimentalSettings.addEntry(entryBuilder
                .startTextDescription(
                Component.literal("These settings are unfinished, untested and might be removed or changed in a future version.\n" +
                        "Use at your own risk.")).build());
        experimentalSettings.addEntry(entryBuilder
                .startBooleanToggle(Component.literal("Show explosion radius of fireworks"), config.showExplosionRadius)
                .setDefaultValue(false)
                .setSaveConsumer(value -> config.showExplosionRadius = value)
                .build());
        experimentalSettings.addEntry(entryBuilder
                .startIntSlider(Component.literal("Number of deviation markers"), config.numberOfDeviationMarkers, 0, 1)
                .setDefaultValue(0)
                .setSaveConsumer(value -> config.numberOfDeviationMarkers = value)
                .setTooltip(Component.literal("How many markers along the projectile path to display that show the maximum possible spread it could have"))
                .build());

        SubCategoryBuilder experimentalSubcategorySettings = entryBuilder
                .startSubCategory(Component.literal("Deviation colors"))
                .setExpanded(false);
        experimentalSubcategorySettings.add(0, entryBuilder
                .startColorField(Component.literal("Marker RGB value"), config.deviationColor)
                .setDefaultValue(0xff0000)
                .setSaveConsumer(value -> config.deviationColor = value)
                .setAlphaMode(false)
                .build());
        experimentalSubcategorySettings.add(1, entryBuilder
                .startIntSlider(Component.literal("Marker opacity"), (int) (config.deviationColorA * 100), 0, 100)
                .setDefaultValue(100)
                .setTextGetter(value -> Component.literal(String.valueOf(value)))
                .setSaveConsumer(scaledValue -> config.deviationColorA = scaledValue / 100f)
                .build());
        experimentalSettings.addEntry(experimentalSubcategorySettings.build());

        builder.setSavingRunnable(ModConfig::triggerSave);
        builder.setTransparentBackground(true);
        return builder;
    }
}
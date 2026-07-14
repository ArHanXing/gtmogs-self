package com.quantumgarbage.gtmogs.integration.xei.widgets;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;

import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;

import com.quantumgarbage.gtmogs.api.gui.widget.SlotWidget;
import com.quantumgarbage.gtmogs.api.registry.GTRegistries;
import com.quantumgarbage.gtmogs.api.transfer.item.CustomItemStackHandler;
import com.quantumgarbage.gtmogs.api.worldgen.DimensionMarker;
import com.quantumgarbage.gtmogs.api.worldgen.OreVeinDefinition;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

import java.util.*;

@Getter
public class GTOreVeinWidget extends WidgetGroup {

    private final String translationKey;
    private final int weight;
    private final String range;
    private final Set<ResourceKey<Level>> dimensionFilter;
    public final static int width = 120;

    public GTOreVeinWidget(Holder<OreVeinDefinition> ore) {
        super(0, 0, width, 160);
        this.translationKey = getOreName(ore);
        this.weight = ore.value().weight();
        this.dimensionFilter = ore.value().dimensionFilter();
        this.range = range(ore.value());
        setClientSideWidget();
        setupBaseGui(ore.value());
        setupText(ore.value());
    }

    @SuppressWarnings("all")
    private String range(OreVeinDefinition oreDefinition) {
        HeightProvider height = oreDefinition.heightRange().height;
        int minHeight = 0, maxHeight = 0;
        if (height instanceof UniformHeight uniform) {
            minHeight = uniform.minInclusive.resolveY(null);
            maxHeight = uniform.maxInclusive.resolveY(null);
        }
        return String.format("%d - %d", minHeight, maxHeight);
    }

    private void setupBaseGui(OreVeinDefinition oreDefinition) {
        NonNullList<ItemStack> containedOresAsItemStacks = NonNullList.create();
        IntList chances = oreDefinition.veinGenerator().getAllChances();
        containedOresAsItemStacks.addAll(getRawMaterialList(oreDefinition));
        int n = containedOresAsItemStacks.size();
        int x = (width - 18 * n) / 2;
        for (int i = 0; i < n; i++) {
            SlotWidget oreSlot = new SlotWidget(new CustomItemStackHandler(containedOresAsItemStacks), i, x, 18, false,
                    false);
            int finalIndex = i;
            oreSlot.setOnAddedTooltips((stack, tooltips) -> tooltips.add(
                    Component.translatable("gtmogs.jei.ore_vein_diagram.chance", chances.getInt(finalIndex))));
            oreSlot.setIngredientIO(IngredientIO.OUTPUT);
            addWidget(oreSlot);
            x += 18;
        }
    }

    private void setupText(OreVeinDefinition ignored) {
        addWidget(new ImageWidget(5, 0, width - 10, 16,
                new TextTexture(translationKey).setType(TextTexture.TextType.LEFT_ROLL)
                        .setWidth(width - 10)));
        addWidget(new LabelWidget(5, 40,
                LocalizationUtils.format("gtmogs.jei.ore_vein_diagram.spawn_range")));
        addWidget(new LabelWidget(5, 50, range));

        addWidget(new LabelWidget(5, 60,
                LocalizationUtils.format("gtmogs.jei.ore_vein_diagram.weight", weight)));
        addWidget(new LabelWidget(5, 70,
                LocalizationUtils.format("gtmogs.jei.ore_vein_diagram.dimensions")));
        setupDimensionMarker(80);
    }

    private void setupDimensionMarker(int yPosition) {
        if (this.dimensionFilter != null) {
            int interval = 2;
            int rowSlots = (width - 10 + interval) / (16 + interval);

            DimensionMarker[] dimMarkers = dimensionFilter.stream()
                    .map(ResourceKey::location)
                    .map(loc -> GTRegistries.DIMENSION_MARKERS.getOptional(loc)
                            .orElse(new DimensionMarker(DimensionMarker.MAX_TIER, () -> Blocks.BARRIER,
                                    loc.toString())))
                    .sorted(Comparator.comparingInt(DimensionMarker::getTier))
                    .toArray(DimensionMarker[]::new);
            var handler = new CustomItemStackHandler(dimMarkers.length);
            for (int i = 0; i < dimMarkers.length; i++) {
                var dimMarker = dimMarkers[i];
                var icon = dimMarker.getIcon();
                int row = Math.floorDiv(i, rowSlots);
                SlotWidget dimSlot = new SlotWidget(handler, i,
                        5 + (16 + interval) * (i - row * rowSlots),
                        yPosition + 18 * row,
                        false, false).setIngredientIO(IngredientIO.CATALYST);
                handler.setStackInSlot(i, icon);
                addWidget(dimSlot.setBackgroundTexture(IGuiTexture.EMPTY));
            }
        } else {
            addWidget(new LabelWidget(5, yPosition, "Any"));
        }
    }

    public static List<ItemStack> getContainedOresAndBlocks(OreVeinDefinition oreDefinition) {
        return getRawMaterialList(oreDefinition);
    }

    public static List<ItemStack> getRawMaterialList(OreVeinDefinition oreDefinition) {
        return oreDefinition.veinGenerator().getAllBlocks().stream()
                .map(block -> block.getBlock().asItem().getDefaultInstance()).toList();
    }

    public static String getOreName(Holder<OreVeinDefinition> ore) {
        var location = ore.getKey().location();
        // Translates the vein ID to a lang key; if no lang entry exists, uses the path as fallback
        return Component.translatableWithFallback(
                location.toLanguageKey("ore_vein"),
                location.getPath().replace('_', ' ').replace("/", ": ")
        ).getString();
    }
}

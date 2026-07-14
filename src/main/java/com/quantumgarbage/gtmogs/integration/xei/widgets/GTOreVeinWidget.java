package com.quantumgarbage.gtmogs.integration.xei.widgets;

import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;

import com.quantumgarbage.gtmogs.api.registry.GTRegistries;
import com.quantumgarbage.gtmogs.api.worldgen.DimensionMarker;
import com.quantumgarbage.gtmogs.api.worldgen.OreVeinDefinition;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;

import java.util.*;

@Getter
public class GTOreVeinWidget {

    public static final int WIDTH = 120;

    private final Holder<OreVeinDefinition> ore;
    private final OreVeinDefinition oreDef;
    private final String translationKey;
    private final int weight;
    private final String range;
    private final Set<ResourceKey<Level>> dimensionFilter;
    private final NonNullList<ItemStack> containedOres;
    private final IntList chances;

    public GTOreVeinWidget(Holder<OreVeinDefinition> ore) {
        this.ore = ore;
        this.oreDef = ore.value();
        this.translationKey = getOreName(ore);
        this.weight = oreDef.weight();
        this.dimensionFilter = oreDef.dimensionFilter();
        this.range = range(oreDef);
        this.containedOres = NonNullList.create();
        this.chances = oreDef.veinGenerator().getAllChances();
        containedOres.addAll(getRawMaterialList(oreDef));
    }

    private static String range(OreVeinDefinition oreDefinition) {
        HeightProvider height = oreDefinition.heightRange().height;
        int minHeight = 0, maxHeight = 0;
        if (height instanceof UniformHeight uniform) {
            minHeight = uniform.minInclusive.resolveY(null);
            maxHeight = uniform.maxInclusive.resolveY(null);
        }
        return String.format("%d - %d", minHeight, maxHeight);
    }

    public DimensionMarker[] getDimensionMarkers() {
        if (this.dimensionFilter == null || this.dimensionFilter.isEmpty()) {
            return new DimensionMarker[0];
        }
        return this.dimensionFilter.stream()
                .map(ResourceKey::location)
                .map(loc -> GTRegistries.DIMENSION_MARKERS.getOptional(loc)
                        .orElse(new DimensionMarker(DimensionMarker.MAX_TIER, () -> Blocks.BARRIER,
                                loc.toString())))
                .sorted(Comparator.comparingInt(DimensionMarker::getTier))
                .toArray(DimensionMarker[]::new);
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
        return Component.translatableWithFallback(
                location.toLanguageKey("ore_vein"),
                location.getPath().replace('_', ' ').replace("/", ": ")
        ).getString();
    }
}

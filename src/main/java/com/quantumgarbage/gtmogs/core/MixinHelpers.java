package com.quantumgarbage.gtmogs.core;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import com.quantumgarbage.gtmogs.GTMOGS;
import com.quantumgarbage.gtmogs.api.GTValues;
import com.quantumgarbage.gtmogs.api.registry.GTRegistries;
import com.quantumgarbage.gtmogs.api.worldgen.OreVeinDefinition;
import com.quantumgarbage.gtmogs.integration.kjs.GTMOGSServerEvents;
import com.quantumgarbage.gtmogs.integration.kjs.events.GTOreVeinKubeEvent;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("deprecation")
public class MixinHelpers {

    private static <T> Collector<T, ?, ArrayList<T>> toArrayList() {
        return Collectors.toCollection(ArrayList::new);
    }

    public static TagLoader.EntryWithSource makeItemEntry(ItemLike item) {
        return makeElementEntry(item.asItem().builtInRegistryHolder().key().location());
    }

    public static TagLoader.EntryWithSource makeBlockEntry(Supplier<? extends Block> block) {
        return makeBlockEntry(block.get());
    }

    public static TagLoader.EntryWithSource makeBlockEntry(Block block) {
        return makeElementEntry(block.builtInRegistryHolder().key().location());
    }

    public static TagLoader.EntryWithSource makeFluidEntry(Fluid fluid) {
        return makeElementEntry(fluid.builtInRegistryHolder().key().location());
    }

    public static TagLoader.EntryWithSource makeElementEntry(ResourceLocation id) {
        return new TagLoader.EntryWithSource(TagEntry.element(id), GTValues.CUSTOM_TAG_SOURCE);
    }

    public static TagLoader.EntryWithSource makeTagEntry(TagKey<?> tag) {
        return new TagLoader.EntryWithSource(TagEntry.tag(tag.location()), GTValues.CUSTOM_TAG_SOURCE);
    }

    public static void postKJSVeinEvents(WritableRegistry<?> registry,
                                         RegistryOps.RegistryInfoLookup infoLookup) {
        if (!GTMOGS.Mods.isKubeJSLoaded()) {
            return;
        }

        if (registry.key() == GTRegistries.ORE_VEIN_REGISTRY) {
            @SuppressWarnings("unchecked")
            var oreVeinRegistry = (WritableRegistry<OreVeinDefinition>) registry;
            KJSCallWrapper.postOreVeinEvent(oreVeinRegistry, infoLookup);
        }
    }

    /**
     * {@link RegistryOps.RegistryInfoLookup} does have biome data during datapack loading,
     * but {@code RegistryInfo.owner()} returns a {@code MappedRegistry$1} anonymous
     * {@link HolderOwner} whose implicit {@code this$0} field IS the {@link Registry} itself.
     */
    @SuppressWarnings("unchecked")
    private static HolderLookup.Provider asHolderProvider(RegistryOps.RegistryInfoLookup infoLookup) {
        return new HolderLookup.Provider() {
            @Override
            public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(
                    ResourceKey<? extends Registry<? extends T>> registryKey) {
                return infoLookup.lookup(registryKey).flatMap(info -> {
                    // info.owner() = MappedRegistry$1 → this$0 = MappedRegistry → Registry.asLookup()
                    Object owner = info.owner();
                    try {
                        Field f = owner.getClass().getDeclaredField("this$0");
                        f.setAccessible(true);
                        Registry<T> reg = (Registry<T>) f.get(owner);
                        return Optional.of(reg.asLookup());
                    } catch (ReflectiveOperationException e) {
                        GTMOGS.LOGGER.error("Cannot extract Registry from HolderOwner {}: {}",
                                owner.getClass().getName(), e.toString());
                        return Optional.empty();
                    }
                });
            }

            @Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistries() {
                return Stream.empty();
            }
        };
    }

    private static final class KJSCallWrapper {

        private static void postOreVeinEvent(WritableRegistry<OreVeinDefinition> registry,
                                             RegistryOps.RegistryInfoLookup infoLookup) {
            GTMOGSServerEvents.ORE_VEIN_MODIFICATION.post(
                    new GTOreVeinKubeEvent(registry, asHolderProvider(infoLookup)));
        }
    }

    public static final class ClientCallWrapper {

        public static Level getClientLevel() {
            return Minecraft.getInstance().level;
        }
    }
}
